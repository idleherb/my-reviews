const pool = require('./pool');

async function checkDatabaseConnection() {
  try {
    console.log('Checking database connection...');
    const result = await pool.query('SELECT 1');
    console.log('✓ Database connection successful');
    return true;
  } catch (error) {
    console.error('✗ Database connection failed:', error.message);
    console.error('Make sure PostgreSQL is running and accessible');
    console.error('Connection details:', {
      host: process.env.DB_HOST || 'localhost',
      port: process.env.DB_PORT || 5432,
      database: process.env.DB_NAME || 'myreviews'
    });
    return false;
  }
}

module.exports = checkDatabaseConnection;