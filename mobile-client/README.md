# Rapid Photo Upload - Mobile Client

React Native mobile application for uploading and managing photos, built with Expo.

## Features

- 📱 **User Authentication** - Secure login and registration
- 📷 **Batch Photo Upload** - Upload up to 100 photos at once with S3 pre-signed URLs
- 🔄 **Real-time Progress** - Track upload progress for each photo
- 🖼️ **Gallery View** - Browse uploaded photos with thumbnails
- 🏷️ **Photo Tagging** - Organize photos with tags
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

- `POST /api/v1/auth/login` - User login (expects: `email`, `password`)
- `POST /api/v1/auth/register` - User registration (expects: `email`, `password`, `fullName`)
- `POST /api/v1/photos/upload/initiate` - Initiate photo upload (returns S3 pre-signed URL)
- `POST /api/v1/uploads/:uploadJobId/complete` - Mark upload as complete after S3 upload
- `GET /api/v1/photos` - Get user's photos (paginated response)
- `PATCH /api/v1/photos/:id` - Update photo tags

**Note:** Backend requires passwords to be at least 8 characters.

### Upload Flow

The app uses a three-step S3 upload process:
1. **Initiate** (10% progress): Request a pre-signed S3 URL from backend via `POST /photos/upload/initiate`
2. **Upload to S3** (30-70% progress): Upload file directly to S3 using the pre-signed URL
3. **Complete** (70-100% progress): Notify backend via `POST /uploads/{uploadJobId}/complete`
4. **Process**: Backend processes the photo and updates status to COMPLETE

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

- [ ] Offline photo queue
- [ ] Photo compression before upload
- [ ] Camera integration
- [ ] Bulk tag editing
- [ ] Photo deletion
- [ ] Search and filter
- [ ] Dark mode

## License

MIT

