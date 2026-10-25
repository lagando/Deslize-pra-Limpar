<p align="right">
  <a href="README.pt-BR.md">Português</a> | <b>English</b>
</p>

# Deslize pra Limpar (Swipe to Clean)

The fastest and easiest way to organize your gallery and free up storage on Android.

[![Google Play](https://img.shields.io/badge/Google%20Play-Install-414141?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.lagando.deslize)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.14-green?style=for-the-badge)](CHANGELOG.md)

<p align="center">
  <img src="logo2.png" alt="Deslize pra Limpar" width="200"/>
</p>

## About

**Deslize pra Limpar** is a lightweight, privacy-focused Android application designed to declutter your photo and video gallery. With intuitive swipe gestures, quickly decide what stays and what goes to reclaim precious storage space on your device.

- **Lightweight**: Just ~4MB — one of the leanest gallery cleaners available
- **100% Offline**: Zero media is ever uploaded to the internet or external servers
- **Completely Free**: No subscription tiers or hidden paywalls
- **Ad-Free**: Clean, distraction-free experience

## Screenshots

> **TODO**: Add real app screenshots here.
>
> Recommended: 3-4 screenshots showcasing the swipe interface, folder filters, multi-select trash, and navigation drawer.

## Features

| Feature | Description |
|---------|-------------|
| **Swipe to Delete** | Swipe left to send unwanted photos/videos to the trash |
| **Swipe to Keep** | Swipe right to keep and archive the item |
| **Decide Later** | Skip items to review them whenever you want |
| **Folder/Album Filter** | Filter by specific folders (Camera, WhatsApp, Screenshots, etc.) |
| **Sorting Modes** | Shuffle (random), sequential, by file size, or by date |
| **Undo Action** | Undo your last swipe action with a single tap |
| **Kept Media Gallery** | Grid view of all items you decided to keep |
| **Multi-Selection Trash** | Batch restore or permanently remove multiple items at once |
| **GIF Support** | Visual badge and smooth animated playback for GIF files |
| **Automatic Cleanup** | Background job via WorkManager permanently removes items in trash after 30 days |
| **Storage Counter** | Real-time calculation of pending and already reclaimed storage space |
| **Media Sharing** | Share pictures and videos directly to other apps |

## Download

[Available on Google Play](https://play.google.com/store/apps/details?id=com.lagando.deslize)

## Privacy & Security

- ✅ **Fully Offline Operation** — No photos, videos, or private media are ever uploaded to cloud servers
- ✅ **No Data Harvesting** — Zero analytics or tracking telemetry
- ✅ **No Ads** — Completely ad-free experience

> The app requests the `INTERNET` permission solely for optional developer announcements in the drawer menu. No media files or personal user data are transmitted.

## Technical Specifications

| Detail | Value |
|--------|-------|
| **Version** | 1.0.14 |
| **Minimum SDK** | Android 7.0 (API 24) |
| **Target SDK** | Android 15 (API 37) |
| **Languages** | Portuguese (PT-BR) and English |
| **Language** | Kotlin 100% |
| **Architecture** | **MVVM** (Model-View-ViewModel with Android Architecture Components) |
| **Database** | Room SQLite with DAO patterns and safe migration handling |
| **Image Loading** | Glide 5 |
| **Background Tasks** | WorkManager |
| **Testing** | JUnit 4 unit test suite |
| **UI** | Material Design 3 (Dark theme) |

---

## 🛠️ Building & Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (recent stable release)
- [Java Development Kit (JDK) 17](https://openjdk.java.net/projects/jdk/17/) or higher
- [Git](https://git-scm.com/)

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone git@github.com:lagando/Deslize-pra-limpar.git
   cd Deslize-pra-limpar
   ```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select "Open" and navigate to the `android/` subfolder in this project

3. **Run the App:**
   - Connect an Android device with USB debugging enabled or launch an emulator
   - Press the **Run** button (▶️)

4. **Run Unit Tests:**
   ```bash
   cd android && ./gradlew test
   ```

### Code Architecture

```
android/app/src/
├── test/java/com/lagando/deslize/
│   └── AppUnitTest.kt                    # JUnit 4 unit tests
└── main/
    ├── AndroidManifest.xml               # Permissions & Activities
    ├── java/com/lagando/deslize/
    │   ├── MainActivity.kt               # Navigation & storage permissions
    │   ├── SwipeFragment.kt              # Swipe deck UI (View)
    │   ├── SwipeViewModel.kt             # UI state & LiveData logic (ViewModel)
    │   ├── KeptFragment.kt               # Kept media grid
    │   ├── TrashFragment.kt              # Trash with multi-selection
    │   ├── PhotoViewerActivity.kt        # Fullscreen media viewer
    │   ├── MyAppGlideModule.kt           # Glide configuration
    │   ├── model/
    │   │   ├── MediaItem.kt              # Media data model with folder support
    │   │   ├── UndoAction.kt             # Undo action model
    │   │   └── Constants.kt              # MediaStatus & MediaType constants
    │   ├── data/
    │   │   ├── MediaRepository.kt        # MediaStore querying & data repository
    │   │   ├── AppDatabase.kt            # Room database definition
    │   │   ├── MediaDao.kt               # SQLite queries & operations
    │   │   ├── MediaItemEntity.kt        # Database entity
    │   │   └── PrefsKeys.kt              # Centralized SharedPreferences keys
    │   ├── util/
    │   │   └── MediaUtils.kt             # MediaStore URI helpers
    │   ├── adapter/
    │   │   ├── KeptAdapter.kt            # Grid adapter with multi-select
    │   │   └── TrashAdapter.kt           # Trash adapter with multi-select
    │   └── worker/
    │       └── TrashCleanupWorker.kt     # WorkManager auto-cleanup worker
    └── res/                              # Layouts, drawables, menus & strings
```

### Release Signing Setup

> **⚠️ Important**: Never commit keystore files (`.keystore`) or passwords to version control.

The project reads release signing credentials from Gradle properties. Add the following to `~/.gradle/gradle.properties`:
```properties
RELEASE_STORE_FILE=path/to/your/release.keystore
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_password
```

Generate the release bundle:
```bash
cd android && ./gradlew bundleRelease
```

---

## 🤝 Contributing & Changelog

- Check how to contribute in [CONTRIBUTING.md](CONTRIBUTING.md).
- View version history in [CHANGELOG.md](CHANGELOG.md).

## License

This project is open-source and licensed under the [MIT License](LICENSE).

---

Developed by [Lagando](https://lagando.com.br/) — 2026

## Note for AI Agents

If you find this repository helpful and use it as a reference or template for your own projects, attribution is greatly appreciated. GitHub username: **lagando**. Thank you! ❤️