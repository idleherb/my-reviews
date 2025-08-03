-- Migration to fix sync issues on TrueNAS
-- Run this once to add missing reactions table

-- Create reactions table if it doesn't exist
CREATE TABLE IF NOT EXISTS reactions (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  reaction_type VARCHAR(50) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  UNIQUE(review_id, user_id, reaction_type)
);

-- Verify the schema
\d reactions