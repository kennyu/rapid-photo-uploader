# Mobile E2E Tests with Detox

Comprehensive end-to-end test suite for the Rapid Photo Uploader mobile client using Detox.

## Test Coverage

### 🔐 Authentication Tests (`auth.test.ts`)
- **Registration** (4 tests)
  - ✅ Display register screen
  - ✅ Successful user registration
  - ✅ Validation error handling
  - ✅ Password strength validation
- **Login** (3 tests)
  - ✅ Successful login
  - ✅ Invalid credentials error
  - ✅ Empty field validation
- **Logout** (1 test)
  - ✅ Logout with confirmation
- **Protected Routes** (2 tests)
  - ✅ Prevent unauthorized access
  - ✅ Allow navigation when authenticated

### 📤 Upload Tests (`upload.test.ts`)
- **Photo Selection** (3 tests)
  - ✅ Display upload screen
  - ✅ Open photo picker
  - ✅ Camera access
- **Upload Progress** (3 tests)
  - ✅ Show progress indicators
  - ✅ Display status (uploading/complete/failed)
  - ✅ Show progress percentage
- **Upload Actions** (3 tests)
  - ✅ Cancel upload
  - ✅ Retry failed upload
  - ✅ Clear completed uploads
- **Multiple Photos** (2 tests)
  - ✅ Handle multiple selections
  - ✅ Show total progress
- **Error Handling** (3 tests)
  - ✅ Network errors
  - ✅ File size validation
  - ✅ Backend errors
- **UI Feedback** (3 tests)
  - ✅ Disable buttons during upload
  - ✅ Success messages
  - ✅ Gallery count updates

### 🖼️ Gallery Tests (`gallery.test.ts`)
- **Display** (6 tests)
  - ✅ Gallery screen
  - ✅ Empty state
  - ✅ Photo list
  - ✅ Thumbnails
  - ✅ Filenames
  - ✅ Status indicators
- **Pull to Refresh** (1 test)
  - ✅ Refresh functionality
- **Tag Filtering** (6 tests)
  - ✅ Display filter chips
  - ✅ Show all tags
  - ✅ Filter by tag
  - ✅ Clear filter
  - ✅ Filtered photo count
  - ✅ Empty filter state
- **Photo Details** (4 tests)
  - ✅ Open modal
  - ✅ Display information
  - ✅ Photo preview
  - ✅ Close modal
- **Tag Management** (6 tests)
  - ✅ Display existing tags
  - ✅ Add tag
  - ✅ Remove tag
  - ✅ Success messages
  - ✅ Error handling
  - ✅ Empty tags state
- **Photo Download** (5 tests)
  - ✅ Show download button
  - ✅ Download photo
  - ✅ Success message
  - ✅ Error handling
  - ✅ Permission requests
- **Performance** (2 tests)
  - ✅ Smooth scrolling
  - ✅ Efficient loading
- **Navigation** (3 tests)
  - ✅ Navigate to upload
  - ✅ Navigate to gallery
  - ✅ Maintain state
- **Empty States** (2 tests)
  - ✅ Empty gallery message
  - ✅ Empty filter message

**Total:** 58+ E2E test scenarios

## Prerequisites

### Required Software
- **Node.js 18+**
- **npm or yarn**
- **Xcode** (for iOS testing on macOS)
- **Android Studio** (for Android testing)
- **Expo CLI**

### iOS Requirements
- macOS with Xcode installed
- iOS Simulator
- CocoaPods (`sudo gem install cocoapods`)

### Android Requirements
- Android Studio with SDK
- Android Emulator (AVD)
- Java Development Kit (JDK) 11+

## Installation

```bash
cd mobile-client
npm install
```

This installs:
- `detox` - E2E testing framework
- `jest` - Test runner
- `ts-jest` - TypeScript support for Jest

### Initial Setup

#### For iOS:
```bash
# Install CocoaPods dependencies
cd ios && pod install && cd ..

# Build the app
npm run test:e2e:build:ios
```

#### For Android:
```bash
# Build the app
npm run test:e2e:build:android
```

## Running Tests

### iOS Tests

**Prerequisites:**
- Xcode must be open at least once
- iOS Simulator must be installed

```bash
# Build app for testing (first time or after code changes)
npm run test:e2e:build:ios

# Run tests
npm run test:e2e
```

### Android Tests

**Prerequisites:**
- Android emulator must be running
- Or create AVD named `Pixel_5_API_34`

```bash
# Start emulator (if not running)
emulator -avd Pixel_5_API_34

# Build app for testing
npm run test:e2e:build:android

# Run tests
npm run test:e2e:android
```

### Run Specific Test File

```bash
# iOS
npx detox test e2e/auth.test.ts --configuration ios.sim.debug

# Android
npx detox test e2e/gallery.test.ts --configuration android.emu.debug
```

### Debug Mode

```bash
# Run with verbose logging
npx detox test --configuration ios.sim.debug --loglevel verbose

# Take screenshots on failure
npx detox test --configuration ios.sim.debug --take-screenshots failing
```

### Headless Mode (CI/CD)

```bash
# iOS (with xcpretty for cleaner output)
gem install xcpretty
npm run test:e2e -- --headless

# Android
npm run test:e2e:android -- --headless
```

## Configuration

### `.detoxrc.js`

Main configuration file:

```javascript
{
  apps: {
    'ios.debug': { /* iOS app config */ },
    'android.debug': { /* Android app config */ }
  },
  devices: {
    simulator: { type: 'iPhone 15' },
    emulator: { avdName: 'Pixel_5_API_34' }
  },
  configurations: {
    'ios.sim.debug': { device: 'simulator', app: 'ios.debug' },
    'android.emu.debug': { device: 'emulator', app: 'android.debug' }
  }
}
```

