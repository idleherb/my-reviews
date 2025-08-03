# My Reviews - Restaurant Review App

Eine native Android-App für Restaurantbewertungen ohne Google-Dienste, ausschließlich mit Open Source APIs.

## Features

- 📍 Kartenbasierte Restaurantsuche
- ⭐ Bewertungssystem
- 🗺️ OpenStreetMap Integration
- 📱 Offline-Funktionalität
- 🔒 Privacy-First Ansatz

## Technologie

- **Frontend**: Native Android (Kotlin)
- **Karten**: OpenStreetMap
- **Backend**: REST API (selbst gehostet)
- **Datenbank**: SQLite (lokal) + PostgreSQL (Server)

## Setup

### Android App
1. Open project in Android Studio
2. Build and run on device/emulator
3. Configure server URL in Settings if using cloud sync

### Backend Server
See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions.

Quick start with Docker:
```bash
cp .env.example .env
# Edit .env with your configuration
docker-compose up -d
```

## Lizenz

(Wird noch festgelegt)