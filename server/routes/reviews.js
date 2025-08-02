const express = require('express');
const router = express.Router();
const pool = require('../db/pool');

// Get all reviews (for sync)
router.get('/', async (req, res) => {
  try {
    const { since } = req.query; // Optional timestamp for incremental sync
    
    let query = `
      SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
             restaurant_address, rating, comment, visit_date, user_id, user_name,
             created_at, updated_at, reaction_counts
      FROM reviews
      WHERE is_deleted = false
    `;
    
    const params = [];
    
    if (since) {
      query += ' AND updated_at > $1';
      params.push(since);
    }
    
    query += ' ORDER BY visit_date DESC';
    
    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching all reviews:', error);
    res.status(500).json({ error: 'Failed to fetch reviews' });
  }
});

// Sync endpoint - respects client timestamps
router.post('/sync/pull', async (req, res) => {
  try {
    const { localReviews } = req.body; // Array of {id, restaurantId, userId, updatedAt}
    
    // Get all non-deleted reviews from server
    const allReviews = await pool.query(
      `SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
              restaurant_address, rating, comment, visit_date, user_id, user_name,
              created_at, updated_at, reaction_counts
       FROM reviews
       WHERE is_deleted = false
       ORDER BY visit_date DESC`
    );
    
    // Filter out reviews where local version is newer
    const reviewsToSend = allReviews.rows.filter(serverReview => {
      const localReview = localReviews?.find(local => 
        local.restaurantId === serverReview.restaurant_id && 
        local.userId === serverReview.user_id
      );
      
      if (!localReview) {
        // Review doesn't exist locally - send it
        return true;
      }
      
      // Compare timestamps - only send if server is newer
      const serverTime = new Date(serverReview.updated_at).getTime();
      const localTime = new Date(localReview.updatedAt).getTime();
      
      return serverTime > localTime;
    });
    
    res.json(reviewsToSend);
  } catch (error) {
    console.error('Error in sync/pull:', error);
    res.status(500).json({ error: 'Failed to sync reviews' });
  }
});

// Get all reviews for a user
router.get('/user/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const { since } = req.query; // Optional timestamp for incremental sync
    
    let query = `
      SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
             restaurant_address, rating, comment, visit_date, user_id, user_name,
             created_at, updated_at, reaction_counts
      FROM reviews
      WHERE user_id = $1 AND is_deleted = false
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
              created_at, updated_at, reaction_counts
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
      userName,
      createdAt,
      updatedAt
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
        restaurant_address, rating, comment, visit_date, user_id, user_name,
        created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 
               COALESCE($11, NOW()), COALESCE($12, NOW()))
       RETURNING *`,
      [restaurantId, restaurantName, restaurantLat, restaurantLon, 
       restaurantAddress, rating, comment, visitDate, userId, userName,
       createdAt, updatedAt]
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

// Update review (with ownership check)
router.put('/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    const { rating, comment, visitDate, userId, updatedAt } = req.body;
    
    if (!rating || !visitDate || !userId) {
      return res.status(400).json({ error: 'Rating, visitDate and userId are required' });
    }
    
    if (rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Rating must be between 1 and 5' });
    }
    
    // Check ownership and get current version
    const currentReview = await pool.query(
      'SELECT user_id, updated_at FROM reviews WHERE id = $1',
      [reviewId]
    );
    
    if (currentReview.rows.length === 0) {
      return res.status(404).json({ error: 'Review not found' });
    }
    
    if (currentReview.rows[0].user_id !== userId) {
      return res.status(403).json({ error: 'Not authorized to update this review' });
    }
    
    // Check if client version is newer or equal
    if (updatedAt) {
      const clientUpdatedAt = new Date(updatedAt);
      const serverUpdatedAt = new Date(currentReview.rows[0].updated_at);
      
      if (clientUpdatedAt < serverUpdatedAt) {
        // Server version is newer - reject update
        return res.status(409).json({ 
          error: 'Conflict: Server version is newer',
          serverVersion: currentReview.rows[0]
        });
      }
    }
    
    const result = await pool.query(
      `UPDATE reviews 
       SET rating = $1, comment = $2, visit_date = $3, updated_at = NOW()
       WHERE id = $4
       RETURNING *`,
      [rating, comment || '', visitDate, reviewId]
    );
    
    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error updating review:', error);
    res.status(500).json({ error: 'Failed to update review' });
  }
});

