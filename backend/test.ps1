$ErrorActionPreference = "Stop"

$backendRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$pom = Join-Path $backendRoot "pom.xml"
$tempRoot = Join-Path $backendRoot "build\temp"

New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
$env:TEMP = $tempRoot
$env:TMP = $tempRoot

if (Test-Path -LiteralPath $pom) {
    $maven = (Get-Command mvn -ErrorAction SilentlyContinue)
    if ($null -eq $maven) {
        $candidateMavens = @(
            $(if ($env:MAVEN_HOME) { Join-Path $env:MAVEN_HOME 'bin\mvn.cmd' } else { $null }),
            'C:\Users\FIERY\dev-tools\apache-maven-3.9.9\bin\mvn.cmd',
            'C:\Users\FIERY\dev-tools\apache-maven-3.9.16\bin\mvn.cmd',
            'C:\Users\QiXing\dev-tools\apache-maven-3.9.16\bin\mvn.cmd'
        )

        $localMaven = $candidateMavens |
            Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
            Select-Object -First 1

        if (-not $localMaven) {
            throw "Maven was not found on PATH and no known Maven installation was found."
        }
        $mavenCommand = $localMaven
    } else {
        $mavenCommand = $maven.Source
    }

    & $mavenCommand -f $pom test
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $classesDir = Join-Path $backendRoot "target\classes"
    $testClassesDir = Join-Path $backendRoot "target\test-classes"
    $testClasses = Get-ChildItem -Path $testClassesDir -Recurse -Filter "*ContractTest.class" |
        ForEach-Object {
            $relativePath = $_.FullName.Substring($testClassesDir.Length + 1)
            $relativePath.Replace("\", ".").Replace("/", ".").Replace(".class", "")
        } |
        Sort-Object

    foreach ($testClass in $testClasses) {
        java -cp "$classesDir;$testClassesDir" $testClass
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }

    exit 0
}

$buildDir = Join-Path $backendRoot "build\test-classes"

New-Item -ItemType Directory -Force -Path $buildDir | Out-Null

$sources = Get-ChildItem `
    -Path (Join-Path $backendRoot "src\main\java"), (Join-Path $backendRoot "src\test\java") `
    -Recurse `
    -Filter "*.java" |
    ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "No Java sources found."
}

javac -encoding UTF-8 -d $buildDir $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$testClasses = Get-ChildItem -Path $buildDir -Recurse -Filter "*ContractTest.class" |
    ForEach-Object {
        $relativePath = $_.FullName.Substring($buildDir.Length + 1)
        $relativePath.Replace("\", ".").Replace("/", ".").Replace(".class", "")
    } |
    Sort-Object

if (-not $testClasses) {
    throw "No contract tests found."
}

foreach ($testClass in $testClasses) {
    java -cp $buildDir $testClass
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
