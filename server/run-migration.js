const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// Database configuration
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'myreviews',
  user: process.env.DB_USER || 'myreviews_user',
  password: process.env.DB_PASSWORD || 'postgres'
});

async function runMigration() {
  const client = await pool.connect();
  
  try {
    console.log('Running database migration...');
    
    // Read migration SQLs
    const resetMigrationSQL = fs.readFileSync(
      path.join(__dirname, 'db/reset-and-migrate.sql'), 
      'utf8'
    );
    
    const avatarMigrationSQL = fs.readFileSync(
      path.join(__dirname, 'db/migrations/003_add_avatars.sql'), 
      'utf8'
    );
    
    // Execute migrations
    await client.query(resetMigrationSQL);
    await client.query(avatarMigrationSQL);
    
    console.log('Migration completed successfully!');
    
    // Show current schema
    const result = await client.query(`
      SELECT column_name, data_type, is_nullable 
      FROM information_schema.columns 
      WHERE table_name = 'reviews' 
      AND column_name IN ('id', 'is_deleted')
      ORDER BY column_name
    `);
    
    console.log('\nCurrent schema:');
    result.rows.forEach(row => {
      console.log(`- ${row.column_name}: ${row.data_type} (nullable: ${row.is_nullable})`);
    });
    
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

runMigration();