# Emulator Setup - Schritt für Schritt

## Der einfachste Weg:

### 1. Android Studio AVD Manager öffnen
- Android Studio ist bereits geöffnet
- Klicke oben in der Menüleiste auf **Tools** → **AVD Manager**
- Falls ein Dialog erscheint, klicke auf "Create Virtual Device"

### 2. Gerät auswählen
- Wähle **Phone** (links)
- Wähle **Pixel 6** (oder ein anderes Gerät)
- Klicke **Next**

### 3. System Image wählen
- Wähle **API 34** (Android 14)
- Falls ein Download-Symbol daneben ist, klicke darauf
- Warte bis der Download fertig ist
- Klicke **Next**

### 4. AVD benennen
- Name: "MyReviews Emulator" (oder was du willst)
- Klicke **Finish**

### 5. Emulator starten
- Im AVD Manager siehst du jetzt deinen Emulator
- Klicke auf den **grünen Play-Button** ▶️
- Warte bis der Emulator hochgefahren ist (kann 1-2 Minuten dauern)

### 6. App installieren
Sobald der Emulator läuft, im Terminal:
```bash
./gradlew installDebug
```

Die App erscheint dann auf dem Emulator!

## Alternative: Echtes Android-Gerät
Falls du ein Android-Handy hast:
1. Aktiviere Entwickleroptionen (7x auf Build-Nummer tippen)
2. Aktiviere USB-Debugging
3. Verbinde das Handy per USB
4. `./gradlew installDebug`

## Troubleshooting
- **Emulator startet nicht**: Mehr RAM zuweisen (in AVD Config)
- **App installiert nicht**: Emulator neustarten
- **Karte zeigt nichts**: Internet-Verbindung prüfen