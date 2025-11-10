# Web Client E2E Tests with Cypress

Comprehensive end-to-end test suite for the Rapid Photo Uploader web client using Cypress.

## Test Coverage

### 🔐 Authentication Tests (`auth.cy.ts`)
- **Registration**
  - ✅ Successful user registration
  - ✅ Validation error handling
  - ✅ Duplicate email prevention
- **Login**
  - ✅ Successful login with valid credentials
  - ✅ Error handling for invalid credentials
  - ✅ Empty field validation
- **Logout**
  - ✅ Logout functionality and token cleanup
- **Protected Routes**
  - ✅ Redirect to login when unauthorized
  - ✅ Access allowed with valid token

### 📤 Upload Tests (`upload.cy.ts`)
- **File Selection**
  - ✅ File selection via button
  - ✅ Multiple file upload
  - ✅ File type validation
- **Upload Process**
  - ✅ Upload progress tracking
  - ✅ Error handling
  - ✅ Completed upload cleanup

### 🖼️ Gallery Tests (`gallery.cy.ts`)
- **Display**
  - ✅ Empty gallery message
  - ✅ Photo grid display
  - ✅ Thumbnail rendering
  - ✅ Pagination
- **Tag Management**
  - ✅ Display existing tags
  - ✅ Tag edit modal
  - ✅ Add new tags
  - ✅ Remove tags
  - ✅ Tag filtering
  - ✅ Clear tag filter
- **Photo Actions**
  - ✅ Photo download
  - ✅ Navigation between pages

## Prerequisites

- **Node.js 18+**
- **npm**
- **Backend API** running on `http://localhost:8080`
- **Web client** dev server on `http://localhost:5173`

## Installation

```bash
cd web-client
npm install
```

This installs:
- `cypress` - E2E testing framework
- `start-server-and-test` - Utility to start dev server and run tests

## Running Tests

### Interactive Mode (Recommended for Development)

Opens Cypress Test Runner with GUI:

```bash
npm run cypress
```

Features:
- Watch mode - tests rerun on file changes
- Time travel debugging
- Network request inspection
- Screenshot/video review

### Headless Mode (CI/CD)

Runs all tests in headless Chrome:

```bash
npm run cypress:headless
```

### Full E2E Test Suite

Starts dev server, runs tests, then stops server:

```bash
npm run test:e2e
```

This is the recommended command for CI/CD pipelines.

### Run Specific Test File

```bash
npx cypress run --spec "cypress/e2e/auth.cy.ts"
npx cypress run --spec "cypress/e2e/upload.cy.ts"
npx cypress run --spec "cypress/e2e/gallery.cy.ts"
```

### Run Tests in Specific Browser

```bash
npx cypress run --browser chrome
npx cypress run --browser firefox
npx cypress run --browser edge
```

## Configuration

### `cypress.config.ts`

```typescript
{
  baseUrl: 'http://localhost:5173',    // Dev server URL
  env: {
    apiUrl: 'http://localhost:8080/api/v1'  // Backend API URL
  },
  viewportWidth: 1280,
  viewportHeight: 720,
  video: false,                        // Disable video recording
  screenshotOnRunFailure: true,        // Screenshot on failure
  defaultCommandTimeout: 10000         // 10s timeout
}
```

### Custom Commands

Located in `cypress/support/commands.ts`:

#### `cy.register(email, password, fullName)`
Registers a new user via API and stores auth token.

```typescript
cy.register('test@example.com', 'password123', 'Test User');
```

#### `cy.login(email, password)`
Logs in a user via API and stores auth token.

```typescript
cy.login('test@example.com', 'password123');
```

#### `cy.getAuthToken()`
Retrieves auth token from localStorage.

```typescript
cy.getAuthToken().then((token) => {
  // Use token
});
```

## Test Structure

```
cypress/
├── e2e/                    # Test files
│   ├── auth.cy.ts         # Authentication tests
│   ├── upload.cy.ts       # Upload flow tests
│   └── gallery.cy.ts      # Gallery & tag tests
├── support/
│   ├── commands.ts        # Custom commands
│   └── e2e.ts            # Support file
├── fixtures/              # Test data (optional)
└── README.md             # This file
```

## Best Practices

### 1. Test Isolation
Each test should:
- Start from a clean state
- Not depend on other tests
- Clean up after itself

```typescript
beforeEach(() => {
  cy.clearLocalStorage();
  cy.visit('/');
});
```

### 2. Use Data Attributes
Prefer `data-testid` attributes over classes or text:

```typescript
cy.get('[data-testid="upload-progress"]').should('exist');
```

### 3. Intercept Network Requests
Mock API responses for predictable tests:

```typescript
cy.intercept('GET', '**/photos*', {
  statusCode: 200,
  body: { content: [...] }
}).as('getPhotos');

cy.wait('@getPhotos');
```

### 4. Use Custom Commands
Reuse common flows:

```typescript
cy.register(email, password, name);  // Instead of repeating form filling
```

### 5. Assertions
Use descriptive assertions:

```typescript
cy.contains('Upload Photos').should('be.visible');
cy.url().should('include', '/gallery');
```

## Debugging

### Failed Tests
Screenshots are saved to: `cypress/screenshots/`

### Network Requests
In Cypress Test Runner:
- Click on test step
- View request/response in command log
- Inspect network tab

### Pause Execution
Add `.debug()` to pause:

```typescript
cy.get('button').debug().click();
```

### Console Logs
View browser console in Cypress Test Runner:
```typescript
cy.window().then((win) => {
  console.log(win);
});
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  cypress:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd web-client
          npm ci
      
      - name: Start backend
        run: |
          cd backend
          ./gradlew bootRun &
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb
      
      - name: Run E2E tests
        run: |
          cd web-client
          npm run test:e2e
      
      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: cypress-screenshots
          path: web-client/cypress/screenshots
```

## Common Issues

### Port Already in Use
```bash
# Kill process on port 5173
npx kill-port 5173

# Or use different port
vite --port 3000
```

### Backend Not Running
Ensure backend is running on port 8080:
```bash
cd backend
./gradlew bootRun
```

### Tests Timing Out
Increase timeout in test:
```typescript
cy.contains('element', { timeout: 20000 }).should('be.visible');
```

Or globally in `cypress.config.ts`:
```typescript
{
  defaultCommandTimeout: 15000
}
```

### CORS Errors
Ensure backend CORS is configured to allow `http://localhost:5173`

## Performance

- **Typical run time**: ~2-3 minutes for full suite
- **Parallel execution**: Use `cypress run --parallel` with CI (requires Cypress Dashboard)
- **Test retries**: Configure in `cypress.config.ts`:

```typescript
{
  retries: {
    runMode: 2,    // Retry 2x in headless
    openMode: 0    // No retry in interactive
  }
}
```

## Adding New Tests

1. Create test file in `cypress/e2e/`:

```typescript
describe('Feature Name', () => {
  beforeEach(() => {
    // Setup
  });

  it('should do something', () => {
    // Test
  });
});
```

2. Run test:

```bash
npm run cypress
```

3. Add to CI pipeline

## Resources

- [Cypress Documentation](https://docs.cypress.io)
- [Best Practices](https://docs.cypress.io/guides/references/best-practices)
- [API Reference](https://docs.cypress.io/api/table-of-contents)
- [Cypress GitHub](https://github.com/cypress-io/cypress)

