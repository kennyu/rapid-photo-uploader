# Rapid Photo Uploader - Technical Architecture

> **Version:** 1.0  
> **Last Updated:** November 2025  
> **Author:** Technical Documentation

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Overview](#architecture-overview)
3. [Backend Architecture](#backend-architecture)
4. [Concurrency Strategy](#concurrency-strategy)
5. [Cloud Storage Integration](#cloud-storage-integration)
6. [Frontend Architectures](#frontend-architectures)
7. [Security](#security)
8. [Deployment Strategy](#deployment-strategy)
9. [Data Flow](#data-flow)
10. [Technology Stack](#technology-stack)

---

## System Overview

The Rapid Photo Uploader is a full-stack photo management system that enables users to upload, process, organize, and retrieve photos efficiently across web and mobile platforms. The system is designed to handle batch uploads (up to 100 photos concurrently) with real-time progress tracking, asynchronous image processing, and secure cloud storage.

### Key Features

- **Batch Photo Upload**: Upload up to 100 photos simultaneously with real-time progress tracking
- **Asynchronous Processing**: Non-blocking image compression and thumbnail generation
- **Tag Management**: Add, remove, and filter photos by tags
- **Secure Storage**: AWS S3 with pre-signed URLs for secure direct uploads
- **Cross-Platform**: Web (React/Vite) and Mobile (React Native/Expo) clients
- **RESTful API**: Spring Boot backend with JWT authentication

### System Context

```
┌─────────────────────────────────────────────────────────────────┐
│                         Internet Users                          │
│                    (Photographers, Teams)                       │
└──────────────┬────────────────────────────────┬─────────────────┘
               │                                │
        ┌──────▼──────┐                  ┌────▼─────┐
        │ Web Client  │                  │  Mobile  │
        │   (Vite)    │                  │  (Expo)  │
        └──────┬──────┘                  └────┬─────┘
               │                              │
               └──────────────┬───────────────┘
                              │ HTTPS/REST
                    ┌─────────▼──────────┐
                    │   Backend API      │
                    │  (Spring Boot)     │
                    │    on EC2          │
                    └─────────┬──────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
    ┌────▼─────┐      ┌──────▼──────┐     ┌──────▼──────┐
    │   RDS    │      │   AWS S3    │     │   ElastiCache│
    │(Postgres)│      │   Bucket    │     │    (Redis)  │
    └──────────┘      └─────────────┘     └─────────────┘
```

---

## Architecture Overview

### Architectural Style

The system follows a **hexagonal (ports and adapters) architecture** with **Domain-Driven Design (DDD)** principles and **CQRS** (Command Query Responsibility Segregation) pattern.

**Key Principles:**
- **Separation of Concerns**: Clear boundaries between domain, application, and infrastructure layers
- **Dependency Inversion**: Domain layer is independent of infrastructure details
- **CQRS**: Separate models for commands (writes) and queries (reads)
- **Event-Driven**: Asynchronous processing through events
- **Clean Architecture**: Business logic isolated from frameworks and external systems

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │     Auth     │    │    Upload    │    │    Query     │     │
│  │ Controllers  │    │  Controllers │    │  Controllers │     │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘     │
└─────────┼────────────────────┼────────────────────┼─────────────┘
          │                    │                    │
┌─────────┼────────────────────┼────────────────────┼─────────────┐
│         │         Application Layer (Use Cases)   │             │
│  ┌──────▼───────┐    ┌──────▼───────┐    ┌──────▼───────┐     │
│  │   Register   │    │   Upload     │    │     Query    │     │
│  │    User      │    │    Photo     │    │    Photos    │     │
│  │   Handler    │    │   Handler    │    │    Handler   │     │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘     │
└─────────┼────────────────────┼────────────────────┼─────────────┘
          │                    │                    │
┌─────────┼────────────────────┼────────────────────┼─────────────┐
│         │            Domain Layer                 │             │
│  ┌──────▼───────┐    ┌──────▼───────┐    ┌──────▼───────┐     │
│  │     User     │    │    Photo     │    │  Upload Job  │     │
│  │   Entity     │    │   Entity     │    │   Aggregate  │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
└─────────────────────────────────────────────────────────────────┘
          │                    │                    │
┌─────────┼────────────────────┼────────────────────┼─────────────┐
│         │         Infrastructure Layer            │             │
│  ┌──────▼───────┐    ┌──────▼───────┐    ┌──────▼───────┐     │
│  │     JPA      │    │      S3      │    │   Async      │     │
│  │  Repository  │    │   Storage    │    │  Processor   │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Backend Architecture

### Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java Version**: 21 (LTS)
- **Database**: PostgreSQL 16 (AWS RDS)
- **Cloud Storage**: AWS S3
- **Cache**: Redis (ElastiCache) - optional
- **Build Tool**: Gradle 8.x
- **Security**: Spring Security + JWT

### Package Structure (DDD Layers)

```
src/main/java/com/rapidphoto/uploader/
│
├── api/                          # Presentation Layer
│   ├── controller/              # REST Controllers
│   ├── dto/                     # Data Transfer Objects
│   ├── mapper/                  # DTO ↔ Domain Mappers
│   └── slices/                  # CQRS Command/Query Handlers
│       ├── uploadphoto/         # Upload Photo Command
│       ├── queryphotos/         # Query Photos
│       └── auth/                # Authentication
│
├── application/                  # Application Layer (Use Cases)
│   └── service/                 # Application Services
│       ├── ImageProcessingService.java
│       ├── PhotoUploadService.java
│       └── UserService.java
│
├── domain/                       # Domain Layer (Business Logic)
│   ├── model/                   # Domain Entities & Aggregates
│   │   ├── Photo.java          # Photo Entity
│   │   ├── User.java           # User Entity
│   │   └── UploadJob.java      # Upload Job Aggregate
│   ├── repository/              # Repository Interfaces (ports)
│   └── service/                 # Domain Services
│
└── infrastructure/               # Infrastructure Layer (Adapters)
    ├── persistence/             # JPA Implementations
    ├── storage/                 # S3 Storage Implementation
    ├── security/                # Security Configuration
    └── config/                  # Spring Configuration

```

### Layer Responsibilities

#### 1. **API Layer (Presentation)**
- **Purpose**: HTTP endpoint handling, request/response mapping
- **Responsibilities**:
  - Validate HTTP requests
  - Map DTOs to/from domain models
  - Return appropriate HTTP status codes
  - Handle authentication/authorization
- **Example**: `PhotoUploadController.java`

#### 2. **Application Layer (Use Cases)**
- **Purpose**: Orchestrate business operations
- **Responsibilities**:
  - Implement use cases (upload photo, query photos, etc.)
  - Coordinate between domain and infrastructure
  - Handle transactions
  - Trigger async processing
- **Example**: `PhotoUploadService.java`, `ImageProcessingService.java`

#### 3. **Domain Layer (Business Logic)**
- **Purpose**: Core business rules and entities
- **Responsibilities**:
  - Define domain entities and value objects
  - Enforce business invariants
  - Contain domain logic (validations, calculations)
  - Define repository interfaces (ports)
- **Example**: `Photo.java`, `User.java`, `UploadJob.java`
- **Note**: Domain layer has NO dependencies on other layers

#### 4. **Infrastructure Layer (Technical Details)**
- **Purpose**: Technical implementations and external integrations
- **Responsibilities**:
  - Implement repository interfaces (JPA)
  - Integrate with external systems (S3, Redis)
  - Configure frameworks (Spring, Security)
  - Handle persistence, caching, messaging
- **Example**: `S3StorageService.java`, `PhotoRepositoryImpl.java`

### CQRS Pattern Implementation

The system separates **Commands** (writes) from **Queries** (reads) for better scalability and clarity.

#### Commands (Write Operations)
Located in `api/slices/uploadphoto/`:
- **InitiateUploadCommand**: Start photo upload
- **CompleteUploadCommand**: Mark upload as complete
- **AddTagCommand**: Add tag to photo

**Characteristics:**
- Modify system state
- Return minimal data (IDs, status)
- Trigger side effects (async processing)
- Use optimistic locking for concurrency

#### Queries (Read Operations)
Located in `api/slices/queryphotos/`:
- **GetPhotosQuery**: Retrieve photos with filtering/pagination
- **GetPhotoByIdQuery**: Get single photo details

**Characteristics:**
- Read-only operations
- Return rich data (DTOs with URLs)
- Optimized for read performance
- Can use caching
- Generate pre-signed URLs on-the-fly

---

## Concurrency Strategy

### Overview

The system handles concurrent photo uploads and processing using a combination of asynchronous processing, thread pools, and database optimistic locking.

### Key Components

#### 1. **Asynchronous Processing with @Async**

**Configuration**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // Min threads
        executor.setMaxPoolSize(20);          // Max threads
        executor.setQueueCapacity(100);       // Queue size
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**Usage in ImageProcessingService**:
```java
@Service
public class ImageProcessingService {
    
    @Async("taskExecutor")
    @Transactional
    public void processImageAsync(UUID photoId) {
        // Non-blocking image processing
        // Runs in separate thread pool
        // Does not block upload response
    }
}
```

**Benefits**:
- Upload initiation returns immediately
- Image processing happens in background
- Thread pool prevents resource exhaustion
- User gets instant feedback

#### 2. **Thread Pool Configuration**

**Core Parameters**:
- **Core Pool Size**: 5 threads (always active)
- **Max Pool Size**: 20 threads (peak capacity)
- **Queue Capacity**: 100 tasks (overflow buffer)
- **Keep-Alive**: 60 seconds (idle thread timeout)

**Scaling Behavior**:
1. Tasks 1-5: Use core threads
2. Tasks 6-105: Queue tasks (core threads busy)
3. Tasks 106+: Spawn additional threads (up to 20)
4. Beyond capacity: Reject with `RejectedExecutionException`

#### 3. **Database Concurrency Control**

**Optimistic Locking**:
```java
@Entity
public class Photo {
    @Version
    private Long version;  // JPA version field
    
    // Prevents lost updates in concurrent modifications
    // Throws OptimisticLockException on conflict
}
```

**Pessimistic Locking** (when needed):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Photo findByIdWithLock(UUID id);
```

#### 4. **Connection Pool (HikariCP)**

**Configuration** (`application.properties`):
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Pool Management**:
- 20 max connections to PostgreSQL
- Prevents database connection exhaustion
- Auto-reconnect on connection failure
- Connection leak detection

### Concurrency Scenarios

#### Scenario 1: Batch Upload (100 Photos)

```
Client → Backend: POST /photos/upload/initiate (×100)
    ↓
Backend: 
    1. Validate request (synchronous)
    2. Generate pre-signed URL (synchronous) 
    3. Save Photo entity (synchronous)
    4. Return response immediately ✓
    ↓
Client: Upload to S3 directly (parallel)
    ↓
Client → Backend: POST /uploads/{id}/complete (×100)
    ↓
Backend:
    1. Mark upload complete (synchronous)
    2. Trigger async processing (non-blocking) ✓
    3. Return 200 OK immediately
    ↓
Background Thread Pool:
    - Process images concurrently (up to 20 threads)
    - Compress, generate thumbnails
    - Upload processed versions to S3
    - Update status to COMPLETE
```

**Timeline**:
- Upload initiation: ~50-100ms per photo
- S3 upload: Parallel, client-side
- Upload completion: ~30ms per photo
- Background processing: 2-5 seconds per photo (non-blocking)

**Concurrency Control**:
- HTTP thread pool handles API requests
- Async thread pool handles image processing
- Database connection pool manages DB access
- No blocking between layers

#### Scenario 2: Concurrent Tag Updates

```
User A: Add tag "vacation" to Photo 1
User B: Add tag "beach" to Photo 1

Photo(id=1, version=1, tags=[])

User A transaction:
    1. Load Photo (version=1)
    2. Add "vacation"
    3. Save (version → 2) ✓

User B transaction:
    1. Load Photo (version=1)
    2. Add "beach"  
    3. Save (version=2 expected, but now 3)
    4. OptimisticLockException thrown!
    5. Retry with latest version
    6. Save (version → 3) ✓

Final: Photo(id=1, version=3, tags=["vacation", "beach"])
```

### Scalability Considerations

#### Current Capacity (Single EC2 Instance)
- **Concurrent Uploads**: ~100 simultaneous initiations
- **Processing Throughput**: 20 images processed concurrently
- **Database Connections**: 20 concurrent queries
- **Bottleneck**: Image processing (CPU-bound)

#### Horizontal Scaling Path
1. **Load Balancer**: Distribute traffic across multiple EC2 instances
2. **Shared Database**: RDS supports multiple connections
3. **Shared Storage**: S3 is already distributed
4. **Session Storage**: Use Redis for shared sessions
5. **Message Queue**: SQS for processing queue (future)

#### Vertical Scaling
- Increase EC2 instance size (more CPU for image processing)
- Increase RDS instance size (more connections)
- Tune thread pool sizes based on CPU cores

---

## Cloud Storage Integration

### AWS S3 Architecture

#### Bucket Structure
```
rapidphoto-uploads-{account-id}/
├── {userId}/
│   └── {year}/
│       └── {month}/
│           └── {day}/
│               ├── {photoId}-{originalName}          # Original
│               ├── {photoId}-compressed.jpg          # Compressed
│               └── {photoId}-thumbnail.jpg           # Thumbnail
```

**Example**:
```
rapidphoto-uploads-297721440242/
└── 891bf7c8-4e51-43fa-9ca6-caab5f49fd34/
    └── 2025/
        └── 11/
            └── 09/
                ├── abc123-photo.jpg
                ├── abc123-compressed.jpg
                └── abc123-thumbnail.jpg
```

### Pre-Signed URL Flow

#### Upload Flow (2-Step Process)

**Step 1: Initiate Upload**
```
POST /api/v1/photos/upload/initiate
Request:
{
  "filename": "vacation.jpg",
  "fileSize": 2048000,
  "contentType": "image/jpeg"
}

Response:
{
  "photoId": "abc-123",
  "uploadJobId": "xyz-789",
  "preSignedUrl": "https://s3.amazonaws.com/...?signature=...",
  "expiresInSeconds": 3600
}
```

**Backend Processing**:
```java
@Service
public class PhotoUploadService {
    
    public InitiateUploadResult initiateUpload(InitiateUploadRequest request) {
        // 1. Create Photo entity (status: UPLOADING)
        Photo photo = createPhoto(request);
        photoRepository.save(photo);
        
        // 2. Generate S3 key
        String s3Key = generateS3Key(userId, photoId, filename);
        
        // 3. Generate pre-signed URL (1 hour expiry)
        URL preSignedUrl = s3Client.generatePresignedUrl(
            bucketName, 
            s3Key, 
            Date.from(Instant.now().plus(1, HOURS)),
            HttpMethod.PUT
        );
        
        // 4. Return URL to client
        return new InitiateUploadResult(photoId, uploadJobId, preSignedUrl);
    }
}
```

**Step 2: Client Upload to S3**
```javascript
// Client-side (direct to S3)
const response = await fetch(preSignedUrl, {
  method: 'PUT',
  body: fileBlob,
  headers: {
    'Content-Type': 'image/jpeg'
  }
});
```

**Step 3: Mark Upload Complete**
```
POST /api/v1/uploads/{uploadJobId}/complete

Response: 200 OK
```

**Backend Processing**:
```java
@PostMapping("/{uploadJobId}/complete")
public ResponseEntity<Void> completeUpload(@PathVariable UUID uploadJobId) {
    // 1. Validate upload job exists
    UploadJob job = uploadJobRepository.findById(uploadJobId);
    
    // 2. Update photo status
    Photo photo = photoRepository.findById(job.getPhotoId());
    photo.setStatus(PhotoStatus.PROCESSING);
    photoRepository.save(photo);
    
    // 3. Trigger async image processing (non-blocking)
    imageProcessingService.processImageAsync(photo.getId());
    
    return ResponseEntity.ok().build();
}
```

#### Download Flow (On-Demand)

**Query Photos** (generates fresh URLs):
```
GET /api/v1/photos

Response:
{
  "content": [
    {
      "id": "abc-123",
      "filename": "vacation.jpg",
      "thumbnailUrl": "https://s3.amazonaws.com/...thumbnail.jpg?signature=...",
      "downloadUrl": "https://s3.amazonaws.com/...photo.jpg?signature=...",
      "status": "COMPLETE",
      "tags": ["vacation", "beach"]
    }
  ],
  "page": 0,
  "totalElements": 42
}
```

**URL Generation**:
```java
@GetMapping
public ResponseEntity<Map<String, Object>> getPhotos() {
    Page<Photo> photoPage = photoRepository.findAll(pageable);
    
    // Enrich each photo with pre-signed URLs
    Page<PhotoDto> dtoPage = photoPage.map(photo -> {
        PhotoDto dto = mapper.toDto(photo);
        
        // Generate thumbnail URL (1 hour expiry)
        URL thumbnailUrl = s3Service.generatePresignedDownloadUrl(
            photo.getThumbnailKey(), 
            Duration.ofHours(1)
        );
        dto.setThumbnailUrl(thumbnailUrl.toString());
        
        // Generate download URL (1 hour expiry)
        URL downloadUrl = s3Service.generatePresignedDownloadUrl(
            photo.getStorageKey(),
            Duration.ofHours(1)
        );
        dto.setDownloadUrl(downloadUrl.toString());
        
        return dto;
    });
    
    return ResponseEntity.ok(paginatedResponse);
}
```

### S3 Security Configuration

#### CORS Configuration
```json
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedOrigins": [
        "http://localhost:3000",
        "http://localhost:8080",
        "http://localhost:8081",
        "https://your-vercel-domain.vercel.app"
      ],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
}
```

#### Bucket Policy
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT:role/RapidPhotoBackendRole"
      },
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::rapidphoto-uploads-ACCOUNT/*",
        "arn:aws:s3:::rapidphoto-uploads-ACCOUNT"
      ]
    }
  ]
}
```

#### Lifecycle Policy
```json
{
  "Rules": [
    {
      "Id": "CleanupIncompleteUploads",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 1
      }
    },
    {
      "Id": "TransitionToIA",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "STANDARD_IA"
        }
      ]
    }
  ]
}
```

### Storage Service Implementation

```java
@Service
public class S3StorageService implements StorageService {
    
    private final S3Client s3Client;
    private final String bucketName;
    
    @Override
    public URL generatePresignedUploadUrl(String key, Duration expiry) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .putObjectRequest(req -> req
                .bucket(bucketName)
                .key(key)
            )
            .build();
            
        PresignedPutObjectRequest presignedRequest = 
            s3Presigner.presignPutObject(presignRequest);
            
        return presignedRequest.url();
    }
    
    @Override
    public URL generatePresignedDownloadUrl(String key, Duration expiry) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .getObjectRequest(req -> req
                .bucket(bucketName)
                .key(key)
            )
            .build();
            
        PresignedGetObjectRequest presignedRequest = 
            s3Presigner.presignGetObject(presignRequest);
            
        return presignedRequest.url();
    }
    
    @Override
    public void uploadFile(String key, InputStream data, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();
            
        s3Client.putObject(putRequest, RequestBody.fromInputStream(data, data.available()));
    }
    
    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
```

### Benefits of Pre-Signed URLs

1. **Scalability**: Clients upload directly to S3, bypassing backend
2. **Security**: URLs expire after 1 hour, no permanent access
3. **Performance**: No backend bandwidth consumed for uploads
4. **Cost**: Reduced EC2 data transfer costs
5. **Reliability**: S3's high availability (99.99%)

### Security Considerations

1. **URL Expiry**: All URLs expire (1 hour default)
2. **IAM Roles**: Backend uses EC2 instance role, no hardcoded credentials
3. **Bucket Encryption**: Server-side encryption enabled (SSE-S3)
4. **Private Bucket**: No public access, all access via pre-signed URLs
5. **User Isolation**: Each user's files stored in separate prefix
6. **CORS**: Restricted to known origins only

---

## Frontend Architectures

### Web Client (React + Vite)

**Technology Stack**:
- **Framework**: React 18.3
- **Build Tool**: Vite 6.0
- **State Management**: React Query (TanStack Query)
- **Styling**: CSS Modules
- **HTTP Client**: Axios
- **Deployment**: Vercel

**Architecture**:
```
src/
├── api/
│   └── client.ts              # Axios configuration + interceptors
├── features/
│   ├── auth/                  # Authentication feature
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── authService.ts
│   ├── upload/                # Photo upload feature
│   │   ├── UploadPage.tsx
│   │   └── UploadPage.css
│   └── gallery/               # Gallery feature
│       ├── GalleryPage.tsx
│       ├── TagEditModal.tsx
│       └── usePhotos.ts       # React Query hook
├── common/
│   └── Button.tsx             # Shared components
└── types/
    └── api.ts                 # TypeScript interfaces
```

**Key Features**:
- Feature-based folder structure
- React Query for server state management
- Axios interceptors for auth token injection
- Pre-signed URL upload directly to S3
- Optimistic UI updates

**Deployment**:
- Hosted on Vercel
- Automatic builds from Git
- Environment variables configured in Vercel dashboard
- CDN distribution for fast global access

### Mobile Client (React Native + Expo)

**Technology Stack**:
- **Framework**: React Native 0.81.5
- **Platform**: Expo SDK 54
- **Navigation**: React Navigation v7
- **HTTP Client**: Axios
- **State**: React Context + Hooks
- **Deployment**: Expo Go (dev), Web build (production)

**Architecture**:
```
src/
├── api/
│   └── client.ts              # Axios with platform-specific storage
├── navigation/
│   ├── AppNavigator.tsx       # Main navigation structure
│   └── AuthContext.tsx        # Global auth state
├── features/
│   ├── auth/
│   │   ├── LoginScreen.tsx
│   │   └── RegisterScreen.tsx
│   ├── upload/
│   │   └── UploadScreen.tsx
│   └── gallery/
│       └── GalleryScreen.tsx
├── components/
│   └── AppHeader.tsx          # Top navigation bar
└── types/
    ├── api.ts
    └── navigation.ts
```

**Platform-Specific Features**:
- **Storage**: SecureStore (native), localStorage (web)
- **File Picker**: expo-image-picker
- **Camera**: expo-camera
- **Downloads**: expo-file-system + expo-media-library

**Deployment**:
- **Development**: Expo Go app (iOS/Android)
- **Web**: Expo web build hosted on Vercel
- **Native**: SDK builds (future App Store/Play Store)

---

## Security

### Authentication & Authorization

**JWT Token Flow**:
```
1. User Login → Backend validates credentials
2. Backend generates JWT token (24h expiry)
3. Token returned to client
4. Client stores token (SecureStore/localStorage)
5. All API requests include: Authorization: Bearer {token}
6. Backend validates token on each request
7. Invalid/expired token → 401 Unauthorized
```

**JWT Payload**:
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "iat": 1699564800,
  "exp": 1699651200,
  "authorities": ["ROLE_USER"]
}
```

**Security Configuration**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disabled for stateless API
            .sessionManagement(session -> session
                .sessionCreationPolicy(STATELESS))  // No server sessions
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()  // Public
                .anyRequest().authenticated())  // Protected
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### Password Security

- **Algorithm**: BCrypt with strength 12
- **Minimum Length**: 8 characters
- **Validation**: Client-side + server-side
- **Never Stored**: Plain passwords never logged or stored

### CORS Configuration

**Backend** (`application.properties`):
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:8081,https://your-vercel-app.vercel.app
cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
cors.allowed-headers=Authorization,Content-Type
cors.exposed-headers=Content-Disposition
cors.max-age=3600
```

---

## Deployment Strategy

### Backend Deployment (EC2)

**Instance Configuration**:
- **Instance Type**: t3.medium (2 vCPU, 4 GB RAM)
- **OS**: Amazon Linux 2023
- **Java**: OpenJDK 21
- **Application**: Spring Boot JAR
- **Process Manager**: systemd

**Deployment Steps**:
1. Build JAR: `./gradlew build`
2. Upload to EC2: `scp target/app.jar ec2-user@{ip}:/opt/app/`
3. Restart service: `sudo systemctl restart rapidphoto`
4. Verify: Check logs and health endpoint

**Environment Variables** (systemd):
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://rds-endpoint:5432/rapidphoto
SPRING_DATASOURCE_USERNAME=${db_username}
SPRING_DATASOURCE_PASSWORD=${db_password}
AWS_REGION=us-east-1
AWS_S3_BUCKET_NAME=rapidphoto-uploads-${account_id}
JWT_SECRET=${secret_key}
```

**Database** (RDS PostgreSQL 16):
- **Instance**: db.t4g.micro
- **Storage**: 20 GB SSD
- **Multi-AZ**: No (can enable for HA)
- **Backups**: Automated daily
- **Security**: VPC security groups, no public access

### Web Frontend Deployment (Vercel)

**Build Configuration**:
```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "installCommand": "npm install",
  "framework": "vite"
}
```

**Environment Variables** (Vercel Dashboard):
```
VITE_API_URL=https://api.rapidphoto.com/api/v1
```

**Deployment**:
- Automatic deploys from `main` branch
- Preview deployments for PRs
- Edge network (CDN) distribution
- HTTPS by default

### Mobile Deployment

**Expo Go** (Development):
- Install Expo Go app on device
- Run: `npm start`
- Scan QR code to load app

**Web Build**:
- Build: `npx expo export:web`
- Deploy to Vercel
- Access via browser on any device

**Native Builds** (Future):
- EAS Build for iOS/Android
- Submit to App Store/Play Store

---

## Data Flow

### Upload Flow Diagram

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│ Client  │                    │ Backend │                    │   S3    │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │ 1. POST /upload/initiate     │                              │
     │─────────────────────────────>│                              │
     │                              │                              │
     │                              │ 2. Save Photo (UPLOADING)    │
     │                              │ 3. Generate pre-signed URL   │
     │                              │                              │
     │ 4. Return URL + photoId      │                              │
     │<─────────────────────────────│                              │
     │                              │                              │
     │ 5. PUT {fileData}                                           │
     │────────────────────────────────────────────────────────────>│
     │                                                              │
     │ 6. 200 OK                                                    │
     │<────────────────────────────────────────────────────────────│
     │                              │                              │
     │ 7. POST /uploads/{id}/complete │                            │
     │─────────────────────────────>│                              │
     │                              │                              │
     │                              │ 8. Update status (PROCESSING)│
     │                              │ 9. Trigger async processing  │
     │                              │                              │
     │ 10. 200 OK                   │                              │
     │<─────────────────────────────│                              │
     │                              │                              │
     │                              │                              │
     │                     [Background Thread]                     │
     │                              │                              │
     │                              │ 11. Download original        │
     │                              │<─────────────────────────────│
     │                              │                              │
     │                              │ 12. Compress image           │
     │                              │ 13. Generate thumbnail       │
     │                              │                              │
     │                              │ 14. Upload compressed        │
     │                              │─────────────────────────────>│
     │                              │                              │
     │                              │ 15. Upload thumbnail         │
     │                              │─────────────────────────────>│
     │                              │                              │
     │                              │ 16. Update status (COMPLETE) │
     │                              │                              │
```

### Query Flow Diagram

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│ Client  │                    │ Backend │                    │   RDS   │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │ 1. GET /photos?tag=vacation  │                              │
     │─────────────────────────────>│                              │
     │                              │                              │
     │                              │ 2. Query photos by tag       │
     │                              │─────────────────────────────>│
     │                              │                              │
     │                              │ 3. Return Photo entities     │
     │                              │<─────────────────────────────│
     │                              │                              │
     │                              │ 4. Generate pre-signed URLs  │
     │                              │ 5. Map to DTOs               │
     │                              │                              │
     │ 6. Return PhotoDTOs          │                              │
     │<─────────────────────────────│                              │
     │                              │                              │
     │ 7. Render thumbnails         │                              │
     │                              │                              │
```

---

## Technology Stack

### Backend
| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | Spring Boot | 3.5.7 | Application framework |
| Language | Java | 21 | Programming language |
| Database | PostgreSQL | 16 | Relational database |
| ORM | JPA/Hibernate | 6.x | Object-relational mapping |
| Build | Gradle | 8.x | Build automation |
| Cloud Storage | AWS S3 | - | Object storage |
| Security | Spring Security | 6.x | Authentication/authorization |
| JWT | jjwt | 0.12.3 | Token generation |
| Image Processing | Thumbnailator | 0.4.20 | Image manipulation |
| Migration | Flyway | 9.x | Database migrations |

### Web Frontend
| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | React | 18.3 | UI framework |
| Build Tool | Vite | 6.0 | Build tool & dev server |
| State | React Query | 5.x | Server state management |
| HTTP Client | Axios | 1.x | API communication |
| Language | TypeScript | 5.x | Type-safe JavaScript |
| Hosting | Vercel | - | Deployment platform |

### Mobile Client
| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | React Native | 0.81.5 | Mobile framework |
| Platform | Expo | 54 | Development platform |
| Navigation | React Navigation | 7.x | Navigation library |
| HTTP Client | Axios | 1.x | API communication |
| Storage | expo-secure-store | 15.x | Secure token storage |
| File Picker | expo-image-picker | 17.x | Photo selection |
| Language | TypeScript | 5.x | Type-safe JavaScript |

### Infrastructure
| Component | Technology | Purpose |
|-----------|-----------|---------|
| Compute | AWS EC2 | Backend hosting |
| Database | AWS RDS | PostgreSQL hosting |
| Storage | AWS S3 | Object storage |
| DNS | Route 53 / Vercel | Domain management |
| CDN | Vercel Edge | Content delivery |

---

## Conclusion

The Rapid Photo Uploader demonstrates a well-architected, scalable photo management system with:

- ✅ **Clean Architecture**: Clear separation of concerns across layers
- ✅ **Async Processing**: Non-blocking image operations
- ✅ **Secure Storage**: Pre-signed URLs for direct S3 access
- ✅ **Cross-Platform**: Web and mobile clients
- ✅ **CQRS Pattern**: Optimized for reads and writes
- ✅ **Concurrency**: Thread pools and optimistic locking
- ✅ **Modern Stack**: Spring Boot 3, React 18, React Native/Expo
- ✅ **Cloud-Native**: AWS S3, RDS, EC2

The architecture is designed for future growth, with clear paths for horizontal scaling, caching, and message queue integration.

---

**Next**: [Cloud Storage Integration](./CLOUD_STORAGE.md) | [Concurrency Guide](./CONCURRENCY.md) | [API Reference](./API_REFERENCE.md)
