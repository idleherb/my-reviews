-- Add reactions table for emoji reactions on reviews
CREATE TABLE IF NOT EXISTS reactions (
  id SERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  emoji VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  -- Each user can only have one reaction per review
  UNIQUE(review_id, user_id),
  
  -- Foreign keys
  FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Create indexes for performance
CREATE INDEX idx_reactions_review ON reactions(review_id);
CREATE INDEX idx_reactions_user ON reactions(user_id);

-- Add reaction counts to reviews table for performance (denormalized)
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reaction_counts JSONB DEFAULT '{}';

-- Example: reaction_counts = {"❤️": 5, "👍": 3, "😂": 2}