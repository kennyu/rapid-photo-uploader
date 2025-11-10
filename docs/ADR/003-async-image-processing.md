# ADR 003: Asynchronous Image Processing with Thread Pools

**Status**: Accepted  
**Date**: 2025-11-09  
**Decision Makers**: Development Team  
**Technical Story**: Non-blocking image processing

---

## Context

After users upload photos to S3, the system needs to:
- Compress images to reduce storage costs (85% quality)
- Generate thumbnails for gallery display (300px max dimension)
- Process up to 100 images concurrently during batch uploads
- Avoid blocking upload API responses

Processing a 5 MB image takes ~2-3 seconds (download, compress, thumbnail, upload). If done synchronously:
- Upload API would take 3+ seconds to respond
- Only 1 image processed at a time
- Batch uploads (100 photos) would take 5+ minutes
- Poor user experience

---

## Decision

We will use **Spring's `@Async` annotation with a custom thread pool** to process images asynchronously in the background.

### Architecture

```
Upload Complete Request
        ↓
Mark Photo as PROCESSING (synchronous)
        ↓
Return 200 OK immediately ✅ (< 100ms)
        ↓
        ├─→ Background Thread Pool (async)
        │   └─→ processImageAsync(photoId)
        │       1. Wait 2s (S3 consistency)
        │       2. Download original from S3
        │       3. Compress (85% quality)
        │       4. Generate thumbnail (300px)
        │       5. Upload processed versions to S3
        │       6. Update Photo status = COMPLETE
        │
        └─→ User sees "PROCESSING" status in gallery
            └─→ After 3-5s, refreshes to see "COMPLETE"
```

### Thread Pool Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // Always-active threads
        executor.setMaxPoolSize(20);          // Max threads during peak
        executor.setQueueCapacity(100);       // Queue size before rejection
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()  // Fallback to sync
        );
        executor.initialize();
        return executor;
    }
}
```

### Usage in Service

```java
@Service
public class ImageProcessingService {
    
    @Async("taskExecutor")
    @Transactional
    public void processImageAsync(UUID photoId) {
        log.info("Starting async processing for photo: {}", photoId);
        
        try {
            Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));
            
            // Wait for S3 eventual consistency
            Thread.sleep(2000);
            
            // Verify file exists
            boolean exists = storageService.fileExists(photo.getStorageKey());
            if (!exists) {
                throw new RuntimeException("File not found in S3");
            }
            
            // Download original
            InputStream original = storageService.downloadFile(photo.getStorageKey());
            
            // Compress image (85% quality)
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            Thumbnails.of(original)
                .scale(1.0)
                .outputQuality(0.85)
                .outputFormat("jpg")
                .toOutputStream(compressed);
            
            // Generate thumbnail (300px max)
            ByteArrayOutputStream thumbnail = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(compressed.toByteArray()))
                .size(300, 300)
                .toOutputStream(thumbnail);
            
            // Upload processed versions
            String compressedKey = photo.getStorageKey().replace(
                photo.getFilename(), 
                photo.getPhotoId() + "-compressed.jpg"
            );
            String thumbnailKey = photo.getStorageKey().replace(
                photo.getFilename(),
                photo.getPhotoId() + "-thumbnail.jpg"
            );
            
            storageService.uploadFile(compressedKey, 
                new ByteArrayInputStream(compressed.toByteArray()), 
                "image/jpeg");
            
            storageService.uploadFile(thumbnailKey,
                new ByteArrayInputStream(thumbnail.toByteArray()),
                "image/jpeg");
            
            // Update photo status
            photo.setStatus(PhotoStatus.COMPLETE);
            photo.setCompressedKey(compressedKey);
            photo.setThumbnailKey(thumbnailKey);
            photoRepository.save(photo);
            
            log.info("Successfully processed photo: {}", photoId);
            
        } catch (Exception e) {
            log.error("Failed to process photo: {}", photoId, e);
            markProcessingFailed(photoId, e.getMessage());
        }
    }
    
    private void markProcessingFailed(UUID photoId, String errorMessage) {
        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo != null) {
            photo.setStatus(PhotoStatus.FAILED);
            photoRepository.save(photo);
        }
    }
}
```

---

## Rationale

### Why Asynchronous Processing?

#### 1. **Fast API Response**

**Synchronous** (BAD):
```
POST /uploads/complete → Process image → Return response
Time: 3000ms ❌
```

**Asynchronous** (GOOD):
```
POST /uploads/complete → Mark PROCESSING → Return 200 OK
Time: 50ms ✅

