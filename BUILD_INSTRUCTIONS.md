# MediPulse - Build Instructions

## Prerequisites

### Required Software

1. **Android Studio** - Hedgehog (2023.1.1) or later
    - Download: https://developer.android.com/studio

2. **JDK 17** - Java Development Kit
    - Bundled with Android Studio OR
    - Download: https://adoptium.net/temurin/releases/

3. **Android SDK** - API Level 24-36
    - Installed via Android Studio SDK Manager

### Device Requirements

- Android device or emulator with API 24+ (Android 7.0+)
- ~2GB RAM available
- ~400MB storage for AI model
- Internet connection (for initial model download)

---

## Quick Start (5 Steps)

### Step 1: Open Project in Android Studio

```bash
# Navigate to project directory
cd C:\Users\Meghana\StudioProjects\Hackss

# Open in Android Studio
# File → Open → Select project folder
```

### Step 2: Sync Gradle Dependencies

```
Android Studio will automatically prompt to sync Gradle.
Click "Sync Now" when prompted.

Wait for:
✅ Gradle sync to complete
✅ Dependencies to download
✅ Build configuration to finish
```

### Step 3: Connect Device or Start Emulator

**Option A: Physical Device**

- Enable Developer Options on device
- Enable USB Debugging
- Connect via USB
- Accept debugging authorization

**Option B: Emulator**

- Tools → Device Manager
- Create Device (Pixel 5, API 34 recommended)
- Start emulator

### Step 4: Build and Run

```
Click the green "Run" button (▶️) or press Shift+F10

Build process:
✅ Compiling Kotlin code
✅ Processing resources
✅ Packaging APK/AAB
✅ Installing on device
✅ Launching app
```

### Step 5: Download AI Model

Once app launches:

1. Navigate to **Setup** tab (gear icon at bottom)
2. Find "Qwen 2.5 0.5B Instruct Q6_K" model
3. Tap **Download** button
4. Wait for download (~374 MB, ~2-5 minutes)
5. Tap **Load** button
6. Wait for "✅ AI Model loaded! MediPulse fully operational."

**Done! The app is ready to use.**

---

## Build Variants

### Debug Build (Development)

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Production)

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
# Note: Requires signing configuration
```

---

## Troubleshooting

### Issue: Gradle Sync Fails

**Solution:**

```bash
# Clear Gradle cache
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# In Android Studio:
# File → Invalidate Caches → Invalidate and Restart
```

### Issue: Build Error - Java Version

**Error:** `Unsupported Java version` or `JAVA_HOME not set`

**Solution:**

1. File → Project Structure → SDK Location
2. Set JDK Location to JDK 17
3. Or set JAVA_HOME environment variable:
   ```bash
   # Windows
   set JAVA_HOME=C:\Program Files\Android Studio\jbr
   
   # Mac/Linux
   export JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home
   ```

### Issue: Module Not Found

**Error:** `Module 'app' not found`

**Solution:**

```bash
# Sync Gradle
./gradlew sync

# Or in Android Studio:
# File → Sync Project with Gradle Files
```

### Issue: SDK Not Found

**Error:** `SDK location not found`

**Solution:**

1. Open SDK Manager (Tools → SDK Manager)
2. Install required SDK versions (API 24-36)
3. Create/edit `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

### Issue: App Crashes on Launch

**Solutions:**

- Check device API level (must be 24+)
- Check Logcat for error messages
- Verify permissions in AndroidManifest.xml
- Clear app data and reinstall

### Issue: AI Model Download Fails

**Solutions:**

- Check internet connection
- Verify storage space (~400MB needed)
- Check INTERNET permission
- Try restarting app
- Check Logcat for network errors

### Issue: AI Model Won't Load

**Solutions:**

- Ensure model fully downloaded (check file size)
- Verify device has 2GB+ RAM available
- Close background apps
- Restart app
- Check `largeHeap="true"` in AndroidManifest.xml

---

## Build Configuration

### Gradle Files

**build.gradle.kts (app)**

