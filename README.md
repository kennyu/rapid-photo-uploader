# Rapid Photo Uploader

A full-stack photo management system with batch uploads, asynchronous processing, tag-based organization, and secure cloud storage.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://reactjs.org/)
[![React Native](https://img.shields.io/badge/React%20Native-0.81.5-blue.svg)](https://reactnative.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![AWS S3](https://img.shields.io/badge/AWS-S3-orange.svg)](https://aws.amazon.com/s3/)

---

## Features

- **Batch Photo Upload** - Upload up to 100 photos simultaneously
- **Real-Time Progress** - Track upload progress for each photo
- **Asynchronous Processing** - Non-blocking image compression and thumbnail generation
- **Tag Management** - Organize photos with custom tags
- **Tag Filtering** - Find photos by tags instantly
- **Secure Storage** - AWS S3 with pre-signed URLs
- **Cross-Platform** - Web (React) and Mobile (React Native/Expo)
- **JWT Authentication** - Secure user authentication
- **Responsive Design** - Works on desktop, tablet, and mobile

---

## Architecture

Built with **Hexagonal Architecture** and **Domain-Driven Design (DDD)** principles:

```
┌─────────────────────────────────────────┐
│         API Layer (Adapters)            │  ← REST Controllers
├─────────────────────────────────────────┤
│      Application Layer (Use Cases)      │  ← Services, Handlers
├─────────────────────────────────────────┤
│   Domain Layer (Business Logic) CORE    │  ← Entities, Repositories
├─────────────────────────────────────────┤
│   Infrastructure Layer (Adapters)       │  ← JPA, S3, Security
└─────────────────────────────────────────┘
```

### Key Patterns

- **CQRS** - Separate commands (writes) from queries (reads)
- **Async Processing** - Background image processing with thread pools
- **Pre-Signed URLs** - Direct client-to-S3 uploads for scalability

**[Read Full Architecture Documentation →](./docs/ARCHITECTURE.md)**

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.7 (Java 21)
- **Database**: PostgreSQL 16 (AWS RDS)
- **Cloud Storage**: AWS S3
- **Image Processing**: Thumbnailator
- **Security**: Spring Security + JWT
- **Build**: Gradle 8.x

### Web Client
- **Framework**: React 18.3
- **Build Tool**: Vite 6.0
- **State Management**: React Query (TanStack Query)
- **HTTP Client**: Axios
- **Language**: TypeScript
- **Deployment**: Vercel

### Mobile Client
- **Framework**: React Native 0.81.5
- **Platform**: Expo SDK 54
- **Navigation**: React Navigation v7
- **HTTP Client**: Axios
- **Language**: TypeScript
- **Deployment**: Expo Go / EAS Build

---

## Project Structure

```
rapid-photo-uploader/
├── backend/                # Spring Boot backend
├── web-client/             # React web application
├── mobile-client/          # React Native mobile app
├── docs/                   # Technical documentation
│   ├── ARCHITECTURE.md     # Architecture overview
│   ├── C4_DIAGRAMS.md      # Visual diagrams
│   ├── DEPLOYMENT.md       # Deployment guide
│   ├── AI_TOOLS.md         # AI-assisted development
│   └── ADR/                # Architecture Decision Records
├── infrastructure/         # AWS infrastructure configs
└── aws-config/            # Deployment scripts
```

---

## Quick Start

### Prerequisites

- **Java 21** - [Download](https://adoptium.net/)
- **Node.js 20+** - [Download](https://nodejs.org/)
- **PostgreSQL 16** - [Download](https://www.postgresql.org/download/)
- **AWS Account** - For S3 storage
- **Git** - [Download](https://git-scm.com/)

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/rapid-photo-uploader.git
cd rapid-photo-uploader
```

### 2. Start Backend

```bash
# Start PostgreSQL (Docker)
docker run -d \
  --name postgres \
  -e POSTGRES_DB=rapidphoto \
  -e POSTGRES_USER=rapidphoto \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:16

# Configure environment
cd backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your AWS credentials

# Run backend
./gradlew bootRun
```

Backend runs at: http://localhost:8080

### 3. Start Web Client

```bash
cd web-client
npm install
npm run dev
```

Web app runs at: http://localhost:3000

### 4. Start Mobile Client

```bash
cd mobile-client
npm install
npm start
```

Scan QR code with Expo Go app on your device.

---

## Documentation

Comprehensive technical documentation available in the [`docs/`](./docs/) directory:

### Core Documentation
- **[Architecture Overview](./docs/ARCHITECTURE.md)** - Complete system architecture, layers, and patterns
- **[C4 Diagrams](./docs/C4_DIAGRAMS.md)** - Visual architecture diagrams
- **[Deployment Guide](./docs/DEPLOYMENT.md)** - Step-by-step deployment (EC2, RDS, S3, Vercel, Expo)
- **[AI Tools](./docs/AI_TOOLS.md)** - AI-assisted development workflow

### Architecture Decision Records (ADRs)
- **[ADR 001: Hexagonal Architecture](./docs/ADR/001-hexagonal-architecture.md)** - Why hexagonal + DDD
- **[ADR 002: S3 Pre-Signed URLs](./docs/ADR/002-s3-presigned-urls.md)** - Direct client uploads
- **[ADR 003: Async Image Processing](./docs/ADR/003-async-image-processing.md)** - Background processing

---

## Testing

### Backend Tests

```bash
cd backend
./gradlew test
```

- **Unit Tests** - Domain logic, services
- **Integration Tests** - Testcontainers (PostgreSQL + LocalStack)

### Web Client Tests

```bash
cd web-client
npm run test:e2e
```

- **E2E Tests** - Cypress tests for auth, upload, gallery

### Mobile Client Tests

```bash
cd mobile-client
npm run test:e2e
```

- **E2E Tests** - Detox tests for iOS, Android, Web

---

## Deployment

### Production Deployment

The system is designed for deployment on:
- **Backend**: AWS EC2 (Amazon Linux 2023)
- **Database**: AWS RDS (PostgreSQL 16)
- **Storage**: AWS S3
- **Web**: Vercel
- **Mobile**: Expo Go / EAS Build

**[Full Deployment Guide →](./docs/DEPLOYMENT.md)**

### Quick Deploy

```bash
# Backend to EC2
cd backend
./gradlew build
scp build/libs/rapid-photo-uploader-*.jar ec2-user@YOUR_IP:/opt/rapidphoto/

# Web to Vercel
cd web-client
vercel

# Mobile web build
cd mobile-client
npx expo export:web
```

---

## Performance

- **Concurrent Uploads**: 100 photos simultaneously
- **Processing Throughput**: 20 images/second
- **Upload Initiation**: ~50ms response time
- **Background Processing**: 2-5 seconds per image (non-blocking)
- **Daily Capacity**: ~360,000 images/day (single EC2 instance)

---

## Cost Estimate

Running on AWS with modest traffic:

| Resource | Monthly Cost |
|----------|-------------|
| EC2 (t3.medium) | ~$30 |
| RDS (db.t4g.micro) | ~$15 |
| S3 Storage (100 GB) | ~$2.30 |
| S3 Requests | ~$1.50 |
| Data Transfer | ~$4.50 |
| Vercel (Web) | Free |
| **Total** | **~$53/month** |

---

## Security

- **JWT Authentication** - 24-hour token expiry
- **BCrypt Passwords** - Strength 12
- **Pre-Signed URLs** - 1-hour expiry, no permanent access
- **IAM Roles** - No hardcoded credentials
- **S3 Encryption** - Server-side encryption (SSE-S3)
- **Private Bucket** - No public access
- **User Isolation** - Files stored in user-specific prefixes

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code style (ESLint, Prettier, Google Java Style)
- Write tests for new features
- Update documentation as needed
- Use conventional commits (`feat:`, `fix:`, `docs:`)

---

## API Endpoints

### Authentication
```
POST   /api/v1/auth/register  - Register new user
POST   /api/v1/auth/login     - Login (get JWT token)
```

### Photo Upload
```
POST   /api/v1/photos/upload/initiate              - Get pre-signed URL
POST   /api/v1/uploads/{uploadJobId}/complete      - Mark upload complete
```

### Photo Query
```
GET    /api/v1/photos              - List photos (paginated, filtered)
GET    /api/v1/photos/{id}         - Get photo details
GET    /api/v1/photos?tag=vacation - Filter by tag
```

### Tag Management
```
POST   /api/v1/photos/{id}/tags/{tag}  - Add tag
DELETE /api/v1/photos/{id}/tags/{tag}  - Remove tag
PATCH  /api/v1/photos/{id}/tags         - Replace all tags
```

**[Full API Reference →](./docs/ARCHITECTURE.md#api-layer-controllers)**

---

## Future Enhancements

- [ ] **AI-Powered Tagging** - Auto-tag photos using AWS Rekognition
- [ ] **Smart Search** - Natural language search with OpenAI embeddings
- [ ] **Image Enhancement** - AI upscaling, color correction
- [ ] **Content Moderation** - Detect inappropriate content
- [ ] **Video Support** - Upload and compress videos
- [ ] **Shared Albums** - Collaborate with other users
- [ ] **Mobile Push Notifications** - Processing complete alerts

---

## Support

- **Documentation**: See [`docs/`](./docs/) folder
- **Issues**: [GitHub Issues](https://github.com/yourusername/rapid-photo-uploader/issues)
- **Email**: support@rapidphoto.com

---

## License

MIT License - See [LICENSE](./LICENSE) for details.

---

## Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://reactjs.org/)
- [React Native](https://reactnative.dev/)
- [Expo](https://expo.dev/)
- [PostgreSQL](https://www.postgresql.org/)
- [AWS S3](https://aws.amazon.com/s3/)
- [Vercel](https://vercel.com/)
- [Thumbnailator](https://github.com/coobird/thumbnailator)

Developed using AI-assisted tools (Cursor AI, Claude, Task Master AI).

---

**Version**: 1.0  
**Status**: Production Ready  
**Last Updated**: November 2025

