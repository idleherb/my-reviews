const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*'
}));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Import routes
const healthRoutes = require('./routes/health-simple'); // Using simple health check for now
const userRoutes = require('./routes/users');
const reviewRoutes = require('./routes/reviews');
const reactionRoutes = require('./routes/reactions');
const avatarRoutes = require('./routes/avatars');

// Serve static files for avatars
app.use('/uploads', express.static('uploads'));

// Use routes
app.use('/api/health', healthRoutes);
app.use('/api/users', userRoutes);
app.use('/api/reviews', reviewRoutes);
app.use('/api/reactions', reactionRoutes);
app.use('/api/avatars', avatarRoutes);

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ 
    error: 'Something went wrong!',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// Check database connection before starting
const checkDatabaseConnection = require('./db/check-connection');

async function startServer() {
  const dbConnected = await checkDatabaseConnection();
  
  if (!dbConnected) {
    console.error('Cannot start server without database connection');
    process.exit(1);
  }
  
  // Start server
  app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
    console.log(`Environment: ${process.env.NODE_ENV}`);
  });
}

startServer();

module.exports = app;