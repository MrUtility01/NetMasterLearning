@echo off
setlocal
cd /d "%~dp0"
echo ===============================================
echo NetMaster Learning 5.1 - Debug Build
echo Creator: Mohandess Masoud Jokar
echo ===============================================
call gradlew.bat test
if errorlevel 1 goto :fail
call gradlew.bat assembleDebug
if errorlevel 1 goto :fail
echo.
echo BUILD SUCCESSFUL
echo APK: app\build\outputs\apk\debug\app-debug.apk
exit /b 0
:fail
echo.
echo BUILD FAILED - see the Gradle output above.
exit /b 1
