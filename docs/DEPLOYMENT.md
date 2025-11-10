# Deployment Guide

This guide covers deploying the Rapid Photo Uploader across all three components: Backend (EC2), Web Frontend (Vercel), and Mobile (Expo).

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Backend Deployment (AWS EC2)](#backend-deployment-aws-ec2)
3. [Database Setup (AWS RDS)](#database-setup-aws-rds)
4. [Storage Setup (AWS S3)](#storage-setup-aws-s3)
5. [Web Frontend Deployment (Vercel)](#web-frontend-deployment-vercel)
6. [Mobile Deployment (Expo)](#mobile-deployment-expo)
7. [Environment Variables](#environment-variables)
8. [Monitoring and Maintenance](#monitoring-and-maintenance)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Accounts
- AWS Account (EC2, RDS, S3)
- Vercel Account (free tier sufficient)
- Expo Account (optional, for EAS builds)

### Required Tools
- AWS CLI (`aws --version`)
- Node.js 20+ (`node --version`)
- Java 21 (`java --version`)
- Gradle 8+ (`gradle --version`)
- Git (`git --version`)

### DNS Setup (Optional)
- Domain name (e.g., rapidphoto.com)
- Route 53 or other DNS provider

---

## Backend Deployment (AWS EC2)

### 1. Launch EC2 Instance

#### Via AWS Console

1. **Navigate to EC2 Dashboard**
2. **Launch Instance**:
   - **Name**: `rapidphoto-backend`
   - **AMI**: Amazon Linux 2023
   - **Instance Type**: t3.medium (2 vCPU, 4 GB RAM)
   - **Key Pair**: Create new or use existing
   - **Network**: Default VPC, enable public IP
   - **Security Group**: 
     - SSH (22) from your IP
     - HTTP (8080) from anywhere (0.0.0.0/0)
     - HTTPS (443) from anywhere (optional, for SSL)
   - **Storage**: 20 GB gp3 SSD

#### Via AWS CLI

```bash
# Create security group
aws ec2 create-security-group \
  --group-name rapidphoto-backend-sg \
  --description "Security group for RapidPhoto backend"

# Add inbound rules
aws ec2 authorize-security-group-ingress \
  --group-name rapidphoto-backend-sg \
  --protocol tcp --port 22 --cidr YOUR_IP/32

aws ec2 authorize-security-group-ingress \
  --group-name rapidphoto-backend-sg \
  --protocol tcp --port 8080 --cidr 0.0.0.0/0

# Launch instance
aws ec2 run-instances \
  --image-id ami-XXXXX \
  --instance-type t3.medium \
  --key-name your-key-pair \
  --security-groups rapidphoto-backend-sg \
  --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3}'
```

### 2. Install Java 21

SSH into the instance:
```bash
ssh -i your-key.pem ec2-user@YOUR_INSTANCE_IP
```

Install Java:
```bash
# Update system
sudo yum update -y

# Install Java 21 (Amazon Corretto)
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -L -o /etc/yum.repos.d/corretto.repo \
  https://yum.corretto.aws/corretto.repo
sudo yum install -y java-21-amazon-corretto-devel

# Verify installation
java --version
```

### 3. Build Application

On your local machine:
```bash
cd backend
./gradlew clean build -x test

# Verify JAR created
ls -lh build/libs/rapid-photo-uploader-*.jar
```

### 4. Transfer JAR to EC2

```bash
# Create application directory on EC2
ssh ec2-user@YOUR_INSTANCE_IP "sudo mkdir -p /opt/rapidphoto && sudo chown ec2-user /opt/rapidphoto"

# Transfer JAR
scp -i your-key.pem \
  backend/build/libs/rapid-photo-uploader-*.jar \
  ec2-user@YOUR_INSTANCE_IP:/opt/rapidphoto/app.jar
```

### 5. Create systemd Service

SSH into EC2 and create service file:
```bash
sudo nano /etc/systemd/system/rapidphoto.service
```

Service configuration:
```ini
[Unit]
Description=Rapid Photo Uploader Backend
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/rapidphoto
ExecStart=/usr/bin/java -jar /opt/rapidphoto/app.jar
Restart=on-failure
RestartSec=10

# Environment Variables
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/rapidphoto"
Environment="SPRING_DATASOURCE_USERNAME=YOUR_DB_USER"
Environment="SPRING_DATASOURCE_PASSWORD=YOUR_DB_PASSWORD"
Environment="AWS_REGION=us-east-1"
Environment="AWS_S3_BUCKET_NAME=rapidphoto-uploads-YOUR_ACCOUNT_ID"
Environment="JWT_SECRET=YOUR_SECURE_SECRET_KEY"
Environment="JWT_EXPIRATION_MS=86400000"

# JVM Options
Environment="JAVA_OPTS=-Xmx3G -Xms512M -XX:+UseG1GC"

[Install]
WantedBy=multi-user.target
```

**Important**: Replace placeholders with actual values.

### 6. Start Service

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service (start on boot)
sudo systemctl enable rapidphoto

# Start service
sudo systemctl start rapidphoto

# Check status
sudo systemctl status rapidphoto

# View logs
sudo journalctl -u rapidphoto -f
```

### 7. Configure IAM Role (for S3 Access)

#### Create IAM Role

1. **Navigate to IAM → Roles → Create Role**
2. **Select**: AWS service → EC2
3. **Attach Policy**:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:PutObject",
           "s3:GetObject",
           "s3:DeleteObject",
           "s3:ListBucket",
           "s3:HeadObject"
         ],
         "Resource": [
           "arn:aws:s3:::rapidphoto-uploads-YOUR_ACCOUNT_ID/*",
           "arn:aws:s3:::rapidphoto-uploads-YOUR_ACCOUNT_ID"
         ]
       }
     ]
   }
   ```
4. **Name**: `RapidPhotoBackendRole`

#### Attach Role to EC2 Instance

```bash
aws ec2 associate-iam-instance-profile \
  --instance-id i-XXXXX \
  --iam-instance-profile Name=RapidPhotoBackendRole
```

Or via Console: EC2 → Actions → Security → Modify IAM Role

### 8. Verify Deployment

```bash
# Test health endpoint
curl http://YOUR_INSTANCE_IP:8080/actuator/health

# Expected response:
# {"status":"UP"}

# Test API
curl http://YOUR_INSTANCE_IP:8080/api/v1/photos
# Expected: 401 Unauthorized (auth required)
```

---

## Database Setup (AWS RDS)

### 1. Create RDS PostgreSQL Instance

#### Via AWS Console

1. **Navigate to RDS → Databases → Create Database**
2. **Configuration**:
   - **Engine**: PostgreSQL 16
   - **Templates**: Free tier or Production
   - **DB Instance Identifier**: `rapidphoto-db`
   - **Master Username**: `rapidphoto_admin`
   - **Master Password**: (generate secure password)
   - **Instance Class**: db.t4g.micro (free tier) or db.t4g.small
   - **Storage**: 20 GB gp3 SSD
   - **Multi-AZ**: No (dev), Yes (production)
   - **VPC**: Same as EC2 instance
   - **Public Access**: No
   - **Security Group**: Create new or use existing
     - PostgreSQL (5432) from EC2 security group

#### Via AWS CLI

```bash
# Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name rapidphoto-db-subnet \
  --db-subnet-group-description "RapidPhoto DB subnet group" \
  --subnet-ids subnet-XXXXX subnet-YYYYY

# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier rapidphoto-db \
  --db-instance-class db.t4g.micro \
  --engine postgres \
  --engine-version 16.1 \
  --master-username rapidphoto_admin \
  --master-user-password YOUR_SECURE_PASSWORD \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-XXXXX \
  --db-subnet-group-name rapidphoto-db-subnet \
  --backup-retention-period 7 \
  --no-multi-az \
  --no-publicly-accessible
```

### 2. Update Security Groups

Allow EC2 to access RDS:
```bash
# Get EC2 security group ID
EC2_SG=$(aws ec2 describe-instances \
  --instance-ids i-XXXXX \
  --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId' \
  --output text)

# Add inbound rule to RDS security group
aws ec2 authorize-security-group-ingress \
  --group-id RDS_SG_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $EC2_SG
```

### 3. Initialize Database

From EC2 instance (after installing `postgresql-client`):
```bash
# Install PostgreSQL client
sudo yum install -y postgresql15

# Connect to database
psql -h YOUR_RDS_ENDPOINT -U rapidphoto_admin -d postgres

# Create database
CREATE DATABASE rapidphoto;

# Connect to database
\c rapidphoto

# Create extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

# Exit
\q
```

### 4. Run Flyway Migrations

Migrations run automatically on backend startup via Flyway.

To run manually:
```bash
cd backend
./gradlew flywayMigrate \
  -Pflyway.url=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/rapidphoto \
  -Pflyway.user=rapidphoto_admin \
  -Pflyway.password=YOUR_PASSWORD
```

---

## Storage Setup (AWS S3)

### 1. Create S3 Bucket

```bash
# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create bucket
aws s3 mb s3://rapidphoto-uploads-${ACCOUNT_ID} --region us-east-1

# Enable versioning (optional)
aws s3api put-bucket-versioning \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --versioning-configuration Status=Enabled
```

### 2. Configure CORS

Create `cors.json`:
```json
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedOrigins": [
        "http://localhost:3000",
        "http://localhost:8081",
        "https://your-vercel-app.vercel.app"
      ],
      "ExposeHeaders": ["ETag", "x-amz-server-side-encryption"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

Apply CORS:
```bash
aws s3api put-bucket-cors \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --cors-configuration file://cors.json
```

### 3. Enable Encryption

```bash
aws s3api put-bucket-encryption \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

### 4. Configure Lifecycle Policy

Create `lifecycle.json`:
```json
{
  "Rules": [
    {
      "Id": "TransitionToIA",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "STANDARD_IA"
        }
      ]
    },
    {
      "Id": "CleanupIncompleteUploads",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 1
      }
    }
  ]
}
```

Apply lifecycle:
```bash
aws s3api put-bucket-lifecycle-configuration \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --lifecycle-configuration file://lifecycle.json
```

---

## Web Frontend Deployment (Vercel)

### 1. Prepare Repository

Ensure `web-client` is in Git repository.

### 2. Connect to Vercel

#### Via Vercel Dashboard

1. Go to [vercel.com](https://vercel.com)
2. Click **"New Project"**
3. Import your Git repository
4. **Root Directory**: `web-client`
5. **Framework Preset**: Vite
6. **Build Command**: `npm run build`
7. **Output Directory**: `dist`

#### Via Vercel CLI

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy
cd web-client
vercel
```

### 3. Configure Environment Variables

In Vercel Dashboard → Settings → Environment Variables:

```
VITE_API_URL=http://YOUR_EC2_IP:8080/api/v1
```

Or via CLI:
```bash
vercel env add VITE_API_URL production
# Enter value: http://YOUR_EC2_IP:8080/api/v1
```

### 4. Deploy

Push to Git:
```bash
git add .
git commit -m "Deploy web client"
git push origin main
```

Vercel auto-deploys on push to `main`.

### 5. Custom Domain (Optional)

In Vercel Dashboard → Settings → Domains:
1. Add domain: `rapidphoto.com`
2. Configure DNS:
   - Add CNAME record: `www` → `cname.vercel-dns.com`
   - Add A records: `@` → Vercel IPs

---

## Mobile Deployment (Expo)

### Development (Expo Go)

1. **Start Dev Server**:
   ```bash
   cd mobile-client
   npm start
   ```

2. **Scan QR Code**:
   - iOS: Camera app
   - Android: Expo Go app

### Web Build

1. **Build for Web**:
   ```bash
   cd mobile-client
   npx expo export:web
   ```

2. **Deploy to Vercel**:
   ```bash
   cd web-build
   vercel
   ```

3. **Configure Environment**:
   - In `app.json`:
     ```json
     {
       "expo": {
         "extra": {
           "apiUrl": "http://YOUR_EC2_IP:8080/api/v1"
         }
       }
     }
     ```

### Native Builds (EAS Build)

#### 1. Install EAS CLI

```bash
npm install -g eas-cli
eas login
```

#### 2. Configure EAS

```bash
cd mobile-client
eas build:configure
```

Creates `eas.json`:
```json
{
  "cli": {
    "version": ">= 5.0.0"
  },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal"
    },
    "preview": {
      "distribution": "internal",
      "android": {
        "buildType": "apk"
      }
    },
    "production": {
      "android": {
        "buildType": "app-bundle"
      },
      "ios": {
        "buildConfiguration": "Release"
      }
    }
  },
  "submit": {
    "production": {}
  }
}
```

#### 3. Build for Android

```bash
# Build APK (for testing)
eas build --platform android --profile preview

# Build AAB (for Play Store)
eas build --platform android --profile production
```

#### 4. Build for iOS

```bash
# Requires Apple Developer account
eas build --platform ios --profile production
```

#### 5. Submit to Stores

```bash
# Google Play Store
eas submit --platform android

# Apple App Store
eas submit --platform ios
```

---

## Environment Variables

### Backend (`/etc/systemd/system/rapidphoto.service`)

```ini
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/rapidphoto"
Environment="SPRING_DATASOURCE_USERNAME=rapidphoto_admin"
Environment="SPRING_DATASOURCE_PASSWORD=YOUR_DB_PASSWORD"
Environment="AWS_REGION=us-east-1"
Environment="AWS_S3_BUCKET_NAME=rapidphoto-uploads-YOUR_ACCOUNT_ID"
Environment="JWT_SECRET=YOUR_SECURE_SECRET_KEY"
Environment="JWT_EXPIRATION_MS=86400000"
Environment="CORS_ALLOWED_ORIGINS=https://your-vercel-app.vercel.app,http://localhost:3000"
```

### Web Client (Vercel)

```
VITE_API_URL=http://YOUR_EC2_IP:8080/api/v1
```

### Mobile Client (`app.json`)

```json
{
  "expo": {
    "extra": {
      "apiUrl": "http://YOUR_EC2_IP:8080/api/v1"
    }
  }
}
```

---

## Monitoring and Maintenance

### Backend Monitoring

#### Check Service Status

```bash
sudo systemctl status rapidphoto
```

#### View Logs

```bash
# Real-time logs
sudo journalctl -u rapidphoto -f

# Last 100 lines
sudo journalctl -u rapidphoto -n 100

# Errors only
sudo journalctl -u rapidphoto -p err
```

#### Monitor Resources

```bash
# CPU and memory
top

# Disk usage
df -h

# Network connections
ss -tunap | grep 8080
```

### Database Monitoring

#### RDS Metrics (CloudWatch)

- CPU Utilization
- Database Connections
- Free Storage Space
- Read/Write IOPS

#### Manual Queries

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Long-running queries
SELECT pid, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY duration DESC;

-- Table sizes
SELECT 
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### S3 Monitoring

```bash
# Bucket size
aws s3 ls s3://rapidphoto-uploads-${ACCOUNT_ID} --recursive --summarize

# Object count
aws s3 ls s3://rapidphoto-uploads-${ACCOUNT_ID} --recursive | wc -l
```

---

## Troubleshooting

### Backend Won't Start

**Check logs**:
```bash
sudo journalctl -u rapidphoto -n 100
```

**Common issues**:
1. **Database connection failed**:
   - Verify RDS endpoint in environment variables
   - Check security group allows EC2 → RDS on port 5432
   - Test connection: `psql -h RDS_ENDPOINT -U rapidphoto_admin -d rapidphoto`

2. **S3 access denied**:
   - Verify IAM role attached to EC2 instance
   - Check bucket name matches environment variable
   - Test IAM role: `aws s3 ls s3://rapidphoto-uploads-${ACCOUNT_ID}`

3. **Port 8080 already in use**:
   - Check: `sudo lsof -i :8080`
   - Kill process: `sudo kill -9 PID`

### Web Client Can't Connect to Backend

**CORS errors**:
1. Update S3 CORS configuration to include Vercel domain
2. Update backend CORS allowed origins:
   ```properties
   cors.allowed-origins=https://your-vercel-app.vercel.app
   ```
3. Restart backend: `sudo systemctl restart rapidphoto`

**401 Unauthorized**:
- Check JWT token in browser DevTools → Network → Headers
- Verify token not expired (24h default)
- Re-login to get fresh token

### Mobile App Upload Fails

**Pre-signed URL expired**:
- URLs expire after 1 hour
- Re-initiate upload

**CORS error**:
- Add `http://localhost:8081` to S3 CORS allowed origins

**S3 403 Forbidden**:
- Verify IAM role has `s3:PutObject` permission
- Check bucket policy

---

## Backup and Disaster Recovery

### Database Backups

RDS automated backups (enabled by default):
- Retention: 7 days
- Backup window: 03:00-04:00 UTC

Manual snapshot:
```bash
aws rds create-db-snapshot \
  --db-snapshot-identifier rapidphoto-snapshot-$(date +%Y%m%d) \
  --db-instance-identifier rapidphoto-db
```

### S3 Backups

Enable versioning (already done):
```bash
aws s3api put-bucket-versioning \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --versioning-configuration Status=Enabled
```

Cross-region replication (optional):
```bash
# Create destination bucket
aws s3 mb s3://rapidphoto-uploads-backup-${ACCOUNT_ID} --region us-west-2

# Configure replication
aws s3api put-bucket-replication \
  --bucket rapidphoto-uploads-${ACCOUNT_ID} \
  --replication-configuration file://replication.json
```

---

## Scaling Strategy

### Horizontal Scaling

1. **Application Load Balancer**:
   - Create ALB targeting multiple EC2 instances
   - Enable sticky sessions (for JWT tokens in cookies)

2. **Auto Scaling Group**:
   - Min: 2 instances
   - Max: 10 instances
   - Scale on CPU > 70%

3. **Shared Session Storage**:
   - Use Redis (ElastiCache) for session management
   - Store JWT tokens in Redis instead of stateless

### Vertical Scaling

- **EC2**: t3.medium → t3.large → t3.xlarge
- **RDS**: db.t4g.micro → db.t4g.small → db.t4g.medium

---

## Cost Estimation

### Monthly Costs (Estimate)

| Resource | Type | Cost |
|----------|------|------|
| EC2 (t3.medium) | 730 hours | ~$30 |
| RDS (db.t4g.micro) | 730 hours | ~$15 |
| S3 Storage | 100 GB | ~$2.30 |
| S3 Requests | 100k PUT, 1M GET | ~$1.50 |
| Data Transfer | 50 GB out | ~$4.50 |
| Vercel | Hobby plan | Free |
| **Total** | | **~$53/month** |

---

## Next Steps

After deployment:
1. ✅ Test all features end-to-end
2. ✅ Configure monitoring (CloudWatch, alerts)
3. ✅ Set up SSL/TLS (Let's Encrypt or AWS Certificate Manager)
4. ✅ Configure domain name (Route 53)
5. ✅ Enable automated backups
6. ✅ Load test (JMeter, Gatling)
7. ✅ Document runbook for common operations

---

**Related**:
- [Architecture Overview](./ARCHITECTURE.md)
- [AI Tools](./AI_TOOLS.md)
- [ADRs](./ADR/)

