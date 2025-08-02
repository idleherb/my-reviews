# Restaurant Reviews App - Projektübersicht

## Projektziel
Entwicklung einer nativen Android-App für Restaurantbewertungen ohne Google-Dienste, ausschließlich mit Open Source APIs.

## Hauptfunktionen
- **Kartenansicht**: Anzeige von Restaurants basierend auf aktuellem Standort
- **Bewertungssystem**: Nutzer können Restaurants bewerten
- **Lokale Speicherung**: Bewertungen werden in Datenbank/Dateien gespeichert
- **Cloud-Hosting**: Backend läuft auf Home-Server

## Technologie-Stack
- **Android App**: Kotlin, Gradle
- **Karten**: OpenStreetMap (OSM) / Leaflet
- **Standortdienste**: Ohne Google Play Services
- **Backend**: REST API (Node.js/Python)
- **Datenbank**: SQLite (lokal) + PostgreSQL (Server)
- **Hosting**: Docker auf Home-Server

## Open Source APIs
- **OpenStreetMap**: Für Kartendaten
- **Nominatim**: Für Geocoding/Reverse Geocoding
- **Overpass API**: Für POI-Daten (Restaurants)

## Todo-Liste

### High Priority
1. **Android-Projekt mit Kotlin und Gradle initialisieren**
   - Grundstruktur erstellen
   - Dependencies konfigurieren
   
2. **OpenStreetMap/Leaflet für Kartenansicht einrichten**
   - OSMDroid oder MapLibre SDK integrieren
   - Offline-Karten Option
   
3. **Location-Services ohne Google implementieren**
   - Android Location API direkt nutzen
   - GPS/Network Provider

### Medium Priority
4. **Datenmodell für Restaurantbewertungen erstellen**
   - Restaurant Entity (ID, Name, Lat/Lng, Adresse)
   - Review Entity (ID, RestaurantID, Rating, Text, Datum)
   
5. **REST API für Backend entwickeln**
   - Endpoints für CRUD-Operationen
   - Authentifizierung (optional)
   
6. **SQLite-Datenbank für lokale Speicherung einrichten**
   - Room Database verwenden
   - Offline-Funktionalität

### Low Priority
7. **UI für Bewertungseingabe erstellen**
   - Rating-Sterne
   - Textfeld für Kommentare
   - Foto-Upload (optional)
   - **WICHTIG: Dark Mode implementieren**
   
8. **Docker-Setup für Home-Server erstellen**
   - Backend containerisieren
   - PostgreSQL Container
   - Reverse Proxy (Nginx/Traefik)

## Architektur-Überlegungen
- **MVVM Pattern** für Android App
- **Repository Pattern** für Datenzugriff
- **Dependency Injection**: Mit AppModule und ServiceLocator
- **Offline-First**: Lokale Speicherung mit Sync zum Server
- **Privacy-First**: Keine Tracking, keine Analytics

## Implementierte Features
1. ✅ Android Studio Projekt erstellt
2. ✅ Gradle Build konfiguriert
3. ✅ Basis-UI mit Kartenansicht implementiert
4. ✅ OSMDroid für OpenStreetMap integriert
5. ✅ Restaurant-Suche mit Overpass API
6. ✅ Tab-Navigation (Karte/Suche)
7. ✅ Dependency Injection mit AppModule
8. ✅ Service-Umschaltung zwischen Overpass und Nominatim

## Nächste Schritte: Cloud-Synchronisation

### Phase 1: Konfiguration in der App
- [ ] Settings-Activity erstellen
  - [ ] Layout mit Preferences/Einstellungen
  - [ ] Toggle: "Cloud-Sync aktivieren"
  - [ ] Eingabefeld: Server-URL (IP:Port)
  - [ ] Test-Verbindung Button
  - [ ] Speichern in SharedPreferences
- [ ] Menü-Eintrag "Einstellungen" im 3-Punkte-Menü hinzufügen
- [ ] Repository-Pattern erweitern für Offline/Online Modi

### Phase 2: REST API Backend
- [ ] Node.js/Express oder Python/FastAPI Setup
- [ ] Endpoints implementieren:
  - [ ] GET /api/reviews - Alle Bewertungen abrufen
  - [ ] GET /api/reviews/:id - Einzelne Bewertung
  - [ ] POST /api/reviews - Neue Bewertung
  - [ ] PUT /api/reviews/:id - Bewertung aktualisieren
  - [ ] DELETE /api/reviews/:id - Bewertung löschen
  - [ ] GET /api/sync - Sync-Status
- [ ] PostgreSQL Datenbank-Schema
- [ ] Fehlerbehandlung
- [ ] CORS für Android App

### Phase 3: Sync-Mechanismus
- [ ] SyncAdapter oder WorkManager für Android
- [ ] Konfliktauflösung (wenn offline bearbeitet)
- [ ] Sync-Status in UI anzeigen
- [ ] Offline-Queue für pending operations
- [ ] Retry-Mechanismus bei Netzwerkfehlern

### Phase 4: Docker Deployment
- [ ] Dockerfile für Backend
- [ ] docker-compose.yml mit:
  - [ ] Backend-Service
  - [ ] PostgreSQL
  - [ ] Optional: Nginx Reverse Proxy
