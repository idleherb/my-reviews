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

## Implementierte Cloud-Synchronisation

### Phase 1: Konfiguration ✅
- ✅ Settings-Activity mit Cloud-Sync Toggle
- ✅ Server-URL und Port-Konfiguration  
- ✅ Verbindungstest-Funktionalität
- ✅ Benutzer-Profile mit UUID-System
- ✅ Avatar-Upload/Delete-Funktionalität

### Phase 2: REST API Backend ✅
- ✅ Node.js/Express Server mit PostgreSQL
- ✅ Vollständige CRUD-API für Reviews und Users
- ✅ Avatar-Upload mit Multer
- ✅ Emoji-Reaktionen System
- ✅ Docker-Deployment mit docker-compose
- ✅ Health-Check Endpoints

### Phase 3: Sync-Mechanismus ✅
- ✅ Tombstone-basierte Sync-Architektur
- ✅ UUID-basierte Reviews (client-generiert)
- ✅ Konfliktkresolution mit Timestamps
- ✅ Offline-First Funktionalität

### Phase 4: Automatische Synchronisation ✅
- ✅ **AutoSyncManager**: Zentrales Management für automatische Sync-Operationen
- ✅ **AutoSync per Default aktiv** wenn Cloud-Sync eingeschaltet
- ✅ **Konfigurierbar**: AutoSync kann in Einstellungen deaktiviert werden
- ✅ **Manueller Sync-Fallback**: Sync-Button nur sichtbar wenn AutoSync deaktiviert
- ✅ **Umfassende Trigger-Integration**:
  - ✅ App-Start (MainActivity)
  - ✅ Review-Operationen (Add/Edit/Delete)
  - ✅ Reaktions-Änderungen (Add/Remove)
  - ✅ Einstellungs-Speicherung
- ✅ **Stilles Sync-Verhalten**: Keine UI-Benachrichtigungen bei AutoSync
- ✅ **Sync-Button aus ReviewsFragment entfernt**: Nur noch in Einstellungen verfügbar

### Phase 5: UI/UX Features ✅
- ✅ Material Design 3 komplette Integration
- ✅ Tab-Navigation (Karte/Bewertungen/Suche)
- ✅ Avatar-System mit Cloud-Upload
- ✅ Emoji-Reaktionen auf fremde Reviews
- ✅ Such- und Sortier-Funktionalität
- ✅ Multi-User Support mit Privacy-by-Default

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

### ✅ Vollständige UUID-Migration und Avatar-Handling implementiert!

#### UUID-basierte Review-IDs
- **Problem gelöst**: Reviews verschwanden nach Sync aufgrund von ID-Konflikten
- **Lösung**: Client generiert UUIDs, Server speichert sie nur
- **Migration**: Automatische Konvertierung von Long zu String IDs
- **Kompatibilität**: Alle APIs und UI-Komponenten angepasst

#### Avatar-System vollständig funktional
- **Upload**: Fotos werden auf Server gespeichert, Cache aktualisiert
- **Delete**: Avatars werden entfernt, UI sofort aktualisiert
- **Anzeige**: Echtzeit-Updates in allen Review-Listen
- **Storage**: Server speichert Avatars in `/uploads/avatars/`

#### Verbesserte Sortierung
- **Primär**: Nach Besuchsdatum (visitDate)
- **Sekundär**: Nach Erstellungszeit (createdAt) bei gleichem Datum
- **Resultat**: Reviews erscheinen immer in korrekter Reihenfolge

### Cloud-Sync ist funktionsfähig!
- Server läuft lokal auf Port 3000
- Android App verbindet sich über 10.0.2.2:3000 (Emulator → localhost)
- Verbindungstest in Settings funktioniert
- Synchronisation funktioniert

### Letzte Implementierungen ✅

#### 1. Automatische Synchronisation
- **AutoSyncManager**: Vollständig implementiert mit allen Triggern
- **Standardverhalten**: AutoSync per Default aktiv
- **UI-Optimierung**: Sync-Button nur in Einstellungen, direkt unter AutoSync-Toggle

#### 2. Professionelles Avatar-System
- **Avatar-Crop-Funktionalität**:
  - Kreisförmige Auswahlmaske mit halbtransparentem Overlay
  - Pinch-to-Zoom (1x-5x) und Pan-Gesten
  - Speichert nur den sichtbaren Kreisbereich
