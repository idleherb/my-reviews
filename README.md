# My Reviews - Restaurant Review App

Eine vollständig Open-Source Android-App für persönliche Restaurantbewertungen - ohne Google-Dienste, ohne Tracking, ohne Kompromisse bei der Privatsphäre.

## Features

### Kernfunktionen
- **Kartenbasierte Restaurantsuche** - Finde Restaurants in deiner Nähe mit OpenStreetMap
- **Persönliche Bewertungen** - Bewerte Restaurants mit Sternen und Kommentaren
- **Cloud-Synchronisation** - Optional: Synchronisiere zwischen Geräten mit eigenem Server
- **Offline-First** - Funktioniert komplett ohne Internet
- **Automatische Synchronisation** - Änderungen werden automatisch im Hintergrund synchronisiert
- **Multi-User Support** - Mehrere Nutzer können die gleiche Server-Instanz teilen
- **Profilbilder** - Personalisiere dein Profil mit Avatar-Upload
- **Emoji-Reaktionen** - Reagiere auf Bewertungen anderer Nutzer

### Privacy & Sicherheit
- **Keine Google-Dienste** - Komplett unabhängig von Google Play Services
- **Kein Tracking** - Keine Analytics, keine Werbung, keine Datensammlung
- **Selbst-gehostet** - Volle Kontrolle über deine Daten
- **Volle Datenkontrolle** - Nur du kannst deine Bewertungen löschen oder ändern
- **Privacy by Default** - Starte anonym, teile nur was du möchtest

## Screenshots

<details>
<summary>App-Screenshots anzeigen</summary>

| Kartenansicht | Bewertungen | Einstellungen |
|:-------------:|:-----------:|:-------------:|
| ![Map](docs/screenshots/map.png) | ![Reviews](docs/screenshots/reviews.png) | ![Settings](docs/screenshots/settings.png) |

</details>

## Technologie-Stack

### Android App
- **Sprache**: Kotlin
- **UI**: Material Design 3
- **Datenbank**: Room (SQLite)
- **Karten**: OSMDroid (OpenStreetMap)
- **Architektur**: MVVM mit Repository Pattern
- **Dependency Injection**: Eigenes DI-System

### Backend (Optional)
- **API**: Node.js mit Express
- **Datenbank**: PostgreSQL
- **Container**: Docker & Docker Compose
- **File Storage**: Avatar-Uploads

## Installation

### Option 1: APK installieren (Einfachste Methode)
1. Lade die neueste APK von den [Releases](https://github.com/idleherb/my-reviews/releases) herunter
2. Erlaube "Installation aus unbekannten Quellen" auf deinem Android-Gerät
3. Installiere die APK

### Option 2: Selbst bauen
```bash
# Repository klonen
git clone https://github.com/idleherb/my-reviews.git
cd my-reviews

# Android App bauen
./gradlew assembleDebug

# APK finden unter:
# app/build/outputs/apk/debug/app-debug.apk
```

### Backend-Server (Optional für Cloud-Sync)
```bash
# Repository klonen (falls noch nicht geschehen)
git clone https://github.com/idleherb/my-reviews.git
cd my-reviews

# Umgebungsvariablen konfigurieren
cp .env.example .env
# Bearbeite .env mit deinem Editor

# Mit Docker starten
docker-compose up -d

# Server läuft auf http://localhost:3000
```

Detaillierte Anleitungen:
- [Android Build-Anleitung](docs/BUILD.md)
- [Server Deployment Guide](docs/DEPLOYMENT.md)
- [TrueNAS Installation](docs/TRUENAS.md)

## Verwendung

1. **App starten** - Die App funktioniert sofort ohne Konfiguration
2. **Restaurant suchen** - Nutze die Karte oder Suchfunktion
3. **Bewertung hinzufügen** - Tippe auf ein Restaurant und bewerte es
4. **Cloud-Sync aktivieren** (Optional):
   - Gehe zu Einstellungen
   - Aktiviere Cloud-Synchronisation
   - Gib deine Server-URL ein
   - Wähle einen Benutzernamen

## Beitragen

Beiträge sind willkommen! Egal ob Bug-Reports, Feature-Requests oder Code-Beiträge.

1. Fork das Repository
2. Erstelle einen Feature-Branch (`git checkout -b feature/AmazingFeature`)
3. Committe deine Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. Push zum Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen Pull Request

Siehe auch [CONTRIBUTING.md](CONTRIBUTING.md) für Details.

## Lizenz

Dieses Projekt ist unter der **Unlicense** veröffentlicht - siehe [LICENSE](LICENSE) Datei.

Das bedeutet: Diese Software ist gemeinfrei (Public Domain). Du kannst sie verwenden, kopieren, modifizieren und verteilen wie du möchtest, ohne jegliche Einschränkungen.

## Danksagungen

- [OpenStreetMap](https://www.openstreetmap.org/) für die fantastischen offenen Kartendaten
- [OSMDroid](https://github.com/osmdroid/osmdroid) für die Android-Kartenintegration
- Allen Contributors und Nutzern des Projekts

## Kontakt

Projekt-Link: [https://github.com/idleherb/my-reviews](https://github.com/idleherb/my-reviews)

