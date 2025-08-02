-- Migration to convert review IDs from SERIAL to UUID

-- Add is_deleted column for tombstone sync first
ALTER TABLE reviews ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for is_deleted
CREATE INDEX idx_reviews_is_deleted ON reviews(is_deleted);

-- Add new UUID column
ALTER TABLE reviews ADD COLUMN id_new VARCHAR(36);

-- Generate UUIDs for existing reviews (for migration only)
UPDATE reviews SET id_new = gen_random_uuid()::text WHERE id_new IS NULL;

-- Make new column NOT NULL
ALTER TABLE reviews ALTER COLUMN id_new SET NOT NULL;

-- Drop the old id column (this will also drop the primary key constraint)
ALTER TABLE reviews DROP CONSTRAINT reviews_pkey;
ALTER TABLE reviews DROP COLUMN id;

-- Rename new column to id
ALTER TABLE reviews RENAME COLUMN id_new TO id;

-- Add primary key constraint
ALTER TABLE reviews ADD PRIMARY KEY (id);