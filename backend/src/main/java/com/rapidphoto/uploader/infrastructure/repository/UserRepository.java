package com.rapidphoto.uploader.infrastructure.repository;

import com.rapidphoto.uploader.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for User entity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find a user by email address.
     * @param email the user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user exists with the given email.
     * @param email the email to check
     * @return true if user exists
     */
    boolean existsByEmail(String email);
}

