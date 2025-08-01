# Local Testing Setup

## 1. Start PostgreSQL locally

If you don't have PostgreSQL installed:
```bash
# macOS with Homebrew
brew install postgresql
brew services start postgresql

# Or use Docker
docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15-alpine
```

## 2. Create database and run schema

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE myreviews;
\q

# Run schema
psql -U postgres -d myreviews -f server/db/schema.sql
```

## 3. Start the Node.js server

```bash
cd server
npm install  # If not done already
npm start
```

The server should start on http://localhost:3000

## 4. Test the health endpoint

```bash
curl http://localhost:3000/api/health
```

Should return:
```json
{
  "status": "healthy",
  "timestamp": "...",
  "database": "connected",
  "dbTime": "..."
}
```

## 5. Configure Android App

1. Open the app in Android Studio
2. Start an Android emulator
3. The app is pre-configured to use `10.0.2.2:3000` (emulator's localhost)
4. Run the app

## 6. Test Cloud Sync

1. Create some restaurant reviews in the app
2. Go to Settings (3-dots menu)
3. Enter a username (optional)
4. Enable "Mit Server synchronisieren"
5. Click "Verbindung testen" - should show success
6. Click "Einstellungen speichern"
7. Click "Jetzt synchronisieren"

## 7. Verify sync worked

```bash
# Check users table
psql -U postgres -d myreviews -c "SELECT * FROM users;"

# Check reviews table  
psql -U postgres -d myreviews -c "SELECT * FROM reviews;"
```

## Troubleshooting

- **Connection refused**: Make sure server is running on port 3000
- **Database error**: Check PostgreSQL is running and database exists
- **Android can't connect**: Ensure you're using 10.0.2.2, not localhost
- **Permission denied**: Check Android manifest has INTERNET permission