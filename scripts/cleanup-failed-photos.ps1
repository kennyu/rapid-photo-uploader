# PowerShell script to cleanup failed/stuck photo uploads
# Requires: PostgreSQL client (psql)

param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbName = "rapid_photo_uploader",
    [string]$DbUser = "postgres",
    [switch]$DryRun = $true,
    [switch]$Force = $false
)

Write-Host "=== Rapid Photo Uploader - Cleanup Failed Photos ===" -ForegroundColor Cyan
Write-Host ""

# Check if psql is available
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: psql not found. Please install PostgreSQL client." -ForegroundColor Red
    Write-Host "Download from: https://www.postgresql.org/download/windows/" -ForegroundColor Yellow
    exit 1
}

# Build connection string
$env:PGPASSWORD = Read-Host "Enter database password" -AsSecureString | ConvertFrom-SecureString -AsPlainText

Write-Host "Connecting to database: $DbHost`:$DbPort/$DbName" -ForegroundColor Gray
Write-Host ""

# First, show what we're about to delete
Write-Host "Checking for failed/stuck uploads..." -ForegroundColor Yellow
$query = @"
SELECT 
    id, 
    filename, 
    status, 
    created_at
FROM photos 
WHERE status IN ('FAILED', 'UPLOADING')
ORDER BY created_at DESC;
"@

$result = psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -t -c $query 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to connect to database" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($result)) {
    Write-Host "✓ No failed or stuck uploads found!" -ForegroundColor Green
    exit 0
}

Write-Host "Found the following failed/stuck uploads:" -ForegroundColor Yellow
Write-Host $result
Write-Host ""

$count = ($result -split "`n" | Where-Object { $_.Trim() -ne "" }).Count
Write-Host "Total: $count photo(s)" -ForegroundColor Cyan
Write-Host ""

if ($DryRun -and -not $Force) {
    Write-Host "DRY RUN MODE - No photos will be deleted" -ForegroundColor Yellow
    Write-Host "To actually delete, run with: -DryRun:`$false" -ForegroundColor Yellow
    exit 0
}

# Confirm deletion
if (-not $Force) {
    $confirm = Read-Host "Delete these photos? (yes/no)"
    if ($confirm -ne "yes") {
        Write-Host "Cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# Delete the photos
Write-Host "Deleting failed/stuck uploads..." -ForegroundColor Red
$deleteQuery = @"
DELETE FROM photos 
WHERE status IN ('FAILED', 'UPLOADING');
"@

psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -c $deleteQuery

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Successfully deleted $count photo(s)" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to delete photos" -ForegroundColor Red
    exit 1
}

# Clear password from environment
$env:PGPASSWORD = $null

