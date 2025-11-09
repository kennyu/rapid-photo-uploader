# RapidPhotoUpload Backend

🚀 **High-Performance Photo Upload System** supporting **100 concurrent uploads** with robust error handling, automatic retry, and AI-powered image processing.

## 📚 Documentation

- **[Setup Guide](SETUP_GUIDE.md)** - Complete installation and configuration guide
- **[Architecture Overview](ARCHITECTURE.md)** - System design, patterns, and data flow
- **[API Reference](API_REFERENCE.md)** - Complete REST API documentation
- **[Testing Guide](TEST_CURL.md)** - cURL testing examples

---

## ✨ Key Features

### 🎯 Core Capabilities
- ✅ **Batch Upload**: Upload up to 100 photos concurrently
- ✅ **Direct S3 Upload**: Pre-signed URLs for client-to-S3 (no backend bottleneck)
- ✅ **Automatic Retry**: Exponential backoff retry logic (up to 3 attempts)
- ✅ **Image Processing**: Automatic compression & thumbnail generation
- ✅ **AI Tagging**: Pluggable design for AWS Rekognition/OpenAI Vision
- ✅ **Event-Driven**: S3 webhook integration for status updates

### 🏗️ Architecture
- ✅ **Domain-Driven Design (DDD)**: Clean separation of concerns
- ✅ **CQRS**: Command/Query separation for scalability
- ✅ **Vertical Slice Architecture**: Feature-focused organization
- ✅ **Async Processing**: Non-blocking operations with thread pools

### 🔐 Security
- ✅ **JWT Authentication**: Secure token-based auth
- ✅ **BCrypt Hashing**: Secure password storage
- ✅ **Spring Security 6.x**: Industry-standard security framework

### 💾 Tech Stack
- **Backend**: Spring Boot 3.5.7 (Java 21)
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Storage**: AWS S3 with pre-signed URLs
- **Image Processing**: Thumbnailator 0.4.20
- **Resilience**: Resilience4j for retry logic
- **Mapping**: MapStruct for DTO conversions

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** (LTS - Recommended)
- **PostgreSQL 15+**
- **AWS S3 Bucket** (with access credentials)
- **Gradle 8.5+** (included via wrapper: `./gradlew`)

### 1. Clone & Setup

```bash
cd backend
```

### 2. Configure Database

Create PostgreSQL database:
```bash
createdb rapidphoto
```

### 3. Set Environment Variables

Create `application-local.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/rapidphoto
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# AWS S3
aws.s3.bucket-name=YOUR_BUCKET_NAME
aws.s3.region=us-east-1

# JWT
jwt.secret=YOUR_SECRET_KEY_HERE_CHANGE_IN_PRODUCTION
jwt.expiration-ms=86400000

# AWS Credentials (via environment or AWS CLI profile)
# AWS_ACCESS_KEY_ID=your_access_key
# AWS_SECRET_ACCESS_KEY=your_secret_key
```

### 4. Run Database Migrations

Flyway will automatically run migrations on startup, creating:
- `users` table
- `photos` table
- `upload_jobs` table

### 5. Build & Run

**Windows (Easiest)**:
```bash
# Run directly
run.bat

# Or build first, then run
run.bat --build
```

**PowerShell**:
```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

# Build
.\gradlew.bat build

# Run
.\gradlew.bat bootRun
```

The server will start at **http://localhost:8080**

### 6. Verify Health

```bash
curl http://localhost:8080/api/v1/health
# Expected: {"status":"OK"}
```

---

## 📖 API Quick Reference

### Authentication

```bash
# Register
POST /api/v1/auth/register
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword"
}

# Login (returns JWT token)
POST /api/v1/auth/login
{
  "username": "johndoe",
  "password": "securePassword"
}
```

### Photo Upload

```bash
# Batch Upload (get pre-signed URLs)
POST /api/v1/photos/upload/batch
Authorization: Bearer <token>
{
  "userId": "USER_ID",
  "files": [
    {"filename": "photo1.jpg", "fileSize": 2048000, "contentType": "image/jpeg"},
    {"filename": "photo2.png", "fileSize": 1536000, "contentType": "image/png"}
  ]
}

# Mark Complete (after uploading to S3)
POST /api/v1/uploads/{uploadJobId}/complete
Authorization: Bearer <token>
```

### Query Photos

```bash
# Get all photos (paginated)
GET /api/v1/photos?userId=USER_ID&page=0&size=20
Authorization: Bearer <token>

# Get specific photo
GET /api/v1/photos/{photoId}
Authorization: Bearer <token>
```

