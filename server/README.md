# MyReviews Server

REST API server for the MyReviews Android app.

## Setup

1. Install PostgreSQL
2. Create database and user:
   ```sql
   CREATE DATABASE myreviews;
   CREATE USER myreviews_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE myreviews TO myreviews_user;
   ```

3. Run schema:
   ```bash
   psql -U myreviews_user -d myreviews -f db/schema.sql
   ```

4. Create `.env` file (copy from `.env.example`)

5. Install dependencies:
   ```bash
   npm install
   ```

6. Start server:
   ```bash
   npm start
   # or for development with auto-reload
   npm run dev
   ```

## API Endpoints

### Health Check
- `GET /api/health` - Server and database health status

### Users
- `GET /api/users` - List all users
- `GET /api/users/:userId` - Get user details
- `PUT /api/users/:userId` - Create or update user

### Reviews
- `GET /api/reviews/user/:userId` - Get all reviews for a user
- `GET /api/reviews/restaurant/:restaurantId` - Get all reviews for a restaurant
- `POST /api/reviews` - Create new review
- `PUT /api/reviews/:reviewId` - Update review
- `DELETE /api/reviews/:reviewId` - Delete review
- `POST /api/reviews/sync` - Bulk sync reviews

## Environment Variables

- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name (default: myreviews)
- `DB_USER` - Database user
- `DB_PASSWORD` - Database password
- `PORT` - Server port (default: 3000)
- `NODE_ENV` - Environment (development/production)
- `CORS_ORIGIN` - CORS origin (default: *)

## Docker Deployment

See `docker-compose.yml` for containerized deployment.