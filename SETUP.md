# Setup Instructions for Solar Monitor KMP

This guide will help you set up the project on your Mac and connect it to GitHub.

## Prerequisites Installation

### 1. Install Android Studio
```bash
# Download from: https://developer.android.com/studio
# Or use Homebrew:
brew install --cask android-studio
```

### 2. Install Java 17 (if not already installed)
```bash
brew install openjdk@17
```

### 3. Set up Android SDK
1. Open Android Studio
2. Go to **Settings → Appearance & Behavior → System Settings → Android SDK**
3. Install:
   - Android SDK Platform 34
   - Android SDK Platform-Tools
   - Android SDK Build-Tools 34.0.0

## Project Setup

### Step 1: Connect to GitHub

1. **Create a new repository on GitHub**
   - Go to https://github.com/new
   - Name it: `SolarMonitorKMP`
   - Don't initialize with README (we already have one)
   - Click "Create repository"

2. **Connect your local repository to GitHub**
```bash
cd /path/to/SolarMonitorKMP

# Add GitHub as remote
git remote add origin https://github.com/YOUR_USERNAME/SolarMonitorKMP.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### Step 2: Open Project in Android Studio

1. Launch Android Studio
2. Click **"Open"**
3. Navigate to `SolarMonitorKMP` folder
4. Click **"Open"**
5. Wait for Gradle sync to complete (this may take a few minutes)

### Step 3: Configure Device IP Range

If your solar devices are on a different IP range:

1. Open `shared/src/commonMain/kotlin/com/solarmonitor/data/repository/SolarDeviceRepositoryImpl.kt`
2. Modify these lines:
```kotlin
private val ipBase = "10.10.100"  // Change to your network
private val ipRange = 250..254     // Change scan range
```

### Step 4: Test on Android Emulator

1. In Android Studio, click **"Device Manager"** (phone icon)
2. Create a new virtual device:
   - Click **"Create Device"**
   - Select **"Pixel 6"** or similar
   - Select **"API 34"** system image
   - Click **"Finish"**
3. Run the app: Click the green **"Run"** button (▶️)

### Step 5: Test on Physical Android Device

1. **Enable Developer Options** on your Android phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect via USB**:
   - Connect phone to Mac with USB cable
   - Accept debugging permission on phone
   - Phone should appear in Android Studio device list

3. **Run the app** on your phone

## Network Configuration

### Connecting to Solar Devices

Your solar devices must be on the same WiFi network as your Android device/emulator.

**For Emulator:**
- Emulator uses your Mac's network connection
- Devices at `10.10.100.x` should be accessible

**For Physical Device:**
- Connect phone to same WiFi as solar devices
- Ensure no firewall blocking port 8893

### Testing Without Real Devices

To test the app without actual solar hardware:

1. Use a Modbus TCP simulator
2. Or modify discovery to use localhost:
```kotlin
private val ipBase = "127.0.0"
private val ipRange = 1..1
```

## Firebase Setup (Optional - for Cloud Sync)

### 1. Create Firebase Project

1. Go to https://console.firebase.google.com
2. Click "Add Project"
3. Name it "Solar Monitor"
4. Disable Google Analytics (optional)
5. Click "Create Project"

### 2. Add Android App to Firebase

1. In Firebase Console, click "Add App" → Android
2. Enter package name: `com.solarmonitor.android`
3. Download `google-services.json`
4. Place it in `androidApp/` directory

### 3. Enable Firestore

1. In Firebase Console, go to "Firestore Database"
2. Click "Create Database"
3. Start in **test mode** (for development)
4. Select a location close to you
5. Click "Enable"

### 4. Uncomment Firebase Code

In `SolarDeviceRepositoryImpl.kt`, uncomment:
```kotlin
// Optionally sync to cloud
syncToCloud(deviceId, data)
```

Then implement the Firebase sync logic in Android-specific code.

## Building and Running

### Debug Build
```bash
./gradlew :androidApp:assembleDebug
```

### Release Build (for distribution)
```bash
./gradlew :androidApp:assembleRelease
```

### Install on connected device
```bash
./gradlew :androidApp:installDebug
```

## Troubleshooting

### Gradle Sync Failed
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Android Studio Can't Find JDK
1. Go to Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Set "Gradle JDK" to Java 17

### Emulator Network Issues
- Emulator might not access local network properly
- Use physical device for testing with actual hardware

### Port 8893 Connection Refused
- Check device is powered on
- Verify IP address is correct
- Ensure no firewall blocking the port
- Try disabling TLS: `ModbusClient(device, useTLS = false)`

## Development Workflow

### 1. Create a new branch for features
```bash
git checkout -b feature/new-feature-name
```

### 2. Make changes and commit
```bash
git add .
git commit -m "Add new feature"
```

### 3. Push to GitHub
```bash
git push origin feature/new-feature-name
```

### 4. Create Pull Request on GitHub
- Go to your repository on GitHub
- Click "Pull Requests" → "New Pull Request"
- Review changes and merge to main

## Next Steps

1. ✅ Run the app and test device discovery
2. ✅ Connect to your solar monitoring hardware
3. ✅ Verify data is displaying correctly
4. 🔄 Add Firebase for cloud storage
5. 🔄 Implement historical data charts
6. 🔄 Add configuration UI
7. 🔄 Build iOS version

## Getting Help

- **Build errors**: Check Android Studio's "Build" tab
- **Runtime errors**: Check Logcat in Android Studio
- **Network issues**: Use Wireshark to inspect Modbus packets
- **GitHub issues**: Create an issue in the repository

## Useful Commands

```bash
# View Git status
git status

# View commit history
git log --oneline

# Discard changes
git checkout -- .

# Update from GitHub
git pull origin main

# View all branches
git branch -a

# Switch branch
git checkout branch-name
```

---

**Happy Coding! 🚀☀️**

For questions, check the main README.md or create a GitHub issue.
