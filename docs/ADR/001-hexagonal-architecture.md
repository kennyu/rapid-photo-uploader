# ADR 001: Adopt Hexagonal Architecture with DDD

**Status**: Accepted  
**Date**: 2025-11-09  
**Decision Makers**: Development Team  
**Technical Story**: Backend architecture foundation

---

## Context

The Rapid Photo Uploader backend needs a clear architectural pattern that:
- Separates business logic from technical concerns
- Enables independent testing of domain logic
- Allows swapping infrastructure components (database, storage) without affecting business rules
- Supports future scaling and feature additions

---

## Decision

We will adopt **Hexagonal Architecture** (Ports and Adapters) combined with **Domain-Driven Design (DDD)** principles for the backend.

### Architecture Layers

```
┌─────────────────────────────────────────┐
│         API Layer (Adapters)            │  ← REST Controllers
├─────────────────────────────────────────┤
│      Application Layer (Use Cases)      │  ← Services, Handlers
├─────────────────────────────────────────┤
│   Domain Layer (Business Logic) CORE    │  ← Entities, Repositories (interfaces)
├─────────────────────────────────────────┤
│   Infrastructure Layer (Adapters)       │  ← JPA, S3, Redis, etc.
└─────────────────────────────────────────┘
```

### Package Structure

```
com.rapidphoto.uploader/
├── api/           # Inbound adapters (REST controllers, DTOs)
├── application/   # Use case orchestration (services)
├── domain/        # Core business logic (entities, interfaces)
└── infrastructure/# Outbound adapters (JPA, S3, security)
```

### Key Principles

1. **Domain Independence**: Domain layer has zero dependencies on frameworks
2. **Dependency Inversion**: Infrastructure depends on domain, not vice versa
3. **Ports**: Interfaces defined in domain layer (e.g., `PhotoRepository`)
4. **Adapters**: Implementations in infrastructure layer (e.g., `JpaPhotoRepository`)

---

## Rationale

### Why Hexagonal Architecture?

1. **Testability**: Domain logic can be tested without databases, HTTP, or external services
2. **Flexibility**: Can swap PostgreSQL for MongoDB without changing domain logic
3. **Clarity**: Clear boundaries between layers prevent mixing concerns
4. **Maintainability**: Business rules are isolated and easy to locate
5. **Scalability**: Easy to add new adapters (REST → GraphQL, JPA → NoSQL)

### Why DDD?

1. **Ubiquitous Language**: Code mirrors business terminology (Photo, Upload, Tag)
2. **Rich Domain Models**: Entities contain business logic, not just data
3. **Aggregates**: Clear consistency boundaries (e.g., UploadJob aggregate)
4. **Domain Events**: Natural fit for async processing

### Alternative Considered: Layered Architecture

**Pros**:
- Simpler, more common pattern
- Easier for junior developers to understand
- Less boilerplate

**Cons**:
- Domain logic leaks into controllers
- Tight coupling to frameworks (Spring, JPA)
- Hard to test without mocking infrastructure
- Database schema often drives design

**Decision**: Hexagonal + DDD better supports long-term maintainability and testing.

---

## Consequences

### Positive

✅ **Clean Separation**: Business logic isolated from technical details  
✅ **Testable**: Domain logic tested without Spring Boot context  
✅ **Flexible**: Can replace infrastructure components independently  
✅ **Clear Contracts**: Repository interfaces define exactly what domain needs  
✅ **Refactoring Safety**: Changes to infrastructure don't break domain  

### Negative

❌ **More Boilerplate**: Requires interfaces + implementations for repositories  
❌ **Learning Curve**: Team must understand hexagonal concepts  
❌ **Mapping Overhead**: DTOs ↔ Entities require mappers  
❌ **Initial Complexity**: More packages and indirection  

### Mitigation

- **Documentation**: Provide clear package structure guide and examples
- **Code Reviews**: Ensure layer boundaries are respected
- **Mappers**: Use MapStruct to reduce manual mapping code
- **Templates**: Create templates for new features to follow pattern

---

## Implementation Examples

### Good: Domain Entity (No Framework Dependencies)

```java
package com.rapidphoto.uploader.domain.model;

public class Photo {
    private final UUID id;
    private String filename;
    private PhotoStatus status;
    private Set<String> tags;
    
    // Business logic methods
    public void markAsProcessing() {
        if (this.status != PhotoStatus.UPLOADING) {
            throw new IllegalStateException("Can only process uploaded photos");
        }
        this.status = PhotoStatus.PROCESSING;
    }
    
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag cannot be blank");
        }
        this.tags.add(tag.trim().toLowerCase());
    }
}
```

### Good: Repository Interface (Port)

```java
package com.rapidphoto.uploader.domain.repository;

public interface PhotoRepository {
    Photo save(Photo photo);
    Optional<Photo> findById(UUID id);
    List<Photo> findByUserId(UUID userId);
    List<Photo> findByTag(String tag);
}
```

### Good: JPA Implementation (Adapter)

```java
package com.rapidphoto.uploader.infrastructure.persistence;

@Repository
class JpaPhotoRepository implements PhotoRepository {
    
    private final SpringDataPhotoRepository springRepo;
    
    @Override
    public Photo save(Photo photo) {
        PhotoEntity entity = toEntity(photo);
        PhotoEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }
    
    // JPA-specific code isolated here
}
```

### Bad: Domain Entity with Framework Dependency ❌

```java
package com.rapidphoto.uploader.domain.model;

@Entity // ❌ JPA annotation in domain!
@Table(name = "photos") // ❌ Infrastructure concern!
public class Photo {
    @Id // ❌ Framework dependency!
    private UUID id;
    
    // Domain logic mixed with JPA concerns
}
```

---

## Compliance

All new code must follow these rules:

1. **Domain layer** must have zero dependencies on `infrastructure` or `api` layers
2. **Repository interfaces** defined in `domain.repository` package
3. **Repository implementations** in `infrastructure.persistence` package
4. **JPA entities** separate from domain entities (use mappers)
5. **Controllers** in `api` layer, never directly call repositories

---

## References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) by Alistair Cockburn
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html) by Eric Evans
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) by Robert C. Martin

---

**Next**: [ADR 002: CQRS Pattern](./002-cqrs-pattern.md)

