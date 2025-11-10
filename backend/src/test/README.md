# Backend Integration Tests

Comprehensive integration test suite for the Rapid Photo Uploader backend using JUnit 5, Spring Boot Test, and Testcontainers.

## Test Coverage

### 📦 `PhotoUploadIntegrationTest`
Tests the complete photo upload workflow:
- ✅ Full upload flow (initiate → complete → verify)
- ✅ Batch upload initiation (10+ concurrent uploads)
- ✅ Tag filtering functionality
- ✅ Authentication requirements
- ✅ Request validation
- ✅ Pre-signed URL generation

### 🖼️ `GalleryAndTagsIntegrationTest`
Tests gallery viewing and tag management:
- ✅ Photo retrieval with pagination
- ✅ Tag addition and removal
- ✅ Tag filtering by single tag
- ✅ Bulk tag replacement
- ✅ Photo sorting by date
- ✅ Pre-signed URL inclusion in responses

## Technology Stack

- **JUnit 5** - Test framework
- **Spring Boot Test** - Spring integration testing
- **Testcontainers** - PostgreSQL container for integration tests
- **TestRestTemplate** - HTTP client for API testing
- **AssertJ** - Fluent assertions

## Prerequisites

- **Java 21**
- **Docker** (for Testcontainers)
- **Gradle 8+**

## Running Tests

### Run All Tests
```bash
cd backend
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests PhotoUploadIntegrationTest
./gradlew test --tests GalleryAndTagsIntegrationTest
```

### Run with Detailed Output
```bash
./gradlew test --info
```

### Generate Test Report
```bash
./gradlew test
# Open: build/reports/tests/test/index.html
```

## Test Configuration

### Database
- Uses **Testcontainers** to spin up a PostgreSQL 16 container
- Container is reused across tests for performance
- Flyway migrations run automatically
- Database is cleaned before each test via `cleanup.sql`

### Authentication
- Each test creates a unique test user
- JWT tokens are generated for authenticated requests
- Tokens are included in all protected endpoint calls

### AWS S3
- S3 operations are **disabled** in tests (`image.processing.enabled=false`)
- Pre-signed URLs are still generated for testing
- No actual file uploads to S3 occur

## Test Structure

```
src/test/
├── java/
│   └── com/rapidphoto/uploader/
│       ├── IntegrationTestBase.java          # Base class with Testcontainers setup
│       ├── PhotoUploadIntegrationTest.java   # Upload flow tests
│       └── GalleryAndTagsIntegrationTest.java # Gallery & tag tests
└── resources/
    ├── cleanup.sql                            # Database cleanup script
    └── application-test.properties            # Test configuration
```

## Key Test Scenarios

### Authentication Flow
1. User registration
2. JWT token generation
3. Token validation on protected endpoints

### Upload Flow
1. **Initiate**: Request pre-signed URL from backend
2. **Upload**: (Simulated) Direct upload to S3
3. **Complete**: Notify backend of completion
4. **Verify**: Check photo status and metadata

### Tag Management
1. Add tags to photos
2. Remove tags from photos
3. Replace all tags at once
4. Filter photos by tag

### Pagination
- Default page size: 20
- Configurable via query params: `?page=0&size=10`
- Response includes: `content`, `page`, `size`, `totalElements`, `totalPages`

## Continuous Integration

Tests are designed to run in CI environments:
- **Docker required** for Testcontainers
- **No external dependencies** needed
- **Self-contained** database via containers
- **Fast startup** with container reuse

### GitHub Actions Example
```yaml
- name: Run Integration Tests
  run: ./gradlew test
  env:
    DOCKER_HOST: unix:///var/run/docker.sock
```

## Debugging Tests

### View Container Logs
Testcontainers automatically logs container output to console when tests fail.

### Increase Log Verbosity
Edit `src/test/resources/application-test.properties`:
```properties
logging.level.com.rapidphoto.uploader=TRACE
logging.level.org.testcontainers=DEBUG
```

### Keep Container Running After Test
Add to test class:
```java
postgres.withReuse(true);
```

## Adding New Tests

### 1. Create Test Class
```java
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class MyNewIntegrationTest extends IntegrationTestBase {
    // Your tests here
}
```

### 2. Use Helper Methods
- `registerAndLogin()` - Creates user and gets auth token
- `createAuthHeaders()` - Generates headers with Bearer token
- `baseUrl()` - Returns http://localhost:{randomPort}

### 3. Follow Naming Conventions
- Test methods: `shouldDoSomething()`
- Test classes: `*IntegrationTest.java`

## Performance

- **Container Reuse**: Postgres container is reused across tests
- **Parallel Execution**: Tests can run in parallel (configure in gradle)
- **Typical Runtime**: ~10-20 seconds for full suite

## Common Issues

**Issue**: Testcontainers can't connect to Docker
- **Solution**: Ensure Docker daemon is running
- **Windows**: Docker Desktop must be running
- **Linux**: Check `docker ps` works

**Issue**: Tests fail with "Address already in use"
- **Solution**: Spring uses random ports, but ensure no conflicting services

**Issue**: Database state persists between tests
- **Solution**: Verify `@Sql(scripts = "/cleanup.sql")` is present on test class

## Future Enhancements

- [ ] Mock S3 client for actual file upload simulation
- [ ] Performance/load testing for batch uploads
- [ ] Security testing (SQL injection, XSS, etc.)
- [ ] Test data factories/builders
- [ ] Integration with SonarQube for coverage reports

