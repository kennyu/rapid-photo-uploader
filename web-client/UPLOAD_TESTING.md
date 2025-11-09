# Upload Feature Testing Guide

## Prerequisites

1. ✅ **Backend Running**: Make sure the backend is running on `http://localhost:8080`
   ```powershell
   cd backend
   .\run.bat
   ```

2. ✅ **Web Client Running**: Start the dev server
   ```powershell
   cd web-client
   npm run dev
   ```

3. ✅ **AWS Services**: Ensure RDS PostgreSQL and S3 bucket are configured
   - RDS: `rapidphoto-db.c5kiq8ygqqij.us-east-1.rds.amazonaws.com`
   - S3: `rapidphoto-uploads-297721440242`

---

## Testing Steps

### 1. **Register a New User**

1. Open `http://localhost:3000` (or your Vite dev server URL)
2. Click **Register** in the navigation
3. Fill out the form:
   - **Full Name**: Test User
   - **Email**: test@example.com
   - **Password**: Test123! (min 8 characters)
   - **Confirm Password**: Test123!
4. Click **Register**
5. Page should reload and you'll be logged in automatically

### 2. **Login** (if you already have an account)

1. Click **Login** in the navigation
2. Enter your **email** and **password**
3. Click **Login**
4. Page should reload and you'll see "Welcome, [Your Name]!"

### 3. **Upload Photos**

1. Click **Upload** in the navigation
2. You'll see the Uppy.js dashboard
3. **Select photos** using one of these methods:
   - Click "Browse files" and select up to 100 images
   - **Drag and drop** images directly onto the upload area
   - Take a photo with your webcam (if available)

4. **Optional: Edit photos**
   - Click the **pencil icon** on any photo to open the image editor
   - Crop, rotate, or adjust before uploading

5. **Optional: Add metadata**
   - Click on a photo to add a name or caption

6. Click **Upload** button
7. Watch the progress bars for each file
8. You'll see:
   - Individual file progress
   - Overall upload statistics
   - Success/failure alerts

### 4. **Verify Upload Success**

**Check the browser console:**
```
Upload successful: photo1.jpg
Upload complete: {successful: [...], failed: [...]}
```

**Check S3 bucket:**
```powershell
aws s3 ls s3://rapidphoto-uploads-297721440242/
```

**Check backend logs:**
```
Generated pre-signed URL for photo: photo1.jpg
Photo metadata saved with ID: ...
```

---

## Features to Test

### ✅ File Restrictions
- ✅ Max 100 files at once
- ✅ Max 10MB per file
- ✅ Images only (JPEG, PNG, GIF, etc.)
- ❌ Try uploading a PDF or video - should be rejected

### ✅ Image Editor
- ✅ Crop photos before upload
- ✅ Rotate photos
- ✅ Adjust quality (set to 0.8 = 80%)

### ✅ Progress Tracking
- ✅ Individual file progress bars
- ✅ Overall upload statistics
- ✅ Success/failure notifications

### ✅ Authentication
- ✅ Cannot access upload page without logging in
- ✅ JWT token stored in localStorage
- ✅ Token sent with every upload request
- ✅ Logout clears token and redirects to login

### ✅ Direct S3 Upload
- ✅ Backend generates pre-signed URL
- ✅ Client uploads directly to S3 (no backend bottleneck)
- ✅ Fast parallel uploads

---

## Troubleshooting

### **Upload fails with 401 Unauthorized**
- **Cause**: JWT token is missing or expired
- **Fix**: Logout and login again

### **Upload fails with 403 Forbidden**
- **Cause**: S3 bucket CORS not configured
- **Fix**: Run `aws s3api put-bucket-cors --bucket rapidphoto-uploads-297721440242 --cors-configuration file://s3-cors-config.json`

### **Upload fails with "Network Error"**
- **Cause**: Backend not running
- **Fix**: Start backend with `cd backend && .\run.bat`

### **"Failed to get upload URL"**
- **Cause**: Backend can't generate pre-signed URL
- **Fix**: Check AWS credentials are configured (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)

### **Photos upload but don't show in gallery**
- **Cause**: Backend processed photo but UI not refreshing
- **Fix**: Click **Gallery** to see uploaded photos
- **Note**: Gallery integration is in Task 13

---

## API Flow

### **1. Initiate Upload**
```
POST /api/v1/photos/upload/initiate
Headers: Authorization: Bearer <jwt_token>
Body: {
  "filename": "photo1.jpg",
  "fileSize": 1024000,
  "contentType": "image/jpeg"
}

Response: {
  "photoId": "uuid",
  "preSignedUrl": "https://s3.amazonaws.com/...",
  "expiresIn": 3600
}
```

### **2. Upload to S3**
```
PUT <preSignedUrl>
Headers: Content-Type: image/jpeg
Body: <binary image data>

Response: 200 OK
```

### **3. Backend Processing** (Automatic)
- Image compression (85% quality)
- Thumbnail generation (300px)
- Optional AI tagging (if enabled)
- Status update to "COMPLETED"

---

## Next Steps

After testing uploads:
- ✅ **Task 11.1**: Integrate Uppy.js (COMPLETE)
- ⏱️ **Task 11.2**: Implement batch upload with progress tracking
- ⏱️ **Task 11.3**: Add retry logic for failed uploads
- ⏱️ **Task 13**: Build photo gallery with download

---

## Notes

- Uploads are asynchronous - you can continue browsing while photos upload
- Files are compressed after upload to save storage
- Thumbnails are generated automatically
- AI tagging is disabled by default (set `image.tagging.enabled=true` to enable)

