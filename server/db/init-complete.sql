-- Complete database initialization
-- This file combines schema and all migrations

-- Base schema
CREATE TABLE IF NOT EXISTS users (
  user_id VARCHAR(36) PRIMARY KEY,
  user_name VARCHAR(255) NOT NULL DEFAULT 'Anonym',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
  id SERIAL PRIMARY KEY,
  restaurant_id BIGINT NOT NULL,
  restaurant_name VARCHAR(255) NOT NULL,
  restaurant_lat DOUBLE PRECISION NOT NULL,
  restaurant_lon DOUBLE PRECISION NOT NULL,
  restaurant_address TEXT,
  rating DECIMAL(2,1) NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  visit_date DATE NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_restaurant_id ON reviews(restaurant_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);

CREATE TABLE IF NOT EXISTS sync_metadata (
  id SERIAL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  last_sync TIMESTAMP NOT NULL DEFAULT NOW(),
  sync_type VARCHAR(50) NOT NULL,
  changes_count INT NOT NULL DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Migrations
-- Add is_deleted column if not exists
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_reviews_not_deleted ON reviews(is_deleted) WHERE is_deleted = FALSE;

-- Add reaction_counts column
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reaction_counts JSONB DEFAULT '{}';

-- Add avatar_url to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- Create reactions table
CREATE TABLE IF NOT EXISTS reactions (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  emoji VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  UNIQUE(review_id, user_id)
);