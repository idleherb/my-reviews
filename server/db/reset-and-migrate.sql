-- Reset database and apply UUID migration

-- First, drop all data
TRUNCATE TABLE reviews, users, sync_metadata CASCADE;

-- Check if migration already applied
DO $$
BEGIN
    -- Check if is_deleted column exists
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'reviews' 
        AND column_name = 'is_deleted'
    ) THEN
        -- Add is_deleted column for tombstone sync
        ALTER TABLE reviews ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
        CREATE INDEX idx_reviews_is_deleted ON reviews(is_deleted);
    END IF;
    
    -- Check if id column is already VARCHAR
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'reviews' 
        AND column_name = 'id'
        AND data_type != 'character varying'
    ) THEN
        -- Add new UUID column
        ALTER TABLE reviews ADD COLUMN id_new VARCHAR(36);
        
        -- Generate UUIDs for existing reviews (should be none after TRUNCATE)
        UPDATE reviews SET id_new = gen_random_uuid()::text WHERE id_new IS NULL;
        
        -- Make new column NOT NULL
        ALTER TABLE reviews ALTER COLUMN id_new SET NOT NULL;
        
        -- Drop foreign key constraints first
        ALTER TABLE reactions DROP CONSTRAINT IF EXISTS reactions_review_id_fkey;
        
        -- Drop the old id column
        ALTER TABLE reviews DROP CONSTRAINT reviews_pkey;
        ALTER TABLE reviews DROP COLUMN id;
        
        -- Rename new column to id
        ALTER TABLE reviews RENAME COLUMN id_new TO id;
        
        -- Add primary key constraint
        ALTER TABLE reviews ADD PRIMARY KEY (id);
        
        -- Update reactions table to use VARCHAR for review_id
        ALTER TABLE reactions ADD COLUMN review_id_new VARCHAR(36);
        UPDATE reactions SET review_id_new = review_id::text;
        ALTER TABLE reactions ALTER COLUMN review_id_new SET NOT NULL;
        ALTER TABLE reactions DROP CONSTRAINT reactions_pkey;
        ALTER TABLE reactions DROP COLUMN review_id;
        ALTER TABLE reactions RENAME COLUMN review_id_new TO review_id;
        ALTER TABLE reactions ADD PRIMARY KEY (review_id, user_id);
        
        -- Re-add foreign key constraint
        ALTER TABLE reactions ADD CONSTRAINT reactions_review_id_fkey 
            FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- Verify the schema
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'reviews' 
AND column_name IN ('id', 'is_deleted')
ORDER BY column_name;