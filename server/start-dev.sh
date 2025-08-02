#!/bin/bash
# Start the development server with nodemon
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=myreviews
export DB_USER=myreviews_user
export DB_PASSWORD=postgres
export PORT=3000

npx nodemon index.js