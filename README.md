# Stack Ball 3D - Android Game

A high-performance 3D Helix Stack Ball arcade game built for Android using modern Jetpack Compose, Kotlin Coroutines, and a custom 3D hardware-accelerated Canvas engine.

---

## 🎮 Key Features

- **1000+ Infinite Progressive Levels:**
  - Dynamically generated procedural levels with diverse shapes (Circles, Hexagons, Stars, Flower petals, Rings, and Multi-shape morphs).
  - Rotation speeds and layer counts scale progressively as you climb levels (levels 100, 500, 1000+).
- **Smooth Full-Screen Adaptive Rendering:**
  - Responsive perspective camera projection automatically scales and centers across phones, foldables, and tablets without aspect ratio distortion.
  - Viewport Frustum Culling ensures zero lag and silky-smooth 60+ FPS even with mega-towers.
- **Dynamic Physics & Tile Shatter Effects:**
  - Central pipe removed; platform segments shatter outwards to the sides away from the screen for optimal user view.
  - **Ball Dip Stamp (छाप):** Every single ball dip leaves an authentic splat imprint right on the tile at the exact point of impact.
- **Procedural Background Arcade Music (BGM):**
  - Synthesized energetic 128 BPM electronic arcade soundtrack at 25% volume.
  - Auto-pauses smoothly on app minimize, back navigation, or incoming interruptions, and automatically resumes when returning to the game.
- **Haptics & Sound FX:**
  - Satisfying bounce, smash, and combo fire explosions with toggleable audio & vibration controls.

---

## 🛠️ Requirements & Tech Stack

- **Android SDK:** Min SDK 24, Target SDK 36, Compile SDK 36.
- **Language:** Kotlin 100%.
- **UI Framework:** Jetpack Compose (Material Design 3).
- **Build System:** Gradle with Kotlin DSL (`build.gradle.kts`).

---

## 🚀 Building & Releasing

### 1. Build via GitHub Actions (Recommended)
Simply push this repository to GitHub! The included `.github/workflows/android-release.yml` automatically builds:
- `StackBall-Release-APK` (`.apk`)
- `StackBall-Release-Bundle` (`.aab`)

You can download the compiled Release APK straight from the GitHub Actions **Artifacts** tab.

### 2. Build Release APK Locally
To build the signed release APK from terminal:

```bash
# 1. Generate release keystore (if not already present):
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload -storepass android -keypass android -dname "CN=StackBall, OU=Game, O=AIStudio, L=MountainView, ST=CA, C=US"

# 2. Build the release APK:
STORE_PASSWORD=android KEY_PASSWORD=android gradle :app:assembleRelease
```

The output Release APK will be created at:
`app/build/outputs/apk/release/app-release.apk`

### 3. Run Unit Tests

```bash
gradle :app:testDebugUnitTest
```
