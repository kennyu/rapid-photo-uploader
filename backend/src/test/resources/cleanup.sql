-- Cleanup script for integration tests
-- Ensures clean state before each test

DELETE FROM photo_tags;
DELETE FROM photos;
DELETE FROM users;
DELETE FROM upload_jobs;

-- Reset sequences
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS photos_id_seq RESTART WITH 1;

