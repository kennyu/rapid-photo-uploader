describe('Authentication', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    cy.clearLocalStorage();
    cy.visit('/');
  });

  describe('Registration', () => {
    it('should successfully register a new user', () => {
      const email = `test-${Date.now()}@example.com`;
      const password = 'TestPassword123!';
      const fullName = 'Test User';

      // Navigate to register page
      cy.contains('Register').click();
      cy.url().should('include', '/register');

      // Fill in registration form
      cy.get('input[type="email"]').type(email);
      cy.get('input[name="fullName"]').type(fullName);
      cy.get('input[type="password"]').first().type(password);

      // Submit form
      cy.contains('button', 'Register').click();

      // Should redirect to upload page
      cy.url().should('include', '/upload');
      
      // Should have auth token
      cy.getAuthToken().should('not.be.null');
    });

    it('should show validation errors for invalid input', () => {
      cy.contains('Register').click();

      // Try to submit with empty fields
      cy.contains('button', 'Register').click();

      // Should show validation errors
      cy.contains('Email is required').should('be.visible');
    });

    it('should not allow duplicate email registration', () => {
      const email = `duplicate-${Date.now()}@example.com`;
      const password = 'TestPassword123!';

      // Register first time
      cy.register(email, password, 'Test User');

      // Try to register again with same email
      cy.visit('/register');
      cy.get('input[type="email"]').type(email);
      cy.get('input[name="fullName"]').type('Another User');
      cy.get('input[type="password"]').first().type(password);
      cy.contains('button', 'Register').click();

      // Should show error
      cy.contains('Email already exists').should('be.visible');
    });
  });

  describe('Login', () => {
    const email = `login-test-${Date.now()}@example.com`;
    const password = 'TestPassword123!';

    beforeEach(() => {
      // Create a user for login tests
      cy.register(email, password, 'Login Test User');
      cy.clearLocalStorage(); // Clear token after registration
    });

    it('should successfully login with valid credentials', () => {
      cy.visit('/login');

      cy.get('input[type="email"]').type(email);
      cy.get('input[type="password"]').type(password);
      cy.contains('button', 'Login').click();

      // Should redirect to upload page
      cy.url().should('include', '/upload');
      
      // Should have auth token
      cy.getAuthToken().should('not.be.null');
    });

    it('should show error for invalid credentials', () => {
      cy.visit('/login');

      cy.get('input[type="email"]').type(email);
      cy.get('input[type="password"]').type('wrongpassword');
      cy.contains('button', 'Login').click();

      // Should show error
      cy.contains('Invalid credentials').should('be.visible');
    });

    it('should show validation errors for empty fields', () => {
      cy.visit('/login');

      cy.contains('button', 'Login').click();

      // Should show validation errors
      cy.contains('Email is required').should('be.visible');
    });
  });

  describe('Logout', () => {
    it('should logout and redirect to login page', () => {
      const email = `logout-test-${Date.now()}@example.com`;
      cy.register(email, 'TestPassword123!', 'Logout Test');

      cy.visit('/upload');

      // Click logout button
      cy.contains('Logout').click();

      // Should redirect to login
      cy.url().should('include', '/login');
      
      // Should not have auth token
      cy.getAuthToken().should('be.null');
    });
  });

  describe('Protected Routes', () => {
    it('should redirect to login when accessing protected route without auth', () => {
      cy.visit('/upload');

      // Should redirect to login
      cy.url().should('include', '/login');
    });

    it('should allow access to protected routes with valid token', () => {
      const email = `protected-test-${Date.now()}@example.com`;
      cy.register(email, 'TestPassword123!', 'Protected Test');

      cy.visit('/upload');
      cy.url().should('include', '/upload');

      cy.visit('/gallery');
      cy.url().should('include', '/gallery');
    });
  });
});

