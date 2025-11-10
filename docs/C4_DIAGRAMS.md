# C4 Architecture Diagrams

This document contains C4 model diagrams for the Rapid Photo Uploader system, from high-level context down to detailed component views.

## Table of Contents
1. [Level 1: System Context](#level-1-system-context-diagram)
2. [Level 2: Container](#level-2-container-diagram)
3. [Level 3: Component (Backend)](#level-3-component-diagram---backend)
4. [Supplementary: Upload Sequence](#supplementary-upload-sequence-diagram)

---

## Level 1: System Context Diagram

Shows the system in its environment and its relationships with users and external systems.

```mermaid
C4Context
    title System Context Diagram - Rapid Photo Uploader

    Person(photographer, "Photographer", "Person who uploads and manages photos")
    Person(team_member, "Team Member", "Person who views shared photos")
    
    System(rapidphoto, "Rapid Photo Uploader", "Allows users to upload, process, organize and retrieve photos")
    
    System_Ext(aws_s3, "AWS S3", "Cloud object storage for photos")
    System_Ext(aws_rds, "AWS RDS", "Managed PostgreSQL database")
    System_Ext(email, "Email System", "Sends notifications (future)")
    
    Rel(photographer, rapidphoto, "Uploads photos, manages tags", "HTTPS")
    Rel(team_member, rapidphoto, "Views photos, downloads", "HTTPS")
    
    Rel(rapidphoto, aws_s3, "Stores/retrieves photo files", "AWS SDK")
    Rel(rapidphoto, aws_rds, "Reads/writes metadata", "JDBC/PostgreSQL")
    Rel(rapidphoto, email, "Sends notifications", "SMTP")
```

### Key Relationships

| From | To | Description | Protocol |
|------|-----|-------------|----------|
| Photographer | Rapid Photo System | Uploads photos, manages tags, organizes | HTTPS/REST |
| Team Member | Rapid Photo System | Views photos, downloads, filters | HTTPS/REST |
| Rapid Photo System | AWS S3 | Stores original/processed photos | AWS SDK v2 |
| Rapid Photo System | AWS RDS | Stores photo metadata, users, tags | PostgreSQL JDBC |

---

## Level 2: Container Diagram

Shows the high-level technology choices and how containers communicate.

```mermaid
C4Container
    title Container Diagram - Rapid Photo Uploader
    
    Person(user, "User", "Web or mobile user")
    
    Container_Boundary(client, "Client Applications") {
        Container(web_app, "Web Application", "React + Vite", "Provides photo upload/management via browser")
        Container(mobile_app, "Mobile Application", "React Native + Expo", "Provides photo upload/management on mobile")
    }
    
    Container_Boundary(backend, "Backend") {
        Container(api, "API Application", "Spring Boot 3", "Provides REST API, handles business logic")
    }
    
    ContainerDb(database, "Database", "PostgreSQL 16", "Stores user data, photo metadata, tags")
    Container_Ext(s3, "Object Storage", "AWS S3", "Stores photo files (original, compressed, thumbnail)")
    
    Rel(user, web_app, "Uses", "HTTPS")
    Rel(user, mobile_app, "Uses", "HTTPS")
    
    Rel(web_app, api, "Makes API calls", "HTTPS/REST, JSON")
    Rel(mobile_app, api, "Makes API calls", "HTTPS/REST, JSON")
    
    Rel(web_app, s3, "Uploads photos directly", "HTTPS/Pre-signed URL")
    Rel(mobile_app, s3, "Uploads photos directly", "HTTPS/Pre-signed URL")
    
    Rel(api, database, "Reads/writes", "TCP/PostgreSQL Protocol")
    Rel(api, s3, "Uploads processed images, generates URLs", "AWS SDK")
```

### Container Details

#### Web Application (React + Vite)
- **Technology**: React 18.3, TypeScript, Vite 6.0
- **Purpose**: Browser-based photo management
- **Key Features**:
  - Batch photo upload (up to 100)
  - Real-time upload progress
  - Photo gallery with filtering
  - Tag management
- **Deployment**: Vercel (CDN + Edge Functions)
- **State Management**: React Query (TanStack Query)

#### Mobile Application (React Native + Expo)
- **Technology**: React Native 0.81.5, Expo SDK 54, TypeScript
- **Purpose**: Native mobile photo management (iOS/Android/Web)
- **Key Features**:
  - Camera integration (expo-camera)
  - Photo picker (expo-image-picker)
  - Secure token storage (expo-secure-store)
  - Download to device (expo-media-library)
- **Deployment**: Expo Go (dev), Web build (Vercel), EAS Build (future native)

#### API Application (Spring Boot 3)
- **Technology**: Spring Boot 3.5.7, Java 21
- **Purpose**: Business logic, orchestration, security
- **Key Features**:
  - JWT authentication
  - Pre-signed URL generation
  - Asynchronous image processing
  - Tag management
  - CQRS pattern implementation
- **Deployment**: AWS EC2 (Amazon Linux 2023)
- **Port**: 8080

#### Database (PostgreSQL 16)
- **Technology**: PostgreSQL 16 on AWS RDS
- **Purpose**: Store structured data (users, photos, tags)
- **Schema**:
  - `users` - User accounts
  - `photos` - Photo metadata
  - `photo_tags` - Many-to-many relationship
- **Deployment**: AWS RDS (db.t4g.micro)
- **Backups**: Automated daily snapshots

#### Object Storage (AWS S3)
- **Technology**: AWS S3
- **Purpose**: Store binary photo files
- **Bucket Structure**: `{userId}/{year}/{month}/{day}/{photoId}-{filename}`
- **File Types**:
  - Original uploads
  - Compressed versions (85% quality)
  - Thumbnails (300px max dimension)
- **Access**: Pre-signed URLs only (no public access)
- **Lifecycle**: Transition to Standard-IA after 90 days

---

## Level 3: Component Diagram - Backend

Shows the internal structure of the API application, organized by DDD layers.

```mermaid
C4Component
    title Component Diagram - Backend API (Spring Boot)
    
    Container_Boundary(api, "API Application") {
        Component(auth_controller, "Auth Controller", "REST Controller", "Handles login, registration")
        Component(upload_controller, "Upload Controller", "REST Controller", "Initiates uploads, marks complete")
        Component(photo_query_controller, "Photo Query Controller", "REST Controller", "Retrieves photos with filters")
        Component(tag_controller, "Tag Controller", "REST Controller", "Manages photo tags")
        
        Boundary(application, "Application Layer") {
            Component(upload_service, "Photo Upload Service", "Service", "Orchestrates upload workflow")
            Component(processing_service, "Image Processing Service", "Async Service", "Compresses, generates thumbnails")
            Component(user_service, "User Service", "Service", "Manages user accounts")
        }
        
        Boundary(domain, "Domain Layer") {
            Component(photo_entity, "Photo Entity", "Domain Model", "Core photo business logic")
            Component(user_entity, "User Entity", "Domain Model", "Core user business logic")
            Component(upload_job, "Upload Job Aggregate", "Domain Model", "Upload tracking")
            Component(photo_repo_interface, "Photo Repository", "Port", "Repository interface")
        }
        
        Boundary(infrastructure, "Infrastructure Layer") {
            Component(photo_repo_impl, "JPA Photo Repository", "Adapter", "PostgreSQL persistence")
            Component(s3_service, "S3 Storage Service", "Adapter", "AWS S3 integration")
            Component(jwt_filter, "JWT Authentication Filter", "Security", "Validates tokens")
            Component(async_config, "Async Configuration", "Config", "Thread pool setup")
        }
    }
    
    ContainerDb(database, "Database", "PostgreSQL", "Persistent storage")
    Container_Ext(s3, "AWS S3", "Object Storage", "Photo file storage")
    
    Rel(auth_controller, user_service, "Uses")
    Rel(upload_controller, upload_service, "Uses")
    Rel(photo_query_controller, photo_repo_interface, "Uses")
    
    Rel(upload_service, photo_entity, "Creates, updates")
    Rel(upload_service, s3_service, "Generates pre-signed URLs")
    Rel(processing_service, s3_service, "Downloads, uploads images")
    
    Rel(photo_repo_impl, database, "Queries, persists", "JDBC")
    Rel(s3_service, s3, "API calls", "AWS SDK")
    
    Rel(jwt_filter, user_service, "Validates users")
```

### Component Responsibilities

#### API Layer (Controllers)

**AuthController**
```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    POST /register - Create new user account
    POST /login    - Authenticate and return JWT
}
```

**UploadPhotoController**
```java
@RestController
@RequestMapping("/api/v1/photos")
public class UploadPhotoController {
    POST /upload/initiate              - Start upload, get pre-signed URL
    POST /uploads/{uploadJobId}/complete - Mark upload complete
}
```

**PhotoQueryController**
```java
@RestController
@RequestMapping("/api/v1/photos")
public class PhotoQueryController {
    GET  /photos                - List photos (paginated, filtered)
    GET  /photos/{id}           - Get single photo details
}
```

**PhotoTagController**
```java
@RestController
@RequestMapping("/api/v1/photos/{photoId}/tags")
public class PhotoTagController {
    POST   /{tag}  - Add tag to photo
    DELETE /{tag}  - Remove tag from photo
    PATCH  /       - Replace all tags
}
```

#### Application Layer (Services)

**PhotoUploadService**
- Coordinates upload workflow
- Generates S3 pre-signed URLs
- Creates Photo entities
- Triggers async processing

**ImageProcessingService**
- Runs in background thread pool
- Downloads original from S3
- Compresses image (85% quality)
- Generates thumbnail (300px)
- Uploads processed versions back to S3
- Updates Photo status to COMPLETE

**UserService**
- User registration with BCrypt
- Authentication and JWT generation
- User profile management

#### Domain Layer

**Photo Entity**
```java
@Entity
public class Photo {
    - UUID id
    - String filename
    - Long fileSize
    - String contentType
    - PhotoStatus status (UPLOADING, PROCESSING, COMPLETE, FAILED)
    - String storageKey (S3 key)
    - Set<String> tags
    - UUID userId
    - Timestamps
}
```

**User Entity**
```java
@Entity
public class User {
    - UUID id
    - String email (unique)
    - String fullName
    - String passwordHash (BCrypt)
    - Timestamps
}
```

**UploadJob Aggregate**
```java
@Entity
public class UploadJob {
    - UUID id
    - UUID photoId
    - JobStatus status
    - String errorMessage
    - Timestamps
}
```

#### Infrastructure Layer

**JPA Photo Repository**
- Implements PhotoRepository interface
- Provides custom queries:
  - `findByUserId(UUID userId)`
  - `findByStatus(PhotoStatus status)`
  - `findByTagsContaining(String tag)`
  - `findByUserIdAndStatus(...)`

**S3 Storage Service**
- Implements StorageService interface
- Generates pre-signed URLs (upload/download)
- Uploads files to S3
- Downloads files from S3
- Checks file existence

**JWT Authentication Filter**
- Intercepts all requests
- Extracts JWT from Authorization header
- Validates token signature and expiry
- Sets Spring Security context

**Async Configuration**
- Configures thread pool:
  - Core: 5 threads
  - Max: 20 threads
  - Queue: 100 tasks
- Named executor: `taskExecutor`

---

## Supplementary: Upload Sequence Diagram

Detailed sequence showing the complete upload workflow.

```mermaid
sequenceDiagram
    actor User
    participant WebApp as Web/Mobile Client
    participant Backend as Spring Boot API
    participant DB as PostgreSQL
    participant S3 as AWS S3
    participant AsyncPool as Async Thread Pool
    
    User->>WebApp: Select photo(s) to upload
    
    rect rgb(200, 220, 255)
        Note over WebApp,Backend: Step 1: Initiate Upload
        WebApp->>Backend: POST /photos/upload/initiate<br/>{filename, fileSize, contentType}
        Backend->>DB: INSERT Photo (status=UPLOADING)
        DB-->>Backend: Photo created
        Backend->>Backend: Generate S3 key
        Backend->>S3: Generate pre-signed PUT URL (1h expiry)
        S3-->>Backend: Pre-signed URL
        Backend-->>WebApp: {photoId, uploadJobId, preSignedUrl}
    end
    
    rect rgb(255, 220, 200)
        Note over WebApp,S3: Step 2: Direct Upload to S3
        WebApp->>S3: PUT {fileData} to pre-signed URL
        S3-->>WebApp: 200 OK
    end
    
    rect rgb(200, 255, 220)
        Note over WebApp,Backend: Step 3: Mark Complete
        WebApp->>Backend: POST /uploads/{uploadJobId}/complete
        Backend->>DB: UPDATE Photo (status=PROCESSING)
        DB-->>Backend: Updated
        Backend->>AsyncPool: Trigger processImageAsync(photoId)
        Backend-->>WebApp: 200 OK
    end
    
    rect rgb(255, 255, 200)
        Note over AsyncPool,S3: Step 4: Background Processing
        AsyncPool->>AsyncPool: Wait 2s (S3 consistency)
        AsyncPool->>S3: Check file exists (HEAD)
        S3-->>AsyncPool: File exists
        AsyncPool->>S3: Download original
        S3-->>AsyncPool: File bytes
        AsyncPool->>AsyncPool: Compress image (85%)
        AsyncPool->>AsyncPool: Generate thumbnail (300px)
        AsyncPool->>S3: Upload compressed version
        AsyncPool->>S3: Upload thumbnail
        AsyncPool->>DB: UPDATE Photo<br/>(status=COMPLETE,<br/>thumbnailKey, compressedKey)
        DB-->>AsyncPool: Updated
    end
    
    User->>WebApp: Refresh gallery
    WebApp->>Backend: GET /photos
    Backend->>DB: SELECT photos WHERE userId=?
    DB-->>Backend: Photo list
    Backend->>Backend: Generate pre-signed download URLs
    Backend-->>WebApp: Photo DTOs with URLs
    WebApp->>WebApp: Display thumbnails
```

### Key Observations

1. **Non-Blocking**: Steps 1-3 complete in < 1 second. User gets immediate feedback.
2. **Direct Upload**: Client uploads directly to S3, bypassing backend for large files.
3. **Background Processing**: Image compression happens asynchronously, doesn't block upload.
4. **Eventual Consistency**: 2-second wait accounts for S3's eventual consistency model.
5. **Pre-Signed URLs**: All S3 access uses temporary, expiring URLs for security.

---

## Deployment View

Shows how components are deployed across infrastructure.

```mermaid
C4Deployment
    title Deployment Diagram - AWS Infrastructure
    
    Deployment_Node(internet, "Internet", "Public Network") {
        Deployment_Node(user_device, "User Device", "Browser/Mobile") {
            Container(web_app, "Web App", "React + Vite", "Browser-based UI")
            Container(mobile_app, "Mobile App", "React Native", "Native mobile UI")
        }
    }
    
    Deployment_Node(vercel, "Vercel", "CDN + Edge") {
        Container(web_hosting, "Static Site", "HTML/JS/CSS", "Web app hosting")
        Container(mobile_web, "Mobile Web", "HTML/JS/CSS", "Mobile web build")
    }
    
    Deployment_Node(aws, "AWS", "Cloud Provider") {
        Deployment_Node(vpc, "VPC", "us-east-1") {
            Deployment_Node(ec2, "EC2 Instance", "t3.medium") {
                Container(spring_boot, "Spring Boot API", "Java 21", "REST API")
            }
            
            Deployment_Node(rds, "RDS Instance", "db.t4g.micro") {
                ContainerDb(postgres, "PostgreSQL", "16", "Relational DB")
            }
        }
        
        Deployment_Node(s3_region, "S3", "us-east-1") {
            Container(s3_bucket, "S3 Bucket", "rapidphoto-uploads", "Object storage")
        }
    }
    
    Rel(web_app, web_hosting, "Loads from", "HTTPS")
    Rel(mobile_app, mobile_web, "Loads from", "HTTPS")
    
    Rel(web_app, spring_boot, "API calls", "HTTPS/REST")
    Rel(mobile_app, spring_boot, "API calls", "HTTPS/REST")
    
    Rel(web_app, s3_bucket, "Direct upload", "HTTPS/Pre-signed URL")
    Rel(mobile_app, s3_bucket, "Direct upload", "HTTPS/Pre-signed URL")
    
    Rel(spring_boot, postgres, "Queries", "PostgreSQL Protocol")
    Rel(spring_boot, s3_bucket, "SDK calls", "AWS SDK v2")
```

### Infrastructure Details

| Component | Type | Size | Purpose | Cost/Month |
|-----------|------|------|---------|-----------|
| EC2 Instance | t3.medium | 2 vCPU, 4 GB RAM | Backend API | ~$30 |
| RDS PostgreSQL | db.t4g.micro | 2 vCPU, 1 GB RAM, 20 GB SSD | Database | ~$15 |
| S3 Bucket | Standard | Variable storage | Object storage | $0.023/GB |
| Vercel | Hobby/Pro | N/A | Web hosting | Free/$20 |
| **Total** | | | | **~$45-65/mo** |

---

## Next Steps

- [Architecture Overview](./ARCHITECTURE.md)
- [Architecture Decision Records](./ADR/)
- [API Reference](./API_REFERENCE.md)
- [Deployment Guide](./DEPLOYMENT.md)