// Delete review (with ownership check)
router.delete('/:reviewId', async (req, res) => {
  try {
    const { reviewId } = req.params;
    const { userId } = req.query; // Pass userId as query parameter
    
    if (!userId) {
      return res.status(400).json({ error: 'userId is required' });
    }
    
    // Check ownership
    const ownershipCheck = await pool.query(
      'SELECT user_id FROM reviews WHERE id = $1',
      [reviewId]
    );
    
    if (ownershipCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Review not found' });
    }
    
    if (ownershipCheck.rows[0].user_id !== userId) {
      return res.status(403).json({ error: 'Not authorized to delete this review' });
    }
    
    // Soft delete - nur als gelöscht markieren
    const result = await pool.query(
      'UPDATE reviews SET is_deleted = true, updated_at = NOW() WHERE id = $1 RETURNING id',
      [reviewId]
    );
    
    res.status(204).send();
  } catch (error) {
    console.error('Error deleting review:', error);
    res.status(500).json({ error: 'Failed to delete review' });
  }
});

// Simple bulk sync endpoint
router.post('/sync', async (req, res) => {
  const client = await pool.connect();
  
  try {
    const { userId, reviews } = req.body;
    
    if (!userId || !reviews || !Array.isArray(reviews)) {
      return res.status(400).json({ error: 'Invalid sync request' });
    }
    
    await client.query('BEGIN');
    
    let processedCount = 0;
    
    console.log(`Sync request from user ${userId} with ${reviews.length} reviews`);
    
    // Process each review from client
    for (const review of reviews) {
      console.log(`Processing review: id=${review.id}, restaurant=${review.restaurantName}, userId=${review.userId}, isDeleted=${review.isDeleted}`);
      
      if (review.isDeleted && review.id > 0) {
        // Soft delete on server
        await client.query(
          'UPDATE reviews SET is_deleted = true, updated_at = NOW() WHERE id = $1',
          [review.id]
        );
        console.log(`Marked review ${review.id} as deleted`);
      } else {
        // Check if review exists (by restaurant_id and user_id)
        const existing = await client.query(
          'SELECT id FROM reviews WHERE restaurant_id = $1 AND user_id = $2',
          [review.restaurantId, review.userId]
        );
        
        if (existing.rows.length > 0) {
          // Update existing review
          const existingId = existing.rows[0].id;
          console.log(`Updating existing review ${existingId} for ${review.restaurantName}`);
          
          await client.query(
            `UPDATE reviews 
             SET rating = $1, comment = $2, visit_date = $3, 
                 restaurant_name = $4, restaurant_lat = $5, 
                 restaurant_lon = $6, restaurant_address = $7,
                 user_name = $8, updated_at = $9
             WHERE id = $10`,
            [review.rating, review.comment, review.visitDate,
             review.restaurantName, review.restaurantLat,
             review.restaurantLon, review.restaurantAddress,
             review.userName, review.updatedAt || new Date(), existingId]
          );
        } else {
          // Insert new review
          console.log(`Inserting new review for ${review.restaurantName} by user ${review.userId}`);
          await client.query(
            `INSERT INTO reviews 
             (restaurant_id, restaurant_name, restaurant_lat, restaurant_lon, 
              restaurant_address, rating, comment, visit_date, user_id, user_name,
              created_at, updated_at)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
            [review.restaurantId, review.restaurantName, review.restaurantLat,
             review.restaurantLon, review.restaurantAddress, review.rating,
             review.comment, review.visitDate, review.userId, review.userName,
             review.createdAt || new Date(), review.updatedAt || new Date()]
          );
        }
      }
      processedCount++;
    }
    
    await client.query('COMMIT');
    
    // Return ALL reviews from database (not deleted)
    const allReviews = await pool.query(
      `SELECT id, restaurant_id, restaurant_name, restaurant_lat, restaurant_lon,
              restaurant_address, rating, comment, visit_date, user_id, user_name,
              created_at, updated_at, reaction_counts
       FROM reviews
       WHERE is_deleted = false
       ORDER BY visit_date DESC`
    );
    
    console.log(`Returning ${allReviews.rows.length} reviews to client`);
    
    res.json({ 
      processed: processedCount,
      allReviews: allReviews.rows 
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error syncing reviews:', error);
    res.status(500).json({ error: 'Failed to sync reviews' });
  } finally {
    client.release();
  }
});

module.exports = router;