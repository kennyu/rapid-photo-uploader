@echo off
REM RapidPhotoUpload Backend - Windows Run Script
REM ==============================================

echo.
echo 🚀 RapidPhotoUpload Backend
echo ============================
echo.

REM Set JAVA_HOME
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java
echo 🔍 Checking Java 21...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Java 21 not found!
    echo Expected: C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot
    pause
    exit /b 1
)

echo.
echo ✅ Java 21 found!
echo.

REM Check for build flag
if "%1"=="--build" goto build
if "%1"=="-b" goto build
goto run

:build
echo 🔨 Building project...
gradlew.bat build
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)
echo.
echo ✅ Build successful!
echo.

:run
echo.
echo ⚠️  PREREQUISITES CHECK:
echo =====================================
echo.
echo   Before running, ensure you have:
echo   1. PostgreSQL installed and running on port 5432
echo   2. Database 'rapidphoto' created
echo   3. AWS S3 credentials configured
echo.
echo   See SETUP_GUIDE.md for complete setup instructions
echo.
pause
echo.
echo 🚀 Starting backend...
echo.
echo   API will be available at: http://localhost:8080
echo   Health check: http://localhost:8080/api/v1/health
echo.
echo   Press Ctrl+C to stop
echo.

gradlew.bat bootRun

