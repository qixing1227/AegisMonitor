@echo off
set PYTHONPATH=%~dp0
"C:\Users\QiXing\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe" -m unittest discover -s "%~dp0tests" -v

