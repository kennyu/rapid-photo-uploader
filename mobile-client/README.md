# Rapid Photo Upload - Mobile Client

React Native mobile application for uploading and managing photos, built with Expo.

## Features

- 📱 **User Authentication** - Secure login and registration
- 📷 **Batch Photo Upload** - Upload up to 100 photos at once with S3 pre-signed URLs
- 🔄 **Real-time Progress** - Track upload progress for each photo
- 🖼️ **Interactive Gallery** - Browse uploaded photos with thumbnails, tap to view details
- 🔍 **Tag Filtering** - Filter gallery by tags with horizontal scrollable chips
- 🏷️ **Tag Management** - Add and remove tags from photos via modal interface
- 📥 **Photo Downloads** - Download full-resolution photos using pre-signed URLs
  - Web: Opens in new tab
  - Native: Saves directly to device gallery
- 🔐 **Secure Storage** - JWT tokens stored in secure device storage
- 🎯 **Top Navigation Bar** - Easy switching between Upload, Gallery, and Logout

## Tech Stack

- **React Native 0.81.5** - Mobile framework
- **Expo SDK 54** - Development tooling
- **React Navigation v7** - Navigation
- **TypeScript** - Type safety
- **Axios** - HTTP client
- **Expo Image Picker** - Photo selection
- **Expo Camera** - Camera access
- **Expo Secure Store** - Secure token storage
- **Expo File System** - File downloads
- **Expo Media Library** - Saving photos to device gallery

## Prerequisites

- Node.js 18+ and npm
- For iOS: macOS with Xcode
- For Android: Android Studio
- Expo Go app on your phone (for quick testing)

## Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure API URL:**
   Edit `src/config.ts` to point to your backend:
   ```typescript
   // For testing on a real device, use your computer's IP
   apiUrl: 'http://192.168.1.XXX:8080/api'
   ```

3. **Start development server:**
   ```bash
   npm start
   ```

## Running the App

### On Your Phone (Easiest)
1. Install **Expo Go** from App Store (iOS) or Play Store (Android)
2. Run `npm start`
3. Scan the QR code with:
   - **iOS**: Camera app
   - **Android**: Expo Go app

### On iOS Simulator (macOS only)
```bash
npm run ios
```

### On Android Emulator
```bash
npm run android
```

### On Web Browser
```bash
npm run web
```

## Project Structure

```
mobile-client/
├── src/
│   ├── api/              # API client and HTTP utilities
│   │   └── client.ts
│   ├── components/       # Reusable components
│   │   └── AppHeader.tsx # Top navigation bar with logout
│   ├── features/         # Feature-based modules
│   │   ├── auth/        # Authentication screens & logic
│   │   │   ├── LoginScreen.tsx
│   │   │   ├── RegisterScreen.tsx
│   │   │   └── authService.ts
│   │   ├── upload/      # Photo upload functionality
│   │   │   └── UploadScreen.tsx
│   │   └── gallery/     # Photo gallery
│   │       └── GalleryScreen.tsx
│   ├── navigation/       # Navigation configuration
│   │   ├── AppNavigator.tsx  # Main navigation structure
│   │   └── AuthContext.tsx   # Authentication state management
│   ├── types/           # TypeScript type definitions
│   │   ├── api.ts
│   │   └── navigation.ts
│   └── config.ts        # App configuration
├── App.tsx              # Root component
├── app.json             # Expo configuration
├── package.json         # Dependencies
└── tsconfig.json        # TypeScript configuration
```

## Development

### Code Quality
```bash
# Lint code
npm run lint

# Fix linting issues
npm run lint:fix

# Format code
npm run format

# Type check
npm run type-check
```

### Testing on Real Device

When testing on a physical device, you need to:
1. Make sure your device and computer are on the same WiFi network
2. Update `src/config.ts` with your computer's local IP address
3. Ensure your backend is accessible from your device

Example:
```typescript
apiUrl: 'http://192.168.1.100:8080/api' // Replace with your IP
```

