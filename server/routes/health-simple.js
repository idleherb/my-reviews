const express = require('express');
const router = express.Router();

// Simple health check endpoint without database
router.get('/', async (req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    message: 'Server is running'
  });
});

module.exports = router;