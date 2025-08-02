-- Add isDeleted column to reviews table
ALTER TABLE reviews 
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for non-deleted reviews (most common query)
CREATE INDEX idx_reviews_not_deleted ON reviews(is_deleted) WHERE is_deleted = FALSE;

-- Add reaction_counts column as JSONB
ALTER TABLE reviews
ADD COLUMN reaction_counts JSONB DEFAULT '{}';