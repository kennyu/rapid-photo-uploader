-- Create upload_jobs table
CREATE TABLE upload_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_upload_jobs_photo FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE,
    CONSTRAINT fk_upload_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_upload_jobs_photo_id ON upload_jobs(photo_id);
CREATE INDEX idx_upload_jobs_user_id ON upload_jobs(user_id);
CREATE INDEX idx_upload_jobs_status ON upload_jobs(status);
CREATE INDEX idx_upload_jobs_created_at ON upload_jobs(created_at);

-- Add comment
COMMENT ON TABLE upload_jobs IS 'Tracks upload job status and retry attempts';

