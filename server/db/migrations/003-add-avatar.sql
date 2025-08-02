-- Add avatar field to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- Add metadata for avatar management
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_uploaded_at TIMESTAMP;

-- Example: avatar_url = '/avatars/user-uuid.jpg' or null