```kotlin
android {
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

**Key Dependencies:**

- RunAnywhere SDK (Core + LlamaCpp Module)
- Jetpack Compose (UI)
- Kotlin Coroutines (Async)
- AndroidX Lifecycle (MVVM)
- Material 3 (Design)

---

## Verification Checklist

### After Successful Build:

- [ ] App installs without errors
- [ ] All 5 tabs visible (Dashboard, AcciAid, LifeLink, RouteIQ, Setup)
- [ ] Status bar shows "Initializing MediPulse..." then "ready"
- [ ] Setup tab shows available model
- [ ] No crashes during navigation

### After Model Download:

- [ ] Download completes (100%)
- [ ] Model shows "Downloaded" status
- [ ] Load button becomes enabled
- [ ] Clicking Load shows loading message

### After Model Load:

- [ ] Status shows "✅ AI Model loaded! MediPulse fully operational."
- [ ] Model card shows "Currently Active"
- [ ] Dashboard shows green status indicator
- [ ] All agent features accessible

### Test Basic Functionality:

- [ ] Simulate Accident works (Dashboard)
- [ ] Activity log updates in real-time
- [ ] AcciAid shows 4 hospitals
- [ ] AcciAid shows 3 ambulances
- [ ] LifeLink shows blood inventory
- [ ] RouteIQ shows 8 traffic areas
- [ ] Tab navigation smooth and responsive

---

## Advanced Build Options

### Clean Build

```bash
./gradlew clean build
```

### Build with Logs

```bash
./gradlew assembleDebug --info
# or for more detail
./gradlew assembleDebug --debug
```

### Check Dependencies

```bash
./gradlew dependencies
```

### Lint Check

```bash
./gradlew lint
```

### Run Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

---

## APK Installation (Manual)

### Generate APK

```bash
./gradlew assembleDebug
```

### Install via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk

# Force reinstall
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Check Installed Version

```bash
adb shell pm list packages | grep runanywhere
adb shell dumpsys package com.runanywhere.startup_hackathon20
```

---

## Performance Tips

### Faster Builds

1. Enable Gradle daemon:
   ```properties
   # gradle.properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

2. Increase heap size:
   ```properties
   # gradle.properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
   ```

3. Use build cache:
   ```bash
   ./gradlew assembleDebug --build-cache
   ```

### Faster Model Download

- Use WiFi instead of mobile data
- Download when device has good connectivity
- Keep device awake during download
- Don't switch apps during download

---

## Environment Setup Verification

### Check Java Version

```bash
java -version
# Should show: openjdk version "17..." or higher
```

### Check Android SDK

```bash
# Windows
dir %ANDROID_HOME%\platforms

# Mac/Linux
ls $ANDROID_HOME/platforms
# Should list android-24 through android-36
```

### Check Gradle Version

```bash
./gradlew --version
# Should show: Gradle 8.x or higher
```

---

## Support

### Logs Location

```
Android Studio → Logcat → Filter: "MediPulse" or "MasterOrchestrator"
```

### Common Log Tags

- `MasterOrchestrator` - Main coordination logs
- `AcciAidAgent` - Accident detection logs
- `LifeLinkAgent` - Blood/maternity logs
- `RouteIQAgent` - Traffic/routing logs
- `MediPulseViewModel` - UI state logs
- `MyApp` - SDK initialization logs

### Get Help

1. Check IMPLEMENTATION_SUMMARY.md for architecture details
2. Check README.md for feature documentation
3. Check DEMO_GUIDE.md for usage examples
4. Check Logcat for error messages
5. Review agent code for implementation details

---

## Quick Reference

### Build Commands

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Run tests
./gradlew test
```

### Android Studio Shortcuts

- **Build**: Ctrl+F9 (Cmd+F9 on Mac)
- **Run**: Shift+F10 (Ctrl+R on Mac)
- **Debug**: Shift+F9 (Ctrl+D on Mac)
- **Stop**: Ctrl+F2 (Cmd+F2 on Mac)
- **Clean Project**: Build → Clean Project
- **Rebuild**: Build → Rebuild Project

---

## Success Indicators

### Build Successful When You See:

```
BUILD SUCCESSFUL in Xs Ys
```

### App Running When You See:

- MediPulse splash/logo
- Bottom navigation with 5 tabs
- Status bar with system message
- No crash dialogs

### AI Ready When You See:

- "✅ AI Model loaded! MediPulse fully operational."
- Green checkmark on model card
- "Currently Active" badge

---

## Next Steps After Build

1. ✅ **Test All Features** - Follow DEMO_GUIDE.md
2. ✅ **Review Code** - Check IMPLEMENTATION_SUMMARY.md
3. ✅ **Prepare Demo** - Practice with demo scenarios
4. ✅ **Showcase** - Present to judges/team

---

**Happy Building! 🚀**

If you encounter any issues not covered here, check the full documentation in README.md or review
the implementation in IMPLEMENTATION_SUMMARY.md.

**🚨 MediPulse - Where AI Meets Emergency Response 🚨**
