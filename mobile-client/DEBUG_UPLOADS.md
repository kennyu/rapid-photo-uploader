# Debugging Mobile Upload Failures

This guide helps you troubleshoot photo upload issues in the mobile app.

## Common Failure Causes

### 1. **File Size = 0 (Backend Validation Error)**
**Symptom:** Upload fails at Step 1 (Initiate)
**Error:** `"File size must be positive"`
**Fix:** ✅ FIXED - App now fetches blob to determine actual size

### 2. **S3 Upload Failed**
**Symptom:** Upload fails at Step 2 (S3 Upload)
**Error:** `S3 upload failed: 403 Forbidden` or `SignatureDoesNotMatch`
**Causes:**
- Pre-signed URL expired (URLs valid for 1 hour)
- Content-Type mismatch
- Clock skew between device and S3

**Fix:**
- Check device time is correct
- Ensure Content-Type matches what backend expects
- Try upload within 1 hour of initiating

### 3. **Backend Complete Notification Failed**
**Symptom:** Upload succeeds to S3 but stays in UPLOADING status
**Error:** `POST /uploads/{uploadJobId}/complete` fails
**Causes:**
- Authentication token expired
- Network timeout
- Backend not reachable

**Fix:**
- Check JWT token is valid (login again if needed)
- Verify backend is running
- Check network connectivity

### 4. **CORS Issues (Web Only)**
**Symptom:** Upload fails with CORS error in browser console
**Error:** `Access-Control-Allow-Origin` error
**Fix:**
- Backend needs CORS configured for Expo web domain
- Check `application.properties` CORS settings

## How to Debug

### Step 1: Open Browser Console (Web) or Metro Logs (Mobile)

**For Web:**
1. Open the app in Chrome
2. Press F12 → Console tab
3. Try uploading a photo
4. Look for `[Upload 0]` log messages

**For Native (iOS/Android):**
1. Check the Metro bundler terminal output
2. Look for `[Upload 0]` log messages

### Step 2: Identify Which Step Failed

The logs will show:
```
[Upload 0] Starting upload for image.jpg
[Upload 0] File size: 2451234, Content-Type: image/jpeg
[Upload 0] Step 1: Initiating upload...
[Upload 0] Step 1 ✓: Got presigned URL and uploadJobId: xyz-123
[Upload 0] Step 2: Fetching file blob for upload...
[Upload 0] Step 2: Blob size: 2451234, type: image/jpeg
[Upload 0] Step 2: Uploading to S3...
[Upload 0] Step 2 ✓: S3 upload successful (200)
[Upload 0] Step 3: Notifying backend of completion...
[Upload 0] Step 3 ✓: Backend notified
[Upload 0] ✅ Upload complete!
```

If it fails, you'll see:
```
[Upload 0] ❌ Upload failed: Error message here
[Upload 0] Error details: { message, response, status }
```

### Step 3: Check Backend Logs

```powershell
# View backend logs
cd backend
mvn spring-boot:run

# Look for:
# - "Initiating upload for user: {userId}, file: {filename}"
# - "Generated pre-signed URL for photo: {photoId}"
# - Any error messages
```

### Step 4: Check Database

```powershell
# Run the cleanup script to see stuck uploads
.\tests\cleanup-database.ps1

# Select option 1 to view incomplete/failed uploads
```

## Testing Checklist

- [ ] Backend is running on http://localhost:8080
- [ ] Mobile app config points to correct API URL
- [ ] User is logged in (JWT token valid)
- [ ] Device/computer time is correct (for S3 signatures)
- [ ] S3 bucket exists and has correct permissions
- [ ] AWS credentials are configured in backend
- [ ] Network allows access to AWS S3

## Manual Test

Try this curl command to test the backend directly:

```bash
# 1. Login and get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Save the token from response

# 2. Initiate upload
curl -X POST http://localhost:8080/api/v1/photos/upload/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "filename": "test.jpg",
    "fileSize": 123456,
    "contentType": "image/jpeg"
  }'

# You should get: presignedUrl, photoId, uploadJobId
```

## Quick Fixes

### Clear Failed Uploads
```powershell
.\tests\cleanup-database.ps1
# Select option 5 or 6
```

### Reset Everything
```powershell
# Stop backend
# Clear database
.\tests\cleanup-database.ps1

# Restart backend
cd backend
mvn spring-boot:run

# Restart mobile app
cd mobile-client
npm start
```

## Still Having Issues?

Check the detailed logs:
1. Mobile app console (`[Upload X]` messages)
2. Backend console (Spring Boot logs)
3. Database (run cleanup script option 1)

Look for specific error messages and match them to the "Common Failure Causes" section above.

