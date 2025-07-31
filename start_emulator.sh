#!/bin/bash

echo "=== Android Emulator Setup für My Reviews App ==="
echo ""
echo "WICHTIG: Bevor du dieses Script ausführst:"
echo "1. Öffne Android Studio"
echo "2. Gehe zu Tools → AVD Manager"
echo "3. Klicke auf 'Create Virtual Device'"
echo "4. Wähle ein Gerät (z.B. Pixel 6)"
echo "5. Wähle Android 14 (API 34)"
echo "6. Gib dem AVD einen Namen"
echo ""
echo "Alternativ kannst du auch:"
echo "- Den Emulator direkt aus Android Studio starten (grüner Play-Button)"
echo "- Die App auf einem echten Android-Gerät testen (USB-Debugging aktivieren)"
echo ""
echo "Um die App zu installieren und zu starten:"
echo "./gradlew installDebug"
echo ""
echo "Verfügbare Emulatoren anzeigen:"
export ANDROID_SDK_ROOT=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/emulator
emulator -list-avds