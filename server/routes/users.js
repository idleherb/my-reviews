const express = require('express');
const router = express.Router();
const pool = require('../db/pool');

// Get user by ID
router.get('/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const result = await pool.query(
      'SELECT user_id, user_name, avatar_url, created_at, updated_at FROM users WHERE user_id = $1',
      [userId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error fetching user:', error);
    res.status(500).json({ error: 'Failed to fetch user' });
  }
});

// Create or update user
router.put('/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const { userName } = req.body;
    
    if (!userName) {
      return res.status(400).json({ error: 'userName is required' });
    }
    
    // Upsert user
    const result = await pool.query(
      `INSERT INTO users (user_id, user_name) 
       VALUES ($1, $2) 
       ON CONFLICT (user_id) 
       DO UPDATE SET user_name = $2, updated_at = NOW()
       RETURNING user_id, user_name, created_at, updated_at`,
      [userId, userName]
    );
    
    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error upserting user:', error);
    res.status(500).json({ error: 'Failed to upsert user' });
  }
});

// Get all users (for admin/debugging)
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT user_id, user_name, avatar_url, created_at, updated_at FROM users ORDER BY created_at DESC'
    );
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching users:', error);
    res.status(500).json({ error: 'Failed to fetch users' });
  }
});

module.exports = router;