Background: Process image (3000ms) → Update to COMPLETE
```

**User Experience**:
- Sees upload complete immediately
- Gallery shows "PROCESSING" status
- Refreshes to see "COMPLETE" after a few seconds

#### 2. **Concurrent Processing**

**Batch Upload (100 photos)**:

| Approach | Time to Process All | User Waiting |
|----------|---------------------|--------------|
| Synchronous | 100 × 3s = **300s (5 min)** | ❌ 5 minutes |
| Async (20 threads) | 100 / 20 × 3s = **15s** | ✅ 15 seconds |

**Improvement**: 20× faster for batch uploads

#### 3. **Resource Management**

**Thread Pool Behavior**:
```
Tasks 1-5:    Use core threads (immediate start)
Tasks 6-105:  Queue tasks (core threads busy)
Tasks 106+:   Spawn new threads up to max (20)
Tasks 121+:   Reject (queue full, max threads reached)
```

**Rejection Policy**: `CallerRunsPolicy`
- If queue full, process synchronously in API thread
- Provides backpressure, prevents OOM
- Rare in practice (only if 120+ concurrent uploads)

#### 4. **Isolation**

**Benefits**:
- Image processing failures don't crash API
- Long-running tasks don't block HTTP threads
- Can monitor async pool separately (metrics)
- Can tune thread pool without restarting

### Alternative Considered: Message Queue (SQS)

**Pros**:
- Fully distributed processing (multiple workers)
- Survives server restarts (durable queue)
- Built-in retry logic
- Auto-scaling based on queue depth

**Cons**:
- Additional infrastructure (SQS)
- More complex architecture
- Higher latency (queue polling)
- Additional costs (~$0.50/million requests)

**Decision**: Thread pools sufficient for current scale (single EC2 instance). Can migrate to SQS if processing needs exceed single server capacity.

---

## Consequences

### Positive

✅ **Fast API**: Upload complete in < 100ms  
✅ **Concurrent Processing**: 20 images at once  
✅ **Fault Isolation**: Processing errors don't crash API  
✅ **Scalability**: Can increase thread pool size  
✅ **Simple**: No additional infrastructure needed  

### Negative

❌ **Lost on Restart**: In-progress tasks lost if server restarts  
❌ **Single Server**: Limited to one EC2 instance's capacity  
❌ **No Retry**: Failed tasks must be manually retried  
❌ **Memory Usage**: All threads share EC2 instance memory  
❌ **Monitoring**: Harder to track async tasks than queue messages  

### Mitigation

- **Restart Safety**: Photos marked PROCESSING, can be reprocessed
- **Scaling Path**: Migrate to SQS when exceeding 1000 photos/hour
- **Retry Logic**: Client can re-trigger processing via API
- **Memory**: Use streaming APIs, avoid loading full images
- **Monitoring**: Track thread pool metrics (active threads, queue size)

---

## Performance Analysis

### Single EC2 Instance Capacity

**EC2 Instance**: t3.medium (2 vCPU, 4 GB RAM)

**Bottleneck**: Image processing is **CPU-bound**

**Throughput**:
```
Processing time per image: 3 seconds (compress + thumbnail)
Thread pool max: 20 threads
Throughput: 20 images / 3 seconds = 6.67 images/second
            ≈ 400 images/minute
            ≈ 24,000 images/hour
```

**Real-World Capacity** (accounting for GC, overhead):
- ~15,000 images/hour sustained
- ~300 concurrent users (50 uploads/user)

**When to Scale**:
- If traffic exceeds 10,000 images/hour consistently
- If thread pool queue frequently hits capacity (100)
- If processing latency > 10 seconds regularly

---

## S3 Eventual Consistency Handling

### Problem

S3 has **eventual consistency** for new object creation:
- Upload completes successfully
- File may not be immediately available for download (~1-2 seconds)
- Backend tries to download → `NoSuchKey` exception

### Solution: Wait and Verify

```java
// Wait 2 seconds for S3 to propagate
Thread.sleep(2000);

// Verify file exists before downloading
boolean exists = storageService.fileExists(photo.getStorageKey());
if (!exists) {
    throw new RuntimeException("File not found in S3 after waiting");
}