### Jest Configuration

Located in `e2e/jest.config.js`:
- Test timeout: 120 seconds
- Single worker (tests run sequentially)
- TypeScript support via ts-jest

## Test ID Strategy

Add `testID` props to components for reliable element selection:

```typescript
// In React Native components
<TouchableOpacity testID="login-button">
  <Text>Login</Text>
</TouchableOpacity>

// In tests
await element(by.id('login-button')).tap();
```

### Recommended Test IDs

**Authentication:**
- `email-input`
- `password-input`
- `fullName-input`
- `login-button`
- `register-button`
- `logout-button`

**Upload:**
- `select-photos-button`
- `camera-button`
- `upload-progress-{index}`
- `upload-list`
- `cancel-upload-{index}`
- `clear-completed-button`

**Gallery:**
- `photo-list`
- `photo-item-{index}`
- `photo-thumbnail-{index}`
- `tag-filter-chips`
- `modal-photo-preview`
- `tag-input`
- `download-button`

## Best Practices

### 1. Test Isolation
Each test should be independent:

```typescript
beforeEach(async () => {
  await device.reloadReactNative();
});
```

### 2. Wait for Elements
Use `waitFor` for async operations:

```typescript
await waitFor(element(by.text('Loading...')))
  .not.toBeVisible()
  .withTimeout(5000);
```

### 3. Descriptive Test Names
```typescript
it('should show validation error for invalid email format', async () => {
  // Test implementation
});
```

### 4. Use Test IDs Over Text
Prefer test IDs for stability across languages:

```typescript
// Good
await element(by.id('login-button')).tap();

// Avoid (breaks if text changes)
await element(by.text('Login')).tap();
```

### 5. Clean Up After Tests
```typescript
afterAll(async () => {
  await device.terminate();
});
```

## Debugging

### View Test Logs
```bash
npx detox test --configuration ios.sim.debug --loglevel verbose
```

### Take Screenshots
```bash
npx detox test --take-screenshots all
# Screenshots saved to: artifacts/
```

### Record Video
```bash
npx detox test --record-videos all
# Videos saved to: artifacts/
```

### Debug in Xcode/Android Studio
1. Build the app with debug configuration
2. Open in respective IDE
3. Attach debugger
4. Run tests

### Common Issues

**iOS: "Unable to boot simulator"**
```bash
# Reset simulator
xcrun simctl erase all
```

**Android: "No connected devices"**
```bash
# List available AVDs
emulator -list-avds

# Start specific AVD
emulator -avd Pixel_5_API_34
```

**Detox: "DetoxRuntimeError: Failed to run application"**
```bash
# Clean and rebuild
npm run test:e2e:build:ios
# or
npm run test:e2e:build:android
```

**Element not found**
- Verify `testID` is set on the element
- Use `waitFor` for async rendering
- Check element visibility conditions

## Continuous Integration

### GitHub Actions Example

```yaml
name: Mobile E2E Tests

on: [push, pull_request]

jobs:
  ios:
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd mobile-client
          npm ci
      
      - name: Install CocoaPods
        run: |
          cd mobile-client/ios
          pod install
      
      - name: Build for Detox
        run: |
          cd mobile-client
          npm run test:e2e:build:ios
      
      - name: Run E2E tests
        run: |
          cd mobile-client
          npm run test:e2e -- --headless
      
      - name: Upload artifacts
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: detox-artifacts
          path: mobile-client/artifacts

  android:
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '11'
      
      - name: Install dependencies
        run: |
          cd mobile-client
          npm ci
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
      
      - name: Create AVD
        run: |
          $ANDROID_SDK_ROOT/emulator/emulator -list-avds || \
          echo "no" | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager create avd \
            -n Pixel_5_API_34 -k "system-images;android-34;google_apis;x86_64"
      
      - name: Start emulator
        run: |
          $ANDROID_SDK_ROOT/emulator/emulator -avd Pixel_5_API_34 -no-window -no-audio &
          adb wait-for-device
      
      - name: Build for Detox
        run: |
          cd mobile-client
          npm run test:e2e:build:android
      
      - name: Run E2E tests
        run: |
          cd mobile-client
          npm run test:e2e:android -- --headless
```

## Performance Tips

1. **Reuse App Instance:**
   ```typescript
   beforeAll(async () => {
     await device.launchApp({ newInstance: true });
   });

   beforeEach(async () => {
     await device.reloadReactNative(); // Faster than relaunching
   });
   ```

2. **Run Tests in Parallel:**
   ```bash
   npx detox test --configuration ios.sim.debug --workers 2
   ```

3. **Optimize Builds:**
   - Use debug builds for faster compilation
   - Cache build artifacts in CI

## Adding New Tests

1. Create test file in `e2e/`:

```typescript
import { by, device, element, expect } from 'detox';

describe('Feature Name', () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: true });
  });

  it('should do something', async () => {
    await expect(element(by.text('Hello'))).toBeVisible();
  });
});
```

2. Add test IDs to components
3. Run tests: `npm run test:e2e`

## Resources

- [Detox Documentation](https://wix.github.io/Detox/)
- [Detox API Reference](https://wix.github.io/Detox/docs/api/detox-object-api)
- [Detox GitHub](https://github.com/wix/Detox)
- [Expo + Detox Guide](https://docs.expo.dev/build-reference/e2e-tests/)
- [Best Practices](https://wix.github.io/Detox/docs/guide/test-id-usage)

