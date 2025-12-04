@rem Gradle startup script for Windows

@if "%DEBUG%"=="" @echo off
setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

"%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
