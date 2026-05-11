# Music Launcher — Agent Guide

A single-purpose Android music kiosk app for a kid-friendly Spotify device. A OnePlus 6T sits on a landscape wireless charging dock; the app provides a full-screen Now Playing interface that controls Spotify (playing on home speakers via Spotify Connect).

## Java / Build Environment

Java comes from Android Studio's bundled JBR. A `.mise.toml` at the project root sets `JAVA_HOME` automatically when using [mise](https://mise.jdx.dev/). If mise is active, all gradle commands work as-is:

```bash
./gradlew installDebug
./gradlew :app:compileDebugKotlin
```

Without mise, prefix every gradle command:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
```

The connected device is a **OnePlus 6T (A6013)** running LineageOS 22.2 (Android 15, API 35). `./gradlew installDebug` builds and installs directly over USB.

## Architecture

- **Kotlin + Jetpack Compose** — single Activity, fully declarative UI, landscape-locked
- **Spotify App Remote SDK 0.8.0** — IPC to the installed Spotify app for playback control and album art (delivered as `Bitmap`)
- **Spotify Web API** — REST calls for device listing, playback transfer, and polling current state
- **Min SDK 28**, Compile/Target SDK 35

```
app/src/main/java/com/blackjid/musiclauncher/
├── MainActivity.kt               # Launcher entry point, OAuth redirect handler
├── MusicLauncherApp.kt           # Application class; wires up all singletons
├── data/
│   ├── EncryptedTokenStore.kt    # EncryptedSharedPreferences for OAuth tokens
│   └── SettingsStore.kt          # SharedPreferences for user settings
├── profile/
│   ├── Profile.kt                # Data class (id, name, avatarColor)
│   └── ProfileRepository.kt      # Single account: save/load/refresh token
├── spotify/
│   ├── SpotifyManager.kt         # App Remote connection + playerState StateFlow
│   ├── SpotifyPlayerState.kt     # MusicPlayerState(trackName, artistName, albumArt Bitmap, …)
│   ├── PlaybackPoller.kt         # Polls Web API every 5s; exposes allPlaybackStates
│   ├── SpotifyWebApi.kt          # Web API calls (current playback, devices, transfer, skip)
│   ├── SpotifyTokenManager.kt    # PKCE token exchange and refresh
│   ├── SpotifyUserApi.kt         # GET /v1/me
│   ├── SpeakerMonitor.kt         # Detects phone speaker playback; fires warning after timeout
│   ├── SpotifyDevice.kt          # Data class for available playback devices
│   └── WebPlaybackState.kt       # Parsed Web API playback state
├── kiosk/
│   └── KioskManager.kt           # Lock Task Mode helpers
└── ui/
    ├── theme/                    # Color.kt, Theme.kt, Type.kt
    ├── navigation/
    │   └── AppNavigation.kt      # NavHost: NOW_PLAYING (root) + SETTINGS
    ├── screens/
    │   ├── NowPlayingScreen.kt   # Main (and only) screen
    │   └── SettingsScreen.kt     # Account management + speaker timeout picker
    ├── components/
    │   ├── AccountPlaybackCard.kt
    │   ├── NowPlayingBar.kt
    │   └── PlaybackControls.kt
    └── overlay/
        └── SpeakerWarningActivity.kt  # Full-screen warning when playing on phone speaker
```

## Current Feature State

- **NowPlayingScreen is the root** — no standby/home screen; the app opens directly to Now Playing
- **Single account** — `ProfileRepository` holds one Spotify profile; first-run shows a "Connect Spotify" button
- **Album art** — delivered as `Bitmap` from App Remote, displayed with animated scale on track change
- **Blurred background** — album art rendered full-screen at 32dp blur behind a dark scrim (API 31+, hardware-accelerated); crossfades on track change (700ms bg, 500ms art)
- **Playback controls** — skip prev/next and play/pause via App Remote IPC
- **Progress bar** — local 1s tick while playing for smooth updates
- **Switch device** — fetches available Spotify devices via Web API; pill turns amber when playing on phone speaker
- **Phone speaker warning** — `SpeakerMonitor` pauses playback and launches `SpeakerWarningActivity` after a configurable timeout
- **Tap-to-reveal settings** — tapping anywhere shows a settings cog in the bottom-right corner; auto-hides after 3s
- **Nothing playing state** — "Nothing playing" headline + "Open Spotify" button

## Design System

Defined in `ui/theme/Color.kt`:

| Token | Value | Use |
|---|---|---|
| `BgPrimary` | `#0A0A0A` | Main background |
| `BgSecondary` | `#1A1A1A` | Cards, pills |
| `BgTertiary` | `#2A2A2A` | Progress track, device rows |
| `FgPrimary` | `#FFFFFF` | Headlines |
| `FgSecondary` | `#A1A1AA` | Secondary text |
| `FgMuted` | `#71717A` | Labels, icons |
| `AccentPurple` | `#A855F7` | Progress bar, play button, glow |
| `ActiveRowBg` | `#1E1628` | Active device row |
| `SpotifyGreen` | `#1DB954` | Spotify-branded buttons |

## Key Patterns

- **App Remote album art** — `spotifyManager.playerState` emits `MusicPlayerState`; `albumArt: Bitmap?` is set via `imagesApi.getImage()` in `SpotifyManager`. This is the authoritative source for the foreground and blurred background.
- **Web API token** — always call `profileRepository.getValidToken(profileId)` which auto-refreshes if expired.
- **Navigation** — only two routes: `Routes.NOW_PLAYING` (start) and `Routes.SETTINGS`.
- **No queue access** — the Spotify App Remote SDK does not expose the next track; the Web API `/v1/me/player/queue` endpoint is not currently called.
