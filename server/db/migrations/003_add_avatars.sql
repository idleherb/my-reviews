-- Add avatar support to users table and create avatars table

-- Add avatar_url column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- Create avatars table for storing avatar files
CREATE TABLE IF NOT EXISTS avatars (
  id SERIAL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  filename VARCHAR(255) NOT NULL,
  filepath TEXT NOT NULL,
  mimetype VARCHAR(100) NOT NULL,
  size INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  UNIQUE(user_id) -- One avatar per user
);

-- Create index for avatars
CREATE INDEX IF NOT EXISTS idx_avatars_user_id ON avatars(user_id);