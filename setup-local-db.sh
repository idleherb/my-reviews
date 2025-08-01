#!/bin/bash

# Setup script for local PostgreSQL database

echo "Setting up MyReviews database..."

# Check if PostgreSQL is running
if ! pg_isready -q; then
    echo "PostgreSQL is not running. Please start it first:"
    echo "  brew services start postgresql (macOS)"
    echo "  or use Docker: docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15-alpine"
    exit 1
fi

# Create database
echo "Creating database..."
createdb -U postgres myreviews 2>/dev/null || echo "Database already exists"

# Run schema
echo "Running schema..."
psql -U postgres -d myreviews -f server/db/schema.sql

echo "Database setup complete!"
echo ""
echo "You can now start the server:"
echo "  cd server"
echo "  npm start"