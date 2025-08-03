# MyReviews Deployment Guide

## Prerequisites

- Docker and Docker Compose installed
- Git installed
- PostgreSQL data will be stored in a Docker volume

## Deployment Steps

### 1. Clone the Repository

```bash
git clone https://github.com/[your-username]/my-reviews.git
cd my-reviews
```

### 2. Create Environment File

```bash
cp .env.example .env
```

Edit `.env` and set your values:
- `DB_PASSWORD` - Set a secure password for PostgreSQL
- Other values can typically remain as defaults

### 3. Deploy with Docker Compose

```bash
docker-compose up -d
```

This will:
- Build the API server from source
- Start PostgreSQL with automatic schema initialization
- Create persistent volumes for data

### 4. Verify Deployment

Check if services are running:
```bash
docker-compose ps
```

Test the API:
```bash
curl http://localhost:3000/api/health
```

### 5. Configure Android App

In the MyReviews Android app:
1. Go to Settings
2. Enable Cloud Sync
3. Enter server URL: `http://[server-ip]:3000`
4. Test connection

## TrueNAS Specific Deployment

For TrueNAS SCALE:

1. Clone repository to a dataset (e.g., `/mnt/[pool]/apps/myreviews`)
2. Create `.env` file with your configuration
3. Create Custom App in TrueNAS:
   - Name: `myreviews`
   - Type: Docker Compose
   - Compose File: Point to `docker-compose.yml`
4. TrueNAS will handle container lifecycle

## Maintenance

### View Logs
```bash
docker-compose logs -f api
docker-compose logs -f postgres
```

### Restart Services
```bash
docker-compose restart
```

### Update Application
```bash
git pull
docker-compose down
docker-compose up -d --build
```

### Backup Database
```bash
docker-compose exec postgres pg_dump -U myreviews_user myreviews > backup.sql
```

## Environment Variables

See `.env.example` for all available configuration options:
- `DB_NAME` - Database name
- `DB_USER` - Database username  
- `DB_PASSWORD` - Database password (required)
- `DB_PORT` - PostgreSQL port
- `API_PORT` - API server port
- `NODE_ENV` - Node environment (production/development)
- `CORS_ORIGIN` - CORS allowed origins

## Data Persistence

- PostgreSQL data: Docker volume `myreviews_postgres_data`
- Uploads: `./server/uploads/` (bind mounted)

## Security Notes

- Always use strong passwords in production
- Consider using HTTPS with a reverse proxy
- Restrict CORS_ORIGIN in production
- Keep `.env` file secure and never commit it