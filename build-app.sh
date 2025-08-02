#!/bin/bash
cd /Users/eric.hildebrand/dev/public/idleherb/my-reviews
./gradlew clean assembleDebug
echo "Build completed. APK location:"
echo "app/build/outputs/apk/debug/app-debug.apk"