**Full API documentation**: [API_REFERENCE.md](API_REFERENCE.md)

---

## 🗂️ Project Structure

```
backend/src/main/java/com/rapidphoto/uploader/
├── domain/              # Entities (User, Photo, UploadJob)
├── application/         # CQRS (Commands, Queries, Services)
├── infrastructure/      # Repositories, S3, Security, Schedulers
└── api/                 # REST Controllers, DTOs, Mappers
    ├── controller/      # Global controllers
    └── slices/          # Vertical slices (auth, upload, query)
```

**Detailed architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 🔧 Configuration Options

### Database

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rapidphoto
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
```

### AWS S3

```properties
aws.s3.bucket-name=${AWS_S3_BUCKET_NAME}
aws.s3.region=${AWS_REGION:us-east-1}
```

### JWT

```properties
jwt.secret=${JWT_SECRET:change-in-production}
jwt.expiration-ms=86400000  # 24 hours
```

### Image Processing

```properties
image.processing.enabled=true
image.processing.compression.quality=0.85
image.processing.thumbnail.size=300
image.tagging.enabled=false  # Enable for AI tagging
```

### Retry Logic

```properties
resilience4j.retry.instances.uploadRetry.max-attempts=3
resilience4j.retry.instances.uploadRetry.wait-duration=2s
resilience4j.retry.instances.uploadRetry.enable-exponential-backoff=true
```

---

## 🧪 Testing

### Run Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing with cURL

See [API_REFERENCE.md](API_REFERENCE.md#testing-with-curl) for examples.

---

## 📊 Database Schema

### Users
```sql
id (UUID) | username | email | password_hash | created_at | updated_at
```

### Photos
```sql
id (UUID) | filename | file_size | storage_key | user_id | status | 
content_type | tags[] | original_width | original_height | 
compressed_size | thumbnail_key | created_at | updated_at
```

### Upload Jobs
```sql
id (UUID) | photo_id | user_id | status | attempt_count | 
error_message | created_at | updated_at
```

---

## 🔄 Upload Workflow

```
1. Client → POST /api/v1/photos/upload/batch
           ↓
2. Backend generates pre-signed S3 URLs
           ↓
3. Client uploads directly to S3 (parallel)
           ↓
4. Client → POST /api/v1/uploads/{id}/complete
           ↓
5. Backend processes image (async):
   - Compress (85% quality)
   - Generate thumbnail (300px)
   - Tag with AI (optional)
           ↓
6. Photo status → COMPLETE
```

---

## 🛠️ Development

### Hot Reload

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Database Migrations

Create new migration:
```sql
-- backend/src/main/resources/db/migration/V4__Description.sql
CREATE TABLE ...
```

Migrations run automatically on startup.

### Adding New Features

1. **Create domain entity** in `domain/`
2. **Add repository** in `infrastructure/repository/`
3. **Create vertical slice** in `api/slices/`
   - Command/Query
   - Handler
   - Controller
4. **Add tests** in `src/test/`

---

## 🐛 Troubleshooting

### Database Connection Fails
```bash
# Verify PostgreSQL is running
psql -U postgres -c "SELECT version();"

# Check connection string in application.properties
```

### AWS S3 Errors
```bash
# Verify credentials
aws s3 ls s3://YOUR_BUCKET_NAME

# Check bucket policy allows pre-signed URLs
```

### JWT Token Invalid
```bash
# Ensure JWT secret is set (not default)
# Check token expiration (default 24h)
```

---

## 📈 Performance

- **Concurrent Uploads**: 100 simultaneous files
- **Batch Processing**: 20 threads
- **Image Processing**: 5-10 threads (configurable)
- **Retry Logic**: Exponential backoff (2s, 4s, 8s)
- **Database**: HikariCP connection pooling

---

## 🔮 Roadmap

- [ ] WebSocket for real-time upload progress
- [ ] Redis caching for hot data
- [ ] CloudFront CDN integration
- [ ] Elasticsearch for advanced photo search
- [ ] Multi-region S3 replication
- [ ] Prometheus metrics & Grafana dashboards

---

## 📄 License

MIT License - see LICENSE file for details

---

## 🙋 Support

- **API Documentation**: [API_REFERENCE.md](API_REFERENCE.md)
- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **Issues**: Create a GitHub issue

---

**Built with ❤️ using Spring Boot, PostgreSQL, and AWS S3**