// Now safe to download
InputStream file = storageService.downloadFile(photo.getStorageKey());
```

**Alternative**: Exponential backoff retry
```java
for (int attempt = 1; attempt <= 5; attempt++) {
    if (storageService.fileExists(key)) {
        return storageService.downloadFile(key);
    }
    Thread.sleep(1000 * attempt);  // 1s, 2s, 3s, 4s, 5s
}
throw new RuntimeException("File not available after 5 retries");
```

---

## Error Handling

### Failure Scenarios

1. **S3 Download Failure**: File not found, network error
2. **Processing Failure**: Corrupt image, unsupported format
3. **S3 Upload Failure**: Network error, permission issue
4. **Database Failure**: Can't update photo status

### Error Recovery

```java
@Async("taskExecutor")
public void processImageAsync(UUID photoId) {
    try {
        // ... processing logic ...
        
        photo.setStatus(PhotoStatus.COMPLETE);
        photoRepository.save(photo);
        
    } catch (Exception e) {
        log.error("Processing failed for photo: {}", photoId, e);
        
        // Mark as FAILED
        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo != null) {
            photo.setStatus(PhotoStatus.FAILED);
            photoRepository.save(photo);
        }
        
        // Don't rethrow - this is already async, nowhere to propagate
    }
}
```

**User-Facing**:
- Gallery shows "FAILED" status
- User can click "Retry Processing" button
- API endpoint: `POST /photos/{photoId}/reprocess`

---

## Monitoring and Observability

### Key Metrics

```java
@Slf4j
@Service
public class ImageProcessingService {
    
    private final Counter processedCounter = Counter.builder("photos.processed")
        .tag("status", "success")
        .register(registry);
    
    private final Counter failedCounter = Counter.builder("photos.processed")
        .tag("status", "failed")
        .register(registry);
    
    private final Timer processingTimer = Timer.builder("photos.processing.time")
        .register(registry);
    
    @Async("taskExecutor")
    public void processImageAsync(UUID photoId) {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            // ... processing ...
            processedCounter.increment();
            
        } catch (Exception e) {
            failedCounter.increment();
            
        } finally {
            sample.stop(processingTimer);
        }
    }
}
```

### Thread Pool Metrics

```java
@Bean
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // ... configuration ...
    
    // Expose metrics
    Metrics.gauge("threadpool.active", executor, ThreadPoolTaskExecutor::getActiveCount);
    Metrics.gauge("threadpool.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize);
    Metrics.gauge("threadpool.queue.size", executor, 
        e -> e.getThreadPoolExecutor().getQueue().size());
    
    return executor;
}
```

### CloudWatch Alarms

- Alert if processing failure rate > 5%
- Alert if active threads = max for > 5 minutes
- Alert if queue size > 80 for > 2 minutes
- Alert if processing time > 10 seconds

---

## Testing Strategy

### Unit Tests

```java
@Test
void processImageAsync_shouldCompressAndGenerateThumbnail() throws Exception {
    // Given
    UUID photoId = UUID.randomUUID();
    Photo photo = createTestPhoto(photoId);
    when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
    when(storageService.fileExists(any())).thenReturn(true);
    when(storageService.downloadFile(any())).thenReturn(testImageStream());
    
    // When
    imageProcessingService.processImageAsync(photoId);
    
    // Then (need to wait for async completion)
    await().atMost(5, SECONDS).untilAsserted(() -> {
        verify(storageService, times(2)).uploadFile(any(), any(), any());
        verify(photoRepository, times(2)).save(argThat(p -> 
            p.getStatus() == PhotoStatus.COMPLETE
        ));
    });
}
```

### Integration Tests

```java
@SpringBootTest
@DirtiesContext  // Clean up async threads between tests
class ImageProcessingIntegrationTest {
    
    @Autowired
    private ImageProcessingService service;
    
    @Test
    void processImageAsync_shouldHandleRealImage() throws Exception {
        // Use Testcontainers for LocalStack (S3) and PostgreSQL
        // Test with real image file
        // Verify compressed and thumbnail are created
    }
}
```

---

## Future Enhancements

1. **Message Queue Migration**: Migrate to SQS when traffic > 10,000/hour
2. **Distributed Tracing**: Add OpenTelemetry for async task tracking
3. **Priority Queues**: Process premium users' photos first
4. **Auto-Scaling**: Scale EC2 instances based on queue depth
5. **Machine Learning**: Add AI tagging as additional async step
6. **Video Support**: Extend to video compression and thumbnail extraction

---

## References

- [Spring @Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)
- [ThreadPoolExecutor Javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)
- [Thumbnailator Library](https://github.com/coobird/thumbnailator)
- [AWS S3 Consistency Model](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html#ConsistencyModel)

---

**Next**: [ADR 004: CQRS Pattern Implementation](./004-cqrs-pattern.md)