- [ ] Environment Variables für Konfiguration
- [ ] Volume für Datenbank-Persistenz
- [ ] Backup-Strategie

### Phase 5: Sicherheit & Extras
- [ ] HTTPS mit Let's Encrypt
- [ ] Optional: Basis-Authentifizierung
- [ ] API Rate Limiting
- [ ] Logging & Monitoring

### Implementierte Features (Phase 2)
- ✅ Settings-Activity mit Server-Konfiguration
- ✅ Tab-Navigation erweitert (Karte/Suche/Bewertungen)
- ✅ Bewertungen bearbeiten/löschen im Bewertungstab
- ✅ Such- und Sortierfunktion im Bewertungstab
- ✅ Dark Mode (automatisch mit Material Design 3)
- ✅ Material Design 3 vollständig implementiert
  - ✅ Farbsystem
  - ✅ Typography Scale
  - ✅ Elevation & Schatten
  - ✅ 8dp Grid System
- ✅ Material Icon Font für Map Marker
- ✅ My Location Button
- ✅ Close Button im Settings Dialog

## Multi-User Support Implementation (Aktuell)

### Konzept
- Jeder Nutzer erhält eine eindeutige UUID
- Username kann geändert werden, UUID bleibt konstant
- Privacy by default: Startet mit "Anonym"
- Bei Cloud-Sync kann Username festgelegt werden

### Datenmodell-Änderungen

#### 1. User Entity (NEU)
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,      // UUID
    val userName: String,    // Anzeigename (änderbar)
    val createdAt: Long,     // Timestamp
    val isCurrentUser: Boolean // Markiert den aktiven User
)
```

#### 2. Review Entity (ERWEITERT)
```kotlin
@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val restaurantId: Long,
    val rating: Float,
    val comment: String,
    val visitDate: Date,
    val createdAt: Long,
    val userId: String,      // NEU: User UUID
    val userName: String     // NEU: Username zum Zeitpunkt der Review
)
```

### Implementierungsschritte

#### Phase 1: Datenbank-Migration ✅
1. ✅ Room Database Version erhöhen (1 → 2)
2. ✅ Migration schreiben:
   - ✅ User-Tabelle erstellen
   - ✅ Review-Tabelle um userId und userName erweitern
   - ✅ Default-User für existierende Reviews erstellen
3. ✅ UserDao implementieren
4. ✅ ReviewDao anpassen (inkl. updateUserNameInReviews)

#### Phase 2: User-Management ✅
1. ✅ UserRepository erstellen
2. ✅ UUID-Generator beim ersten App-Start (in ensureDefaultUser)
3. ✅ CurrentUser in User-Tabelle (isCurrentUser flag)
4. ✅ AppModule erweitert mit UserRepository

#### Phase 3: UI-Anpassungen ✅
1. Settings erweitern:
   - ✅ Benutzername-Eingabefeld
   - ✅ Anzeige der User-ID
   - ✅ Dialog bei Cloud-Sync wenn noch anonym
   - ✅ Automatisches Update aller Reviews bei Namensänderung
2. ReviewsFragment:
   - ⏳ Username bei Reviews anzeigen (wenn Multi-User aktiv)
3. AddReviewActivity:
   - ✅ UserId und UserName werden automatisch über Repository gesetzt

#### Phase 4: Sync-Vorbereitung ✅
1. Bei Username-Änderung:
   - ✅ Alle lokalen Reviews updaten
   - ⏳ Flag für Sync setzen
2. Cloud-Sync Dialog:
   - ✅ "Wie möchtest du erscheinen?" wenn noch Anonym
3. API-Endpoints vorbereiten:
   - ⏳ User-Registrierung/Update
   - ⏳ Reviews mit User-Daten

### Implementierungsdetails

#### Benutzer-System
- **UUID-basiert**: Jeder Nutzer erhält eine eindeutige UUID, die nie geändert wird
- **Privacy by default**: App startet mit "Anonym" als Benutzername
- **Flexibel**: Benutzername kann jederzeit geändert werden
- **Konsistent**: Bei Namensänderung werden alle Reviews automatisch aktualisiert

#### Settings-Dialog
- **Benutzer-Section** ganz oben mit:
  - Eingabefeld für Benutzername
  - Anzeige der User-ID (klein, grau)
- **Cloud-Sync Aktivierung**:
  - Dialog wenn noch anonym: "Möchtest du einen Namen festlegen?"
  - Bestätigung bei Namensänderung während aktivem Sync

#### Datenbank-Schema
```sql
-- Users Tabelle
CREATE TABLE users (
    userId TEXT NOT NULL PRIMARY KEY,
    userName TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    isCurrentUser INTEGER NOT NULL
);