To find your IP:
- **macOS/Linux**: `ifconfig | grep "inet "`
- **Windows**: `ipconfig`

## Backend Integration

The mobile app connects to the Spring Boot backend. Ensure:

1. Backend is running on port 8080
2. CORS is configured to allow requests from Expo
3. JWT authentication is working
4. Photo upload endpoint is accessible

### Required Backend Endpoints

**Authentication:**
- `POST /api/v1/auth/login` - User login (expects: `email`, `password`)
- `POST /api/v1/auth/register` - User registration (expects: `email`, `password`, `fullName`)

**Photo Upload:**
- `POST /api/v1/photos/upload/initiate` - Initiate photo upload (returns S3 pre-signed URL)
- `POST /api/v1/uploads/:uploadJobId/complete` - Mark upload as complete after S3 upload

**Gallery & Tags:**
- `GET /api/v1/photos` - Get user's photos (paginated response with downloadUrl and thumbnailUrl)
- `GET /api/v1/photos?tag=tagname` - Filter photos by tag
- `POST /api/v1/photos/:photoId/tags/:tag` - Add a tag to a photo
- `DELETE /api/v1/photos/:photoId/tags/:tag` - Remove a tag from a photo
- `PATCH /api/v1/photos/:photoId/tags` - Bulk replace all tags (array of strings)

**Note:** Backend requires passwords to be at least 8 characters.

### Upload Flow

The app uses a three-step S3 upload process:
1. **Initiate** (10% progress): Request a pre-signed S3 URL from backend via `POST /photos/upload/initiate`
2. **Upload to S3** (30-70% progress): Upload file directly to S3 using the pre-signed URL
3. **Complete** (70-100% progress): Notify backend via `POST /uploads/{uploadJobId}/complete`
4. **Process**: Backend processes the photo and updates status to COMPLETE

## Usage

### Gallery Features

**Viewing Photos:**
- Navigate to the Gallery tab to see all your uploaded photos
- Pull down to refresh the gallery
- Tap any photo to open the detail modal

**Filtering by Tags:**
- Horizontal scrollable tag list appears below the gallery header
- Tap "All" to show all photos (default)
- Tap any tag to filter photos by that tag
- Tag list automatically updates based on all photos in your library
- Current filter is shown in the gallery subtitle

**Managing Tags:**
1. Tap a photo in the gallery to open details
2. View existing tags in the "Tags" section
3. Add new tags:
   - Type a tag name in the input field
   - Press "Add" or hit Enter
4. Remove tags:
   - Tap the "✕" button next to any tag

**Downloading Photos:**
1. Tap a photo to open the detail modal
2. Scroll down and tap "📥 Download Photo"
3. On web: Photo opens in a new tab
4. On native: Photo is saved to your device gallery (requires permission)

## Building for Production

### iOS (macOS only)
```bash
npx expo build:ios
```

### Android
```bash
npx expo build:android
```

## Troubleshooting

### Common Issues

**Metro bundler port conflict:**
```bash
# Kill the process on port 8081
npx react-native start --reset-cache
```

**Permissions not working:**
```bash
# Rebuild the app with updated permissions
npx expo prebuild --clean
```

**Can't connect to backend:**
- Verify backend is running
- Check API URL in `src/config.ts`
- Ensure CORS is configured correctly
- Use computer's IP, not localhost, when testing on device

**Images not uploading:**
- Check photo permissions in app settings
- Verify file size limits on backend
- Check network connectivity

## Features Roadmap

**Completed:**
- [x] User authentication
- [x] Batch photo upload
- [x] Gallery view with thumbnails
- [x] Tag management (add/remove)
- [x] Tag filtering
- [x] Photo downloads

**Future Enhancements:**
- [ ] Offline photo queue
- [ ] Photo compression before upload
- [ ] Direct camera integration
- [ ] Bulk tag editing
- [ ] Photo deletion
- [ ] Search by filename
- [ ] Dark mode

## License

MIT

