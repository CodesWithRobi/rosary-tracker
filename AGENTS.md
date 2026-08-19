# RosaryTracker

Empty Android Studio project template. No source code exists — everything built from scratch.

## Project Facts

- **Package:** `com.example.rosarytracker`
- **Language:** Java 11 (compileOptions) — staying with Java
- **AGP:** 9.0.1 | **Gradle:** 9.1.0 | **compileSdk:** 36 | **minSdk:** 24
- **Source dir:** `app/src/main/java/com/example/rosarytracker/` (empty)
- **No CI, no pre-commit, no tests beyond template stubs**

## Build Commands

```bash
./gradlew assembleDebug        # build debug APK
./gradlew assembleRelease      # build release APK
./gradlew lint                 # lint check
./gradlew testDebugUnitTest    # run unit tests (none exist yet)
```

No single-test-run command needed yet. No codegen, no migrations, no special env loading.

## Architecture

Fake music player using Android MediaSession API to track Rosary mystery progress. Appears as media player on lock screen, supports headphone skip buttons.

- **Media3** (ExoPlayer + MediaSession) — lock screen + headphone controls
- **Room** — persist state across restarts
- **XML Layouts + Java** — UI (no Compose)
- **Foreground Service** (`MediaSessionService`) — background playback

### Package Structure

```
com.example.rosarytracker/
├── MainActivity.java              # Main UI
├── data/
│   ├── MysterySet.java            # Enum: JOYFUL, LUMINOUS, SORROWFUL, GLORIOUS
│   ├── RosaryState.java           # Room entity (singleton row)
│   ├── RosaryStateDao.java        # Room DAO
│   └── RosaryDatabase.java        # Room database
├── service/
│   ├── RosaryPlaybackService.java # MediaSessionService (foreground)
│   └── RosaryNotificationManager.java
└── viewmodel/
    └── RosaryViewModel.java       # Bridges UI ↔ Service ↔ DB
```

### Current State

All files are **stubs only** — class definitions and method signatures with TODOs inside. Do NOT implement methods until user says "implement".

## Key Gotchas

- `AndroidManifest.xml` has `<application>` but **no `<activity>`** — app crashes on launch if installed as-is
- Version catalog at `gradle/libs.versions.toml` — add new deps there first
- `local.properties` has SDK path `/home/sec/Android/Sdk` — do not commit
- Project uses `compileSdk { version = release(36) }` syntax (AGP 9+)