- **UI-Verbesserungen**:
  - Avatar immer als perfekter Kreis (80dp)
  - Avatar-Bild selbst ist klickbar
  - "Foto löschen" nur aktiv wenn Avatar vorhanden
  - Bestätigungsdialog vor dem Löschen
- **Smart Clipping**: Rechteckige Bilder werden automatisch rund zugeschnitten

#### 3. Karten-Navigation Fix
- **Problem**: Horizontales Verschieben der Karte löste Tab-Wechsel aus
- **Lösung**: `isUserInputEnabled = false` im ViewPager2
- **Resultat**: Karte frei beweglich, Tabs nur per Klick wechselbar

#### 4. Custom App-Icon
- **Design**: Blauer Kreis mit weißem Restaurant-Symbol (Gabel & Messer)
- **Adaptive Icon**: Unterstützt verschiedene Icon-Formen ab Android 8.0
- **Farbe**: Material Blue (#2196F3) - passend zu den Karten-Markern
- **Icon-Größe**: Symbol um 30% vergrößert für bessere Sichtbarkeit
- **Hinweis**: Toast-Icons in Android 12+ werden automatisch vom System angezeigt

#### 5. UI-Verbesserungen (August 2025)
- **Tab-Layout Fix**: 
  - "Bewertungen" bricht nicht mehr in nächste Zeile um
  - `TabLayout.MODE_FIXED` verhindert scrollbare Tabs
  - `TabLayout.GRAVITY_FILL` verteilt Tabs gleichmäßig
- **AutoSync Default**: 
  - Automatisch aktiviert beim Einschalten von Cloud-Sync
  - Bessere User Experience ohne manuellen Eingriff
- **Icon-Ressourcen**: Alle PNG-Dateien wiederhergestellt für fehlerfreien Build

### 📝 Entwicklungsprozess-Dokumentation

**Wichtig**: Jeder inhaltliche Entwicklungsschritt wird:
1. **Implementiert** mit sauberem, kommentiertem Code
2. **Getestet** durch Build und Installation auf Emulatoren
3. **Committed** mit aussagekräftigen Commit-Messages
4. **Dokumentiert** in dieser CLAUDE.md Datei

Diese Praxis sorgt für:
- Nachvollziehbare Entwicklungshistorie
- Einfaches Rollback bei Problemen
- Klare Kommunikation über Änderungen
- Professionelle Projektführung

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

### Wichtige neue Dateien
- **Migration Scripts**: `server/db/migrations/002_uuid_reviews.sql`, `003_add_avatars.sql`
- **Migration Runner**: `server/run-migration.js`
- **Avatar API**: `server/routes/avatars.js`
- **Upload Storage**: `server/uploads/avatars/`

### Automatische Synchronisation ✅ IMPLEMENTIERT

#### ✅ Vollständig umgesetztes AutoSync-System
Die App synchronisiert jetzt automatisch im Hintergrund, ohne dass der Benutzer manuell eingreifen muss.

#### ✅ Phase 1: AutoSync Infrastructure
- ✅ **AutoSyncManager**: Zentrales Singleton für Sync-Management
- ✅ **Per Default aktiv**: AutoSync ist automatisch eingeschaltet bei aktivem Cloud-Sync
- ✅ **Konfigurierbar**: Kann in Einstellungen deaktiviert werden
- ✅ **Graceful Fallback**: Manueller Sync-Button nur wenn AutoSync deaktiviert

#### ✅ Phase 2: Umfassende Sync-Trigger
- ✅ **App-Start**: Triggert AutoSync beim App-Launch (MainActivity)
- ✅ **Review-Operationen**: AutoSync bei Add/Edit/Delete von Reviews
- ✅ **Reaktions-Änderungen**: AutoSync bei Add/Remove von Emoji-Reaktionen
- ✅ **Einstellungs-Updates**: AutoSync beim Speichern von Einstellungen
- ✅ **Stilles Verhalten**: Keine UI-Benachrichtigungen bei AutoSync

#### ✅ Phase 3: UI-Optimierungen
- ✅ **Sync-Button entfernt**: Nicht mehr in ReviewsFragment sichtbar
- ✅ **Settings-Integration**: Manueller Sync nur noch in Einstellungen verfügbar
- ✅ **AutoSync-Toggle**: Benutzer kann AutoSync deaktivieren bei Bedarf
- ✅ **Offline-First**: App funktioniert vollständig ohne Serververbindung

### Weitere mögliche Features
- [ ] Push-Notifications für neue Reviews anderer User
- [ ] Export/Import von Bewertungen als JSON/CSV
- [ ] Fotos zu Bewertungen hinzufügen
- [ ] Multi-Device Support (gleiche UUID auf mehreren Geräten)
- [ ] WorkManager für periodische Hintergrund-Syncs
- [ ] Offline-Maps Integration für bessere Performance
- [ ] Analytics und Usage-Tracking (optional)

## 🚀 TrueNAS SCALE Deployment Plan

### Übersicht
Deployment der My Reviews Backend-Infrastruktur (PostgreSQL + Node.js API) auf TrueNAS SCALE Fangtooth 25.04 im Home-Netzwerk mit VPN-Zugriff.

### Voraussetzungen
- TrueNAS SCALE Fangtooth 25.04.1 installiert und konfiguriert
- Netzwerkzugriff zum TrueNAS System
- Grundlegende Kenntnisse der TrueNAS Web-UI
- VPN bereits eingerichtet (keine zusätzliche Security nötig)

### Phase 1: Vorbereitung auf TrueNAS

#### 1.1 Storage Pool und Dataset erstellen
1. **Navigiere zu**: Storage → Pools
2. **Erstelle Dataset**: `apps/myreviews`
   - Record Size: 128K (default)
   - Compression: lz4
   - Sync: Standard
3. **Erstelle Unterverzeichnisse**:
   - `apps/myreviews/postgres` (für Datenbank-Daten)
   - `apps/myreviews/uploads` (für Avatar-Uploads)
   - `apps/myreviews/config` (für Konfigurationsdateien)

#### 1.2 Netzwerk-Konfiguration prüfen
1. **Notiere**: IP-Adresse des TrueNAS Systems
2. **Stelle sicher**: Ports 3000 (API) und 5432 (PostgreSQL) sind verfügbar
3. **Prüfe**: Keine Firewall-Regeln blockieren diese Ports

### Phase 2: Docker Image vorbereiten

#### 2.1 API Docker Image bauen
Da TrueNAS kein lokales Build unterstützt, müssen wir das Image vorab bauen und in eine Registry pushen:

**Option A: Docker Hub (öffentlich)**
```bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews/server
docker build -t yourusername/myreviews-api:latest .
docker push yourusername/myreviews-api:latest
```

**Option B: Lokale Registry auf TrueNAS**
1. Deploye zuerst eine Docker Registry auf TrueNAS
2. Pushe das Image zur lokalen Registry

**Option C: Pre-built Image bereitstellen**
1. Exportiere das Image als TAR:
   ```bash
   docker save myreviews-api:latest > myreviews-api.tar
   ```
2. Kopiere auf TrueNAS und importiere

### Phase 3: TrueNAS Docker Compose Deployment

#### 3.1 Docker Compose App vorbereiten
1. **Erstelle angepasste docker-compose.yml für TrueNAS**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: myreviews-db
    environment:
      POSTGRES_DB: myreviews
      POSTGRES_USER: myreviews_user
      POSTGRES_PASSWORD: ${DB_PASSWORD:-changeme}
    volumes:
      - /mnt/[your-pool]/apps/myreviews/postgres:/var/lib/postgresql/data
      - /mnt/[your-pool]/apps/myreviews/config/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U myreviews_user -d myreviews"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    image: yourusername/myreviews-api:latest
    container_name: myreviews-api
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: myreviews
      DB_USER: myreviews_user
      DB_PASSWORD: ${DB_PASSWORD:-changeme}
      PORT: 3000
      NODE_ENV: production
      CORS_ORIGIN: "*"
    volumes:
      - /mnt/[your-pool]/apps/myreviews/uploads:/app/uploads
    ports:
      - "3000:3000"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

#### 3.2 Via TrueNAS Custom App (Docker Compose)
1. **Navigiere zu**: Apps → Discover Apps → Custom App
2. **Wähle**: "Create Custom App"
3. **Application Name**: `myreviews`
4. **Container Images**: Nicht nötig bei Docker Compose
5. **Compose Configuration**:
   - Füge die docker-compose.yml ein
   - Oder verwende Git Repository URL
6. **Storage**: Wird über docker-compose.yml Volumes definiert
7. **Install** klicken

#### 3.3 Alternative: Via Portainer (Empfohlen für komplexe Setups)
1. **Installiere Portainer** aus dem TrueNAS App Katalog
2. **Öffne Portainer UI**: `http://[truenas-ip]:9000`
3. **Erstelle Stack**:
   - Stacks → Add Stack
   - Name: `myreviews`
   - Build method: "Web editor" oder "Git Repository"
   - Paste docker-compose.yml oder Git URL
   - Environment variables:
     - `DB_PASSWORD`: [sicheres-passwort]
4. **Deploy the stack**

#### 3.4 Wichtige Pfad-Anpassungen
- Ersetze `/mnt/[your-pool]/` mit deinem tatsächlichen Pool-Pfad
- Beispiel: `/mnt/tank/apps/myreviews/`
- Stelle sicher, dass die Verzeichnisse existieren und Schreibrechte haben

```yaml
name: myreviews
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: myreviews
      POSTGRES_USER: myreviews_user
      POSTGRES_PASSWORD: your_secure_password
    volumes:
      - type: bind
        source: /mnt/pool/apps/myreviews/postgres
        target: /var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U myreviews_user -d myreviews"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    image: yourusername/myreviews-api:latest
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: myreviews
      DB_USER: myreviews_user
      DB_PASSWORD: your_secure_password
      PORT: 3000
      NODE_ENV: production
      CORS_ORIGIN: "*"
    volumes:
      - type: bind
        source: /mnt/pool/apps/myreviews/uploads
        target: /app/uploads
    ports:
      - "3000:3000"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
```

3. **Wichtige Anpassungen**:
   - Ersetze `/mnt/pool/` mit deinem tatsächlichen Pool-Pfad
   - Verwende ein sicheres Passwort
   - Passe die Image-URL an

#### 3.2 Via Wizard (Alternative)
Falls YAML nicht funktioniert, nutze den Custom App Wizard:

1. **App 1: PostgreSQL**
   - Name: `myreviews-postgres`
   - Image: `postgres:15-alpine`
   - Environment Variables hinzufügen
   - Host Path Volume konfigurieren
   - Port 5432 freigeben

2. **App 2: API**
   - Name: `myreviews-api`
   - Image: Dein gepushtes Image
   - Environment Variables hinzufügen
   - Host Path Volume für uploads
   - Port 3000 freigeben

### Phase 4: Datenbank initialisieren

#### 4.1 Schema laden
1. **Kopiere** `schema.sql` nach TrueNAS:
   ```bash
   scp server/db/schema.sql root@truenas-ip:/mnt/pool/apps/myreviews/config/
   ```

2. **Führe aus** (via TrueNAS Shell):
   ```bash
   docker exec -i myreviews-postgres psql -U myreviews_user -d myreviews < /path/to/schema.sql
   ```

#### 4.2 Migrations ausführen
Falls Migrations vorhanden:
```bash
docker exec myreviews-api node run-migration.js
```

### Phase 5: Android App konfigurieren

#### 5.1 Server-URL anpassen
In der Android App Einstellungen:
- Server URL: `truenas-ip` (ohne http://)
- Port: `3000`
- Verbindung testen

#### 5.2 Netzwerk-Zugriff
- **Im lokalen Netzwerk**: Direkte IP verwenden
- **Über VPN**: VPN-IP oder Hostname verwenden

### Phase 6: Monitoring und Wartung

#### 6.1 Logs prüfen
- TrueNAS UI: Apps → Installed → App Name → Logs
- Oder via Shell: `docker logs myreviews-api`

#### 6.2 Backups
1. **Datenbank-Backup** (täglich):
   ```bash
   docker exec myreviews-postgres pg_dump -U myreviews_user myreviews > backup.sql
   ```

2. **Upload-Verzeichnis**: Snapshot des Datasets

#### 6.3 Updates
1. Neues Docker Image bauen und pushen
2. In TrueNAS: App stoppen → Image-Tag ändern → App starten

### Troubleshooting

#### Häufige Probleme:
1. **Permission Errors**: 
   - Stelle sicher, dass die Datasets die richtigen Permissions haben
   - UID/GID im Container muss zum Host passen

2. **Netzwerk-Probleme**:
   - Prüfe ob die Ports nicht bereits belegt sind
   - Teste mit `curl http://truenas-ip:3000/api/health`

3. **Container startet nicht**:
   - Logs prüfen
   - Healthchecks validieren
   - Environment Variables überprüfen

### Sicherheitshinweise
- Da im VPN: Keine zusätzliche Authentifizierung nötig
- Trotzdem: Verwende sichere Passwörter
- Regelmäßige Backups sind wichtig
- Updates zeitnah einspielen