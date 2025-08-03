-- Migration to fix sync issues on TrueNAS
-- Run this once to add missing reactions table

-- Drop old reactions table if it exists with wrong schema
DROP TABLE IF EXISTS reactions;

-- Create reactions table with correct schema
CREATE TABLE reactions (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  emoji VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  UNIQUE(review_id, user_id)
);

-- Verify the schema
\d reactions