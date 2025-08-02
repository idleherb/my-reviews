const express = require('express');
const router = express.Router();
const pool = require('../db/pool');

// Allowed emojis
const ALLOWED_EMOJIS = ['❤️', '👍', '😂', '🤔', '😮'];

// Get reactions for a review
router.get('/review/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    
    // Get all reactions for this review
    const reactionsResult = await pool.query(
      `SELECT r.*, u.user_name 
       FROM reactions r
       JOIN users u ON r.user_id = u.user_id
       WHERE r.review_id = $1
       ORDER BY r.created_at DESC`,
      [reviewId]
    );
    
    // Get reaction counts
    const countsResult = await pool.query(
      `SELECT emoji, COUNT(*) as count 
       FROM reactions 
       WHERE review_id = $1 
       GROUP BY emoji`,
      [reviewId]
    );
    
    // Convert counts to object
    const counts = {};
    countsResult.rows.forEach(row => {
      counts[row.emoji] = parseInt(row.count);
    });
    
    res.json({
      reactions: reactionsResult.rows,
      counts: counts
    });
  } catch (error) {
    console.error('Error fetching reactions:', error);
    res.status(500).json({ error: 'Failed to fetch reactions' });
  }
});

// Add or update reaction
router.post('/review/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    const { userId, emoji } = req.body;
    
    // Validate emoji
    if (!ALLOWED_EMOJIS.includes(emoji)) {
      return res.status(400).json({ error: 'Invalid emoji' });
    }
    
    // Check if review exists
    const reviewCheck = await pool.query(
      'SELECT id FROM reviews WHERE id = $1',
      [reviewId]
    );
    
    if (reviewCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Review not found' });
    }
    
    // Upsert reaction
    const result = await pool.query(
      `INSERT INTO reactions (review_id, user_id, emoji) 
       VALUES ($1, $2, $3) 
       ON CONFLICT (review_id, user_id) 
       DO UPDATE SET emoji = $3, created_at = NOW()
       RETURNING *`,
      [reviewId, userId, emoji]
    );
    
    // Update reaction counts in reviews table
    await updateReactionCounts(reviewId);
    
    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error adding reaction:', error);
    res.status(500).json({ error: 'Failed to add reaction' });
  }
});

// Remove reaction
router.delete('/review/:reviewId/user/:userId', async (req, res) => {
  try {
    const { reviewId, userId } = req.params;
    
    const result = await pool.query(
      'DELETE FROM reactions WHERE review_id = $1 AND user_id = $2 RETURNING *',
      [reviewId, userId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Reaction not found' });
    }
    
    // Update reaction counts in reviews table
    await updateReactionCounts(reviewId);
    
    res.json({ message: 'Reaction removed' });
  } catch (error) {
    console.error('Error removing reaction:', error);
    res.status(500).json({ error: 'Failed to remove reaction' });
  }
});

// Get user's reactions
router.get('/user/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const result = await pool.query(
      `SELECT r.*, rev.restaurant_name 
       FROM reactions r
       JOIN reviews rev ON r.review_id = rev.id
       WHERE r.user_id = $1
       ORDER BY r.created_at DESC`,
      [userId]
    );
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching user reactions:', error);
    res.status(500).json({ error: 'Failed to fetch user reactions' });
  }
});

// Helper function to update reaction counts
async function updateReactionCounts(reviewId) {
  const countsResult = await pool.query(
    `SELECT emoji, COUNT(*) as count 
     FROM reactions 
     WHERE review_id = $1 
     GROUP BY emoji`,
    [reviewId]
  );
  
  const counts = {};
  countsResult.rows.forEach(row => {
    counts[row.emoji] = parseInt(row.count);
  });
  
  await pool.query(
    'UPDATE reviews SET reaction_counts = $1 WHERE id = $2',
    [JSON.stringify(counts), reviewId]
  );
}

module.exports = router;