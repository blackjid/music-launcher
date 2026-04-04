# Music Launcher

A custom Android launcher that turns a phone into a **single-purpose family music device for kids**. The phone sits on a landscape wireless charging dock; kids pick songs via Spotify that play on home speakers. When idle and docked, a standby screen shows the time and currently playing track. When undocked, a usage timer limits screen time.

## Objectives

- **Single-purpose device** — only Spotify is accessible, no distractions
- **Kid-friendly** — large touch targets, simple UI, dark theme for bedside/dock use
- **Standby mode** — clock + now playing display when charging and idle
- **Usage timer** — configurable countdown when undocked to prevent extended use
- **Family accounts** — switch between Spotify family plan profiles
- **Kiosk behavior** — no access to other apps, settings, or notifications

## Target Device

- **OnePlus 6T** running LineageOS 22.2 (Android 15)
- OLED display (standby mode uses burn-in prevention)
- Landscape wireless charging dock

## Architecture

- **Kotlin + Jetpack Compose** — single Activity, modern declarative UI
- **Spotify App Remote Library** — controls the installed Spotify app via IPC (Spotify handles streaming, Connect, and casting to speakers)
- **Min SDK 28** (Android 9), Target SDK 35
- Dark theme, landscape-locked

## Roadmap

### Phase 1: Basic Launcher Shell (current)
- Android launcher registered as HOME app
- Landscape dark-themed home screen
- Button to launch Spotify
- Immersive mode (hidden system bars, back button disabled)

### Phase 2: Spotify Integration
- Spotify SDK (App Remote Library) for playback control
- Now playing screen with album art and controls
- Multi-account profile selector for family plan members
- OAuth tokens stored in EncryptedSharedPreferences

### Phase 3: Standby Mode
- Charging + idle detection triggers standby screen
- Large clock + now playing info + album art
- Screen stays on while charging, low brightness
- OLED burn-in prevention (pixel shifting)

### Phase 4: Usage Timer
- Countdown starts when device is undocked (stops charging)
- Visual timer badge on home screen (green > yellow > red)
- Lock screen when timer expires ("please dock the device")
- Timer resets when re-docked

### Phase 5: Polish & Hardening
- Lock Task Mode for true kiosk (requires device owner setup)
- Parent PIN for settings access
- Boot receiver for auto-start
- NFC dock detection (optional)

## Development Setup

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (or Android command-line tools)
- Android SDK Platform 35
- Android Build Tools
- JDK 17+

### Build & Install

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Device Setup (OnePlus 6T / LineageOS)

1. **Enable Developer Options**: Settings > About Phone > tap "Build Number" 7 times
2. **Enable USB Debugging**: Settings > Developer Options > USB Debugging
3. **Connect via USB** and authorize the computer on the phone
4. **Set as default launcher**: After installing, go to Settings > Apps > Default Apps > Home App > select "Music Launcher"

To revert to the original launcher, go to Settings > Apps > Default Apps > Home App and select a different launcher.

## Project Structure

```
app/src/main/java/com/blackjid/musiclauncher/
├── MainActivity.kt              # Single activity, launcher entry point
├── MusicLauncherApp.kt          # Application class
├── ui/
│   ├── theme/                   # Dark theme (colors, typography)
│   ├── navigation/              # Compose navigation
│   ├── screens/                 # HomeScreen, StandbyScreen, etc.
│   └── components/              # Reusable UI components
├── spotify/                     # Spotify SDK integration
├── profile/                     # Family account management
├── device/                      # Charging, orientation, timer monitors
├── kiosk/                       # Kiosk mode management
└── data/                        # DataStore, encrypted token storage
```

## License

TBD
