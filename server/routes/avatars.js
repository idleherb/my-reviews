const express = require('express');
const router = express.Router();
const pool = require('../db/pool');
const multer = require('multer');
const path = require('path');
const fs = require('fs').promises;
const crypto = require('crypto');

// Ensure avatars directory exists
const AVATARS_DIR = path.join(__dirname, '..', 'uploads', 'avatars');
fs.mkdir(AVATARS_DIR, { recursive: true }).catch(err => {
  // Only log if it's not a permission error - the directory might already exist
  if (err.code !== 'EACCES' && err.code !== 'EEXIST') {
    console.error('Error creating avatars directory:', err);
  }
});

// Configure multer for avatar uploads
const storage = multer.diskStorage({
  destination: async (req, file, cb) => {
    cb(null, AVATARS_DIR);
  },
  filename: (req, file, cb) => {
    const { userId } = req.params;
    const ext = path.extname(file.originalname);
    // Use userId + timestamp to avoid caching issues
    const filename = `${userId}-${Date.now()}${ext}`;
    cb(null, filename);
  }
});

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB max
  },
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Only image files are allowed'));
    }
  }
});

// Upload avatar
router.post('/:userId', upload.single('avatar'), async (req, res) => {
  try {
    const { userId } = req.params;
    
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }
    
    // Delete old avatar if exists
    const userResult = await pool.query(
      'SELECT avatar_url FROM users WHERE user_id = $1',
      [userId]
    );
    
    if (userResult.rows.length > 0 && userResult.rows[0].avatar_url) {
      const oldPath = path.join(__dirname, '..', userResult.rows[0].avatar_url);
      await fs.unlink(oldPath).catch(() => {}); // Ignore if file doesn't exist
    }
    
    // Update user with new avatar
    const avatarUrl = `/uploads/avatars/${req.file.filename}`;
    const result = await pool.query(
      `UPDATE users 
       SET avatar_url = $1, updated_at = NOW()
       WHERE user_id = $2
       RETURNING user_id, user_name, avatar_url, updated_at`,
      [avatarUrl, userId]
    );
    
    if (result.rows.length === 0) {
      // Delete uploaded file if user not found
      await fs.unlink(req.file.path);
      return res.status(404).json({ error: 'User not found' });
    }
    
    res.json({
      avatarUrl: avatarUrl,
      message: 'Avatar uploaded successfully'
    });
  } catch (error) {
    console.error('Error uploading avatar:', error);
    if (req.file) {
      await fs.unlink(req.file.path).catch(() => {});
    }
    res.status(500).json({ error: 'Failed to upload avatar' });
  }
});

// Delete avatar
router.delete('/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    // Get current avatar
    const userResult = await pool.query(
      'SELECT avatar_url FROM users WHERE user_id = $1',
      [userId]
    );
    
    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    // Delete file if exists
    if (userResult.rows[0].avatar_url) {
      const filePath = path.join(__dirname, '..', userResult.rows[0].avatar_url);
      await fs.unlink(filePath).catch(() => {}); // Ignore if file doesn't exist
    }
    
    // Update database
    await pool.query(
      'UPDATE users SET avatar_url = NULL, updated_at = NOW() WHERE user_id = $1',
      [userId]
    );
    
    res.json({ message: 'Avatar deleted successfully' });
  } catch (error) {
    console.error('Error deleting avatar:', error);
    res.status(500).json({ error: 'Failed to delete avatar' });
  }
});

module.exports = router;