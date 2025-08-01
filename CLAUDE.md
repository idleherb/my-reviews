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

#### Phase 3: UI-Anpassungen ⏳
1. Settings erweitern:
   - ⏳ Benutzername-Eingabefeld
   - ⏳ Anzeige der User-ID (optional)
2. ReviewsFragment:
   - ⏳ Username bei Reviews anzeigen (wenn nicht eigene)
3. AddReviewActivity:
   - ✅ UserId und UserName werden automatisch über Repository gesetzt

#### Phase 4: Sync-Vorbereitung
1. Bei Username-Änderung:
   - Alle lokalen Reviews updaten
   - Flag für Sync setzen
2. Cloud-Sync Dialog:
   - "Wie möchtest du erscheinen?" wenn noch Anonym
3. API-Endpoints vorbereiten:
   - User-Registrierung/Update
   - Reviews mit User-Daten

### Noch offene Features
- [ ] Export/Import von Bewertungen
- [ ] Fotos zu Bewertungen hinzufügen
- [ ] Multi-Device Support (gleiche UUID)
- [ ] Weitere User-Features (Avatar, etc.)