-- Reviews erweitert um:
ALTER TABLE reviews ADD COLUMN userId TEXT NOT NULL;
ALTER TABLE reviews ADD COLUMN userName TEXT NOT NULL;
```

## REST API Backend (Implementiert)

### Server-Setup
- **Framework**: Node.js mit Express
- **Datenbank**: PostgreSQL
- **Deployment**: Docker Compose

### Implementierte API Endpoints

#### Health Check
- `GET /api/health` - Server und Datenbank Status

#### Users
- `GET /api/users` - Alle Benutzer auflisten
- `GET /api/users/:userId` - Benutzerdetails abrufen
- `PUT /api/users/:userId` - Benutzer erstellen/aktualisieren

#### Reviews
- `GET /api/reviews/user/:userId` - Alle Bewertungen eines Benutzers
- `GET /api/reviews/restaurant/:restaurantId` - Alle Bewertungen eines Restaurants
- `POST /api/reviews` - Neue Bewertung erstellen
- `PUT /api/reviews/:reviewId` - Bewertung aktualisieren
- `DELETE /api/reviews/:reviewId` - Bewertung löschen
- `POST /api/reviews/sync` - Bulk-Sync für mehrere Bewertungen

### Datenbank-Schema (PostgreSQL)
```sql
-- Users Tabelle
CREATE TABLE users (
  user_id VARCHAR(36) PRIMARY KEY,
  user_name VARCHAR(255) NOT NULL DEFAULT 'Anonym',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Reviews Tabelle  
CREATE TABLE reviews (
  id SERIAL PRIMARY KEY,
  restaurant_id BIGINT NOT NULL,
  restaurant_name VARCHAR(255) NOT NULL,
  restaurant_lat DOUBLE PRECISION NOT NULL,
  restaurant_lon DOUBLE PRECISION NOT NULL,
  restaurant_address TEXT,
  rating DECIMAL(2,1) NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  visit_date DATE NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Sync Metadata
CREATE TABLE sync_metadata (
  id SERIAL PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  last_sync TIMESTAMP NOT NULL DEFAULT NOW(),
  sync_type VARCHAR(50) NOT NULL,
  changes_count INT NOT NULL DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### Docker Deployment
```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: myreviews
      POSTGRES_USER: myreviews_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./server/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    
  api:
    build: ./server
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: myreviews
      DB_USER: myreviews_user
      DB_PASSWORD: ${DB_PASSWORD}
      PORT: 3000
    ports:
      - "3000:3000"
```

### Deployment-Anleitung
1. `.env` Datei erstellen (von `.env.example` kopieren)
2. Passwort in `.env` setzen
3. `docker-compose up -d` ausführen
4. API ist erreichbar unter `http://localhost:3000`

## Aktueller Stand (August 2025)

### Cloud-Sync ist funktionsfähig!
- Server läuft lokal auf Port 3000
- Android App verbindet sich über 10.0.2.2:3000 (Emulator → localhost)
- Verbindungstest in Settings funktioniert
- Synchronisation funktioniert

### Letzte Änderungen
1. **Benutzername in Bewertungsliste hinzugefügt**:
   - In `ReviewsFragment.kt` wird nun "von [Username]" angezeigt
   - Kursiv unter dem Restaurantnamen
   - Code wurde committed, App muss neu gebaut werden

### Server starten

#### Development (mit Auto-Reload)
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews/server
./start-dev.sh
```
Der Server startet mit Nodemon und lädt automatisch neu bei Code-Änderungen.

#### Production (ohne Docker)
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews/server
DB_HOST=localhost DB_PORT=5432 DB_NAME=myreviews DB_USER=myreviews_user DB_PASSWORD=postgres PORT=3000 node index.js
```

#### Production (mit Docker)
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews
docker-compose up -d
```

### App bauen und installieren
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews
./gradlew assembleDebug installDebug
```

### Wichtige Dateien
- Server: `/server/index.js` (nutzt `routes/health-simple.js` ohne DB)
- Android Sync: `app/src/main/java/com/myreviews/app/data/api/SyncService.kt`
- Settings: `app/src/main/java/com/myreviews/app/ui/settings/SettingsActivity.kt`

### Implementierte Sync-Features (August 2025)

#### Tombstone-Synchronisation
- Gelöschte Reviews werden nicht wirklich gelöscht, sondern mit `isDeleted = true` markiert
- Diese "Grabsteine" bleiben für immer in der Datenbank
- Ermöglicht zuverlässige Synchronisation auch bei längerer Offline-Zeit
- Server und Client filtern gelöschte Reviews bei der Anzeige heraus

#### Geräte-persistente User ID
- Basiert auf Android Secure ID (`Settings.Secure.ANDROID_ID`)
- UUID bleibt gleich, auch nach App-Neuinstallation
- Ermöglicht Bearbeitung eigener Reviews nach Neuinstallation
- User-ID wird deterministisch aus Geräte-ID generiert

#### Sync-Verhalten
- Editierte Reviews: `syncedAt` wird auf `null` gesetzt, beim nächsten Sync hochgeladen
- Gelöschte Reviews: Werden als gelöscht markiert und beim Sync an Server gemeldet
- Reactions: Werden in Echtzeit an Server gesendet und lokal aktualisiert

### Noch offene Features
- [ ] Export/Import von Bewertungen  
- [ ] Fotos zu Bewertungen hinzufügen
- [ ] Multi-Device Support (gleiche UUID)
- [ ] Weitere User-Features (Avatar, etc.)