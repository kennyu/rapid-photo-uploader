import { by, device, element, expect } from 'detox';

describe('Authentication Flow', () => {
  beforeAll(async () => {
    await device.launchApp({
      newInstance: true,
      permissions: { photos: 'YES', camera: 'YES' },
    });
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  describe('Registration', () => {
    it('should display register screen', async () => {
      // Wait for login screen to load
      await expect(element(by.text('Login'))).toBeVisible();
      
      // Tap register button/link
      await element(by.text('Register')).tap();
      
      // Should navigate to register screen
      await expect(element(by.text('Create Account'))).toBeVisible();
    });

    it('should successfully register a new user', async () => {
      const email = `test-${Date.now()}@example.com`;
      const password = 'TestPassword123!';
      const fullName = 'Test User';

      // Navigate to register
      await element(by.text('Register')).tap();

      // Fill in registration form
      await element(by.id('email-input')).typeText(email);
      await element(by.id('fullName-input')).typeText(fullName);
      await element(by.id('password-input')).typeText(password);

      // Submit registration
      await element(by.id('register-button')).tap();

      // Should redirect to upload screen
      await expect(element(by.text('Upload Photos'))).toBeVisible();
    });

    it('should show validation errors for invalid input', async () => {
      await element(by.text('Register')).tap();

      // Try to submit with empty fields
      await element(by.id('register-button')).tap();

      // Should show validation error
      await expect(element(by.text('Email is required'))).toBeVisible();
    });

    it('should validate password strength', async () => {
      await element(by.text('Register')).tap();

      await element(by.id('email-input')).typeText('test@example.com');
      await element(by.id('fullName-input')).typeText('Test User');
      await element(by.id('password-input')).typeText('weak'); // Too short
      
      await element(by.id('register-button')).tap();

      // Should show password validation error
      await expect(element(by.text(/password.*8.*characters/i))).toBeVisible();
    });
  });

  describe('Login', () => {
    const testEmail = `login-test-${Date.now()}@example.com`;
    const testPassword = 'TestPassword123!';

    beforeAll(async () => {
      // Create a test user
      await element(by.text('Register')).tap();
      await element(by.id('email-input')).typeText(testEmail);
      await element(by.id('fullName-input')).typeText('Login Test User');
      await element(by.id('password-input')).typeText(testPassword);
      await element(by.id('register-button')).tap();

      // Wait for registration to complete
      await expect(element(by.text('Upload Photos'))).toBeVisible();

      // Logout
      await element(by.text('Logout')).tap();
    });

    it('should successfully login with valid credentials', async () => {
      await expect(element(by.text('Login'))).toBeVisible();

      await element(by.id('email-input')).typeText(testEmail);
      await element(by.id('password-input')).typeText(testPassword);
      await element(by.id('login-button')).tap();

      // Should redirect to upload screen
      await expect(element(by.text('Upload Photos'))).toBeVisible();
    });

    it('should show error for invalid credentials', async () => {
      await element(by.id('email-input')).typeText(testEmail);
      await element(by.id('password-input')).typeText('wrongpassword');
      await element(by.id('login-button')).tap();

      // Should show error message
      await expect(element(by.text('Invalid credentials'))).toBeVisible();
    });

    it('should show validation errors for empty fields', async () => {
      await element(by.id('login-button')).tap();

      // Should show validation error
      await expect(element(by.text('Email is required'))).toBeVisible();
    });
  });

  describe('Logout', () => {
    it('should logout and return to login screen', async () => {
      // Assume we're logged in from previous tests
      await expect(element(by.text('Upload Photos'))).toBeVisible();

      // Tap logout button
      await element(by.text('Logout')).tap();

      // Should show confirmation dialog
      await expect(element(by.text('Are you sure you want to log out?'))).toBeVisible();
      await element(by.text('Logout')).atIndex(1).tap(); // Confirm logout

      // Should return to login screen
      await expect(element(by.text('Login'))).toBeVisible();
    });
  });

  describe('Protected Routes', () => {
    it('should prevent access to upload without authentication', async () => {
      // Ensure we're logged out
      await device.reloadReactNative();

      // Should show login screen
      await expect(element(by.text('Login'))).toBeVisible();
      
      // Upload screen should not be accessible
      await expect(element(by.text('Upload Photos'))).not.toBeVisible();
    });

    it('should allow navigation between screens when authenticated', async () => {
      const email = `nav-test-${Date.now()}@example.com`;
      
      // Login
      await element(by.text('Register')).tap();
      await element(by.id('email-input')).typeText(email);
      await element(by.id('fullName-input')).typeText('Nav Test');
      await element(by.id('password-input')).typeText('TestPassword123!');
      await element(by.id('register-button')).tap();

      // Should be on upload screen
      await expect(element(by.text('Upload Photos'))).toBeVisible();

      // Navigate to gallery
      await element(by.text('Gallery')).tap();
      await expect(element(by.text('Gallery'))).toBeVisible();

      // Navigate back to upload
      await element(by.text('Upload')).tap();
      await expect(element(by.text('Upload Photos'))).toBeVisible();
    });
  });
});

