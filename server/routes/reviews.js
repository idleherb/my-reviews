const express = require('express');
const router = express.Router();
const pool = require('../db/pool');

// Get all reviews for a user
router.get('/user/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const { since } = req.query; // Optional timestamp for incremental sync
    
    let query = `
      SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
             restaurant_address, rating, comment, visit_date, user_id, user_name,
             created_at, updated_at
      FROM reviews
      WHERE user_id = $1
    `;
    
    const params = [userId];
    
    if (since) {
      query += ' AND updated_at > $2';
      params.push(since);
    }
    
    query += ' ORDER BY visit_date DESC';
    
    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching user reviews:', error);
    res.status(500).json({ error: 'Failed to fetch reviews' });
  }
});

// Get reviews for a restaurant
router.get('/restaurant/:restaurantId', async (req, res) => {
  try {
    const { restaurantId } = req.params;
    
    const result = await pool.query(
      `SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
              restaurant_address, rating, comment, visit_date, user_id, user_name,
              created_at, updated_at
       FROM reviews
       WHERE restaurant_id = $1
       ORDER BY visit_date DESC`,
      [restaurantId]
    );
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching restaurant reviews:', error);
    res.status(500).json({ error: 'Failed to fetch reviews' });
  }
});

// Create new review
router.post('/', async (req, res) => {
  const client = await pool.connect();
  
  try {
    const {
      restaurantId,
      restaurantName,
      restaurantLat,
      restaurantLon,
      restaurantAddress,
      rating,
      comment,
      visitDate,
      userId,
      userName
    } = req.body;
    
    // Validate required fields
    if (!restaurantId || !restaurantName || !restaurantLat || !restaurantLon || 
        !rating || !visitDate || !userId || !userName) {
      return res.status(400).json({ error: 'Missing required fields' });
    }
    
    // Validate rating range
    if (rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Rating must be between 1 and 5' });
    }
    
    await client.query('BEGIN');
    
    // Ensure user exists
    await client.query(
      `INSERT INTO users (user_id, user_name) 
       VALUES ($1, $2) 
       ON CONFLICT (user_id) DO NOTHING`,
      [userId, userName]
    );
    
    // Insert review
    const result = await client.query(
      `INSERT INTO reviews 
       (restaurant_id, restaurant_name, restaurant_lat, restaurant_lon, 
        restaurant_address, rating, comment, visit_date, user_id, user_name)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [restaurantId, restaurantName, restaurantLat, restaurantLon, 
       restaurantAddress, rating, comment, visitDate, userId, userName]
    );
    
    await client.query('COMMIT');
    res.status(201).json(result.rows[0]);
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error creating review:', error);
    res.status(500).json({ error: 'Failed to create review' });
  } finally {
    client.release();
  }
});

// Update review
router.put('/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    const { rating, comment, visitDate } = req.body;
    
    if (!rating || !visitDate) {
      return res.status(400).json({ error: 'Rating and visitDate are required' });
    }
    
    if (rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Rating must be between 1 and 5' });
    }
    
    const result = await pool.query(
      `UPDATE reviews 
       SET rating = $1, comment = $2, visit_date = $3
       WHERE id = $4
       RETURNING *`,
      [rating, comment || '', visitDate, reviewId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Review not found' });
    }
    
    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error updating review:', error);
    res.status(500).json({ error: 'Failed to update review' });
  }
});

// Delete review
router.delete('/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    
    const result = await pool.query(
      'DELETE FROM reviews WHERE id = $1 RETURNING id',
      [reviewId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Review not found' });
    }
    
    res.status(204).send();
  } catch (error) {
    console.error('Error deleting review:', error);
    res.status(500).json({ error: 'Failed to delete review' });
  }
});

// Bulk sync endpoint for multiple reviews
router.post('/sync', async (req, res) => {
  const client = await pool.connect();
  
  try {
    const { userId, reviews } = req.body;
    
    if (!userId || !reviews || !Array.isArray(reviews)) {
      return res.status(400).json({ error: 'Invalid sync request' });
    }
    
    await client.query('BEGIN');
    
    const results = [];
    
    for (const review of reviews) {
      // Check if review exists (by restaurant_id and user_id)
      const existing = await client.query(
        'SELECT id FROM reviews WHERE restaurant_id = $1 AND user_id = $2',
        [review.restaurantId, userId]
      );
      
      if (existing.rows.length > 0) {
        // Update existing review
        const result = await client.query(
          `UPDATE reviews 
           SET rating = $1, comment = $2, visit_date = $3, 
               restaurant_name = $4, restaurant_lat = $5, 
               restaurant_lon = $6, restaurant_address = $7,
               user_name = $8
           WHERE id = $9
           RETURNING *`,
          [review.rating, review.comment, review.visitDate,
           review.restaurantName, review.restaurantLat,
           review.restaurantLon, review.restaurantAddress,
           review.userName, existing.rows[0].id]
        );
        results.push(result.rows[0]);
      } else {
        // Insert new review
        const result = await client.query(
          `INSERT INTO reviews 
           (restaurant_id, restaurant_name, restaurant_lat, restaurant_lon, 
            restaurant_address, rating, comment, visit_date, user_id, user_name)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
           RETURNING *`,
          [review.restaurantId, review.restaurantName, review.restaurantLat,
           review.restaurantLon, review.restaurantAddress, review.rating,
           review.comment, review.visitDate, userId, review.userName]
        );
        results.push(result.rows[0]);
      }
    }
    
    // Update sync metadata
    await client.query(
      `INSERT INTO sync_metadata (user_id, sync_type, changes_count)
       VALUES ($1, 'upload', $2)`,
      [userId, reviews.length]
    );
    
    await client.query('COMMIT');
    res.json({ synced: results.length, reviews: results });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error syncing reviews:', error);
    res.status(500).json({ error: 'Failed to sync reviews' });
  } finally {
    client.release();
  }
});

module.exports = router;