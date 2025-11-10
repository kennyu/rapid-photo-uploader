# Database Cleanup Script for RapidPhotoUpload
# This script helps you clean up incomplete uploads and failed jobs in PostgreSQL
# Supports cleaning UPLOADING and FAILED photo records from AWS RDS

Write-Host "RapidPhotoUpload Database Cleanup Tool" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Database connection details (update if needed)
$DB_HOST = "rapidphoto-db.c5kiq8ygqqij.us-east-1.rds.amazonaws.com"
$DB_PORT = "5432"
$DB_NAME = "rapidphoto"
$DB_USER = "postgres"
$DB_PASSWORD = "RapidPhoto2024SecurePass!"

Write-Host "Database: $DB_HOST" -ForegroundColor Yellow
Write-Host ""

# Check if psql is installed
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psqlPath) {
    Write-Host "ERROR: PostgreSQL client (psql) not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install PostgreSQL client tools:" -ForegroundColor Yellow
    Write-Host "  Option 1: Install full PostgreSQL: https://www.postgresql.org/download/windows/" -ForegroundColor White
    Write-Host "  Option 2: Use pgAdmin 4 (GUI tool): https://www.pgadmin.org/" -ForegroundColor White
    Write-Host "  Option 3: Run queries manually via AWS RDS console" -ForegroundColor White
    Write-Host ""
    Write-Host "Or use this connection string in your SQL client:" -ForegroundColor Cyan
    $connString = "postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
    Write-Host "  $connString" -ForegroundColor White
    exit 1
}

Write-Host "PostgreSQL client found!" -ForegroundColor Green
Write-Host ""

# Menu
Write-Host "Select an option:" -ForegroundColor Cyan
Write-Host "  1. View incomplete/failed uploads (safe - no changes)" -ForegroundColor White
Write-Host "  2. View all upload statistics" -ForegroundColor White
Write-Host "  3. Delete uploads stuck for > 1 hour" -ForegroundColor Yellow
Write-Host "  4. Delete ALL incomplete uploads (UPLOADING only)" -ForegroundColor Red
Write-Host "  5. Delete ALL failed uploads (FAILED only)" -ForegroundColor Red
Write-Host "  6. Delete BOTH incomplete AND failed uploads" -ForegroundColor Red
Write-Host "  7. Mark incomplete uploads as FAILED (keeps records)" -ForegroundColor Yellow
Write-Host "  8. Connect to database (manual queries)" -ForegroundColor White
Write-Host "  9. Exit" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice (1-9)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "Viewing incomplete/failed uploads..." -ForegroundColor Cyan
        $query = @'
SELECT 
    p.id, 
    p.filename, 
    p.status, 
    p.file_size / 1024 as size_kb,
    p.created_at,
    EXTRACT(EPOCH FROM (NOW() - p.created_at))/3600 as hours_old
FROM photos p
WHERE p.status IN ('UPLOADING', 'FAILED')
ORDER BY p.status, p.created_at DESC;
'@
        $env:PGPASSWORD = $DB_PASSWORD
        psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
    }
    "2" {
        Write-Host ""
        Write-Host "Upload statistics..." -ForegroundColor Cyan
        $query = @'
SELECT 
    status,
    COUNT(*) as count,
    SUM(file_size) / 1024 / 1024 as total_mb
FROM photos
GROUP BY status
ORDER BY count DESC;
'@
        $env:PGPASSWORD = $DB_PASSWORD
        psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
    }
    "3" {
        Write-Host ""
        Write-Host "WARNING: Deleting uploads/failures stuck for > 1 hour..." -ForegroundColor Yellow
        $confirm = Read-Host "Are you sure? (yes/no)"
        if ($confirm -eq "yes") {
            $query = 'DELETE FROM photos WHERE status IN (''UPLOADING'', ''FAILED'') AND created_at < NOW() - INTERVAL ''1 hour'';'
            $env:PGPASSWORD = $DB_PASSWORD
            psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
            Write-Host "Cleanup complete!" -ForegroundColor Green
        } else {
            Write-Host "Cancelled" -ForegroundColor Red
        }
    }
    "4" {
        Write-Host ""
        Write-Host "WARNING: This will DELETE ALL incomplete uploads (UPLOADING only)!" -ForegroundColor Red
        $confirm = Read-Host "Type 'DELETE' to confirm"
        if ($confirm -eq "DELETE") {
            $query = 'DELETE FROM photos WHERE status = ''UPLOADING'';'
            $env:PGPASSWORD = $DB_PASSWORD
            psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
            Write-Host "All UPLOADING photos deleted!" -ForegroundColor Green
        } else {
            Write-Host "Cancelled" -ForegroundColor Red
        }
    }
    "5" {
        Write-Host ""
        Write-Host "WARNING: This will DELETE ALL failed uploads (FAILED only)!" -ForegroundColor Red
        $confirm = Read-Host "Type 'DELETE' to confirm"
        if ($confirm -eq "DELETE") {
            $query = 'DELETE FROM photos WHERE status = ''FAILED'';'
            $env:PGPASSWORD = $DB_PASSWORD
            psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
            Write-Host "All FAILED photos deleted!" -ForegroundColor Green
        } else {
            Write-Host "Cancelled" -ForegroundColor Red
        }
    }
    "6" {
        Write-Host ""
        Write-Host "WARNING: This will DELETE BOTH incomplete AND failed uploads!" -ForegroundColor Red
        $confirm = Read-Host "Type 'DELETE ALL' to confirm"
        if ($confirm -eq "DELETE ALL") {
            $query = 'DELETE FROM photos WHERE status IN (''UPLOADING'', ''FAILED'');'
            $env:PGPASSWORD = $DB_PASSWORD
            psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
            Write-Host "All UPLOADING and FAILED photos deleted!" -ForegroundColor Green
        } else {
            Write-Host "Cancelled" -ForegroundColor Red
        }
    }
    "7" {
        Write-Host ""
        Write-Host "Marking incomplete uploads as FAILED..." -ForegroundColor Yellow
        $confirm = Read-Host "Are you sure? (yes/no)"
        if ($confirm -eq "yes") {
            $query = 'UPDATE photos SET status = ''FAILED'', updated_at = NOW() WHERE status = ''UPLOADING'';'
            $env:PGPASSWORD = $DB_PASSWORD
            psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c $query
            Write-Host "Status updated!" -ForegroundColor Green
        } else {
            Write-Host "Cancelled" -ForegroundColor Red
        }
    }
    "8" {
        Write-Host ""
        Write-Host "Connecting to database..." -ForegroundColor Cyan
        Write-Host "Type 'exit' or press Ctrl+D to disconnect" -ForegroundColor Yellow
        Write-Host ""
        $env:PGPASSWORD = $DB_PASSWORD
        psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
    }
    "9" {
        Write-Host ""
        Write-Host "Goodbye!" -ForegroundColor Cyan
        exit 0
    }
    default {
        Write-Host ""
        Write-Host "Invalid choice" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green

