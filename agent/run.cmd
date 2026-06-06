@echo off
setlocal

set "AGENT_DIR=%~dp0"

if "%AEGIS_AGENT_PYTHON%"=="" (
    set "AEGIS_AGENT_PYTHON=python"
)

set "PYTHONPATH=%AGENT_DIR%;%PYTHONPATH%"

if "%~1"=="" goto default_config

"%AEGIS_AGENT_PYTHON%" -m aegis_agent.cli run %*
exit /b %ERRORLEVEL%

:default_config
"%AEGIS_AGENT_PYTHON%" -m aegis_agent.cli run --config "%AGENT_DIR%agent.yml"
exit /b %ERRORLEVEL%
