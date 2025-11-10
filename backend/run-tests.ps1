# PowerShell script to run backend integration tests
# Requires: Java 21, Docker

Write-Host "Running Backend Integration Tests" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
$dockerRunning = docker ps 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker is not running. Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Docker is running" -ForegroundColor Green

# Run tests
Write-Host ""
Write-Host "Running tests..." -ForegroundColor Yellow
.\gradlew.bat test

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] All tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Test Report: backend\build\reports\tests\test\index.html" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "[FAILED] Tests failed. Check the output above for details." -ForegroundColor Red
    exit 1
}

