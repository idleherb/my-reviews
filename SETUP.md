# Setup-Anleitung für macOS

## Voraussetzungen

### 1. Android Studio installieren
```bash
# Mit Homebrew:
brew install --cask android-studio

# Oder direkt von https://developer.android.com/studio
```

### 2. Nach der Installation von Android Studio:
1. Android Studio öffnen
2. SDK Manager öffnen (Tools → SDK Manager)
3. Folgendes installieren:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Platform-Tools
   - Android Emulator
   - Intel x86 Emulator Accelerator (HAXM)

### 3. Emulator erstellen
1. AVD Manager öffnen (Tools → AVD Manager)
2. "Create Virtual Device" klicken
3. Gerät wählen (z.B. Pixel 6)
4. System Image wählen (API 34)
5. AVD benennen und erstellen

## App bauen und starten

### Option 1: Mit Android Studio
1. Projekt in Android Studio öffnen
2. Grünen Play-Button drücken
3. Emulator oder Gerät auswählen

### Option 2: Command Line
```bash
# Gradle Wrapper ausführbar machen
chmod +x gradlew

# App bauen
./gradlew build

# Tests ausführen
./gradlew test

# App auf Emulator installieren
./gradlew installDebug

# Emulator starten (falls nicht läuft)
emulator -avd <AVD_Name>
```

## Entwicklung ohne Android Studio

Falls Sie lieber mit VS Code oder einem anderen Editor arbeiten:

```bash
# Android SDK Tools installieren
brew install --cask android-sdk
brew install --cask android-platform-tools

# Umgebungsvariablen setzen (in ~/.zshrc oder ~/.bash_profile)
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## Physisches Gerät verwenden

1. Entwickleroptionen auf Android-Gerät aktivieren
2. USB-Debugging einschalten
3. Gerät per USB verbinden
4. `adb devices` ausführen um zu prüfen ob erkannt

## Troubleshooting

- **Gradle Sync Failed**: SDK Pfad in `local.properties` prüfen
- **Emulator startet nicht**: HAXM Installation prüfen
- **Build failed**: `./gradlew clean` ausführen