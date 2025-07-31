#!/bin/bash

# Android SDK Pfad setzen
export ANDROID_SDK_ROOT=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/emulator
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin

echo "Installiere notwendige Komponenten..."
sdkmanager --install "emulator" "platform-tools"

echo "Installiere System Image für ARM64..."
sdkmanager --install "system-images;android-34;google_apis;arm64-v8a"

echo "Erstelle AVD..."
echo no | avdmanager create avd \
    -n "MyReviews_Emulator" \
    -k "system-images;android-34;google_apis;arm64-v8a" \
    -d "pixel_6" \
    -c 512M

echo "AVD erstellt! Starte Emulator mit:"
echo "emulator -avd MyReviews_Emulator"