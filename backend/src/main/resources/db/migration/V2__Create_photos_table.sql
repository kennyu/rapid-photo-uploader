-- Create photos table
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photos_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create photo_tags table for many-to-many relationship
CREATE TABLE photo_tags (
    photo_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    CONSTRAINT fk_photo_tags_photo FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_status ON photos(status);
CREATE INDEX idx_photos_created_at ON photos(created_at);
CREATE INDEX idx_photo_tags_photo_id ON photo_tags(photo_id);
CREATE INDEX idx_photo_tags_tag ON photo_tags(tag);

-- Add comments
COMMENT ON TABLE photos IS 'Stores photo metadata and upload information';
COMMENT ON TABLE photo_tags IS 'Stores tags associated with photos';

