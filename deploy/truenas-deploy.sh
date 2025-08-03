#!/bin/bash
# TrueNAS SCALE Deployment Helper Script
# This script helps prepare the My Reviews app for TrueNAS deployment

set -e

echo "🚀 My Reviews - TrueNAS SCALE Deployment Helper"
echo "=============================================="

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Configuration
DOCKER_USERNAME=${DOCKER_USERNAME:-""}
IMAGE_NAME="myreviews-api"
IMAGE_TAG="latest"

# Prompt for Docker Hub username if not set
if [ -z "$DOCKER_USERNAME" ]; then
    read -p "Enter your Docker Hub username: " DOCKER_USERNAME
fi

echo ""
echo "📦 Building Docker image..."
cd ../server
docker build -t $DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG .

echo ""
echo "🔐 Logging into Docker Hub..."
docker login

echo ""
echo "⬆️  Pushing image to Docker Hub..."
docker push $DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG

echo ""
echo "✅ Docker image successfully pushed!"
echo ""
echo "📋 Next steps for TrueNAS SCALE deployment:"
echo ""
echo "1. Log into your TrueNAS SCALE web interface"
echo ""
echo "2. Create storage directories on TrueNAS:"
echo "   mkdir -p /mnt/[your-pool]/apps/myreviews/{postgres,uploads,config}"
echo ""
echo "3. Copy files to TrueNAS:"
echo "   - Copy docker-compose.truenas.yml"
echo "   - Copy server/db/schema.sql to /mnt/[your-pool]/apps/myreviews/config/"
echo ""
echo "4. Deploy via Docker Compose:"
echo ""
echo "   Option A - TrueNAS Custom App:"
echo "   - Apps → Discover Apps → Custom App"
echo "   - Name: myreviews"
echo "   - Paste docker-compose.yml content"
echo "   - Set DB_PASSWORD environment variable"
echo ""
echo "   Option B - Via Portainer (recommended):"
echo "   - Install Portainer from TrueNAS catalog"
echo "   - Create new stack with docker-compose.yml"
echo "   - Image: $DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG"
echo ""
echo "5. Database will auto-initialize from schema.sql"
echo ""
echo "📱 Configure Android app:"
echo "   - Server URL: [TrueNAS-IP]"
echo "   - Port: 3000"
echo "   - Test connection in app settings"
echo ""