@echo off
setlocal
cd /d "%~dp0"
echo ===============================================
echo NetMaster Learning 5.1 - Release Build
echo Creator: Mohandess Masoud Jokar
echo Contact: 09132184122
echo ===============================================
call gradlew.bat test
if errorlevel 1 goto :fail
call gradlew.bat assembleRelease bundleRelease
if errorlevel 1 goto :fail
echo.
echo BUILD SUCCESSFUL
echo APK: app\build\outputs\apk\release\app-release-unsigned.apk
echo AAB: app\build\outputs\bundle\release\app-release.aab
exit /b 0
:fail
echo.
echo BUILD FAILED - see the Gradle output above.
exit /b 1
