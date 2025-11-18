# Civil Sunrise Alarm

A native Android application designed to wake you up at or before the break of dawn. This app calculates the time of civil dawn based on your device's location and automatically triggers an alarm at that time each day (with an offset if desired).

## Prerequisites

Before building and installing this app, ensure you have:

- **Android SDK** (API level 34 or higher)
- **Android NDK** (optional, for native libraries)
- **Gradle** 8.x or higher (comes with Android Studio)
- **JDK 17** or higher
- **Android device** running Android 8.0 (API 26) or higher, or an Android emulator
- **USB cable** (for device installation)

## Building the App

### 1. Clone or Navigate to the Project

```bash
cd /path/to/civil_sunrise_alarm
```

### 2. Build a Release APK

The project includes a `justfile` with build recipes. To build a release APK:

```bash
just build-release
```

This command will:
- Clean previous builds
- Compile the Kotlin source code
- Resolve dependencies
- Build an optimized, signed release APK
- Output the APK to `android/app/build/outputs/apk/release/app-release.apk`

**Alternative (using Gradle directly):**

```bash
cd android
./gradlew clean build
```

### 3. Build a Debug APK (Optional)

For development and testing:

```bash
cd android
./gradlew assembleDebug
```

The debug APK will be output to `android/app/build/outputs/apk/debug/app-debug.apk`

## Installing on an Android Device

### Prerequisites for Installation

1. **Enable USB Debugging** on your Android device:
   - Go to **Settings** → **About phone**
   - Tap **Build number** 7 times to enable Developer mode
   - Go back to **Settings** → **Developer options**
   - Enable **USB Debugging**

2. **Connect via USB**:
   - Connect your Android device to your computer using a USB cable
   - Select "File Transfer" or "Android System Integration" when prompted on your device

### Installation Methods

#### Option 1: Using Android Debug Bridge (ADB)

```bash
adb install android/app/build/outputs/apk/release/app-release.apk
```

#### Option 2: Using Gradle

From the `android` directory:

```bash
./gradlew installRelease
```

Or using the `justfile`:

```bash
just install-release
```

#### Option 3: Manual Installation (File Manager)

1. Transfer the APK file to your device
2. Open your device's file manager
3. Locate and tap the `app-release.apk` file
4. Select "Install" when prompted

### Verifying Installation

Once installed, you should see the "Civil Sunrise Alarm" app icon on your home screen or app drawer. Launch the app to begin using it.

## Troubleshooting

### Build Issues

- **Gradle sync fails**: Ensure you have JDK 17 installed and set `JAVA_HOME` correctly
- **Dependency resolution errors**: Run `./gradlew build --refresh-dependencies`
- **Out of memory during build**: Increase Gradle heap size in `gradle.properties`

### Installation Issues

- **APK installation fails**: Ensure USB Debugging is enabled and your device is connected
- **"App not installed" error**: Your device may not support API level 26 or higher. Check your device's Android version
- **Device not recognized**: Install USB drivers for your device model or restart the ADB daemon: `adb kill-server && adb start-server`

### Runtime Permissions

The app requires the following permissions:

- **Location** (Fine & Coarse): To determine your location for accurate civil dawn calculations
- **Alarm & Reminder**: To set and trigger the alarm
- **Notifications**: To send alarm notifications
- **Wake Lock & Full Screen Intent**: To turn on the screen and display the full-screen alarm

Grant these permissions when prompted by the app during first use.

## Development

### Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/democ/civilsunrisealarm/  (Kotlin source files)
│   │   │   └── res/                               (Resources: layouts, strings, drawables)
│   │   └── test/                                  (Unit tests)
│   └── build.gradle.kts                           (App-level Gradle configuration)
├── gradle/                                         (Gradle wrapper)
└── settings.gradle.kts                            (Root Gradle settings)
```

### Key Technologies

- **Kotlin**: Primary language
- **Jetpack Compose**: Modern UI framework
- **Hilt**: Dependency injection
- **DataStore**: Persistent preferences
- **WorkManager**: Background task scheduling
- **Play Services Location**: GPS location services
- **AlarmManager**: System-level alarm scheduling

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or contributions, please open an issue in the project repository.

