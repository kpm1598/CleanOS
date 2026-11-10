# CleanOS — Milestone 1

A native Android storage cleaner for personal use (sideloaded APK, no account, no ads,
no backend, no internet needed). Kotlin + Jetpack Compose + Material 3, MVVM, Room,
MediaStore and the Storage Access Framework.

Target device: OnePlus Nord CE 3 Lite 5G · Package: `com.cleanos.storage` · minSdk 29, targetSdk 34.

## What Milestone 1 does

- **Dashboard** — real whole-device storage (total / used / available / % used, via StatFs),
  clearly separated from the *files CleanOS can actually access* (scanned count, size,
  per-category breakdown, last scan time).
- **Permission onboarding** — explains why access is needed, then offers, in order:
  All-files access (`MANAGE_EXTERNAL_STORAGE` via Android Settings, optional but best
  coverage), standard media permissions (Android 13+ `READ_MEDIA_*`), and a SAF folder
  picker. The app works with whichever access you grant and degrades gracefully.
- **Category selection** — Photos, Videos, PDFs, Documents, Audio, APKs, Archives, Other,
  with select all / deselect all. Classification uses MIME type first, then extension.
- **Real scanner** — MediaStore Files collection + the SAF folder you picked, on
  background coroutines, with progress, files-found count, current category and cancel.
  Results are de-duplicated (URI, then path), only metadata is read (never file contents),
  protected `Android/data` and `Android/obb` directories are skipped, and results are
  stored in Room, so they survive restarts.
- **Swipe-to-clean** — one card per file with preview (photos/videos), name, type, size,
  modified date and location. Swipe **left** = queue for deletion (red DELETE indicator),
  swipe **right** = keep (green KEEP indicator), **tap** = open in the system viewer.
  Buttons are provided too. Swiping **never deletes anything** — it only records a
  decision in Room. **Undo** reverses the last decision, **Pause/Resume** is persisted,
  and kept files never reappear in the same session unless you reset it.
- **Keep list** — everything you kept; can be previewed or moved back to review.
- **Delete queue** — count + total size, per-file select/deselect, select all/deselect
  all, remove-from-queue, clear queue, preview, and a final confirmation dialog showing
  the exact file count and total size before anything happens.
- **Deletion (user-confirmed only)** — tries a direct file delete (with all-files access),
  then MediaStore delete, then SAF document delete. Files Android protects are handed to
  the **system deletion dialog** (`MediaStore.createDeleteRequest`, Android 11+). Every
  result is verified afterwards and reported honestly — deleted count/size and per-file
  failure reasons. Kept files are never deleted. After deletion, the database, scan
  results and storage statistics are refreshed.
- **Safety** — no automatic deletion, no system files, no "old = junk" assumptions,
  no silent success claims, CleanOS's own database/app data is never scanned.

## Project layout

```
app/src/main/java/com/cleanos/storage/
  core/permissions/   PermissionManager (all-files access, media permissions)
  core/scanner/       StorageScanner (MediaStore + SAF), FileCategory
  core/storage/       StorageStatsProvider (StatFs)
  core/utils/         Formatters, FileOpener
  data/database/      Room database + DAO
  data/models/        ScannedFile entity, Decision
  data/repositories/  FileRepository, AppPrefs
  domain/cleaner/     DeleteFilesUseCase
  features/dashboard|onboarding|categories|scanner|swipecleaner|deletequeue/
  navigation/         NavHost (onboarding → dashboard → categories → scanning → swipe → queue/keep)
  ui/components/  ui/theme/   (premium dark theme: bg #101522, card #1C2534, primary #567FE7,
                               delete #E66B71, keep #53C99A, text #FFFFFF / #A9B5C9)
app/src/test/         Unit tests for category classification + size formatting
```

## Build on Windows (PowerShell)

Requirements: Android Studio (Ladybug or newer) with an Android SDK (API 34).
**Build from Android Studio — this is the primary path.** The project ships without
`gradlew.bat` / `local.properties` on purpose: Android Studio creates both when it
first syncs the project, so do not look for them before opening it.

1. Extract the zip into a new folder, e.g. `C:\Users\patel\Desktop\CleanOS`
   (the zip is flat — its files land directly in that folder).
2. Open that folder in Android Studio, let the Gradle sync finish, then
   **Run ▶** (or Build → Build App Bundle(s) / APK(s) → Build APK(s)).
3. APK output: `app\build\outputs\apk\debug\app-debug.apk`

Command-line build (only after Android Studio has synced the project once, so
`gradlew.bat` and `local.properties` exist):

```powershell
cd C:\Users\patel\Desktop\CleanOS
.\gradlew.bat assembleDebug          # debug APK
.\gradlew.bat testDebugUnitTest      # unit tests (optional)
```

If Android Studio asks where the SDK is, point it at e.g.
`C:\Users\patel\AppData\Local\Android\Sdk` — or copy `local.properties.example`
to `local.properties` and set `sdk.dir` there.

**Not yet verified:** the project was reviewed file-by-file but has not been
compiled — the sandbox used to build it cannot start a Gradle daemon, so the
first Android Studio build on your laptop is the real compile check. If it
fails, paste the first error block from the Build output and it will be fixed
from that exact error, not guessed at.

## Install on the OnePlus Nord CE 3 Lite

Option A — Android Studio: Run ▶ with the phone connected (USB debugging on).

Option B — adb from PowerShell:

```powershell
# one-time: phone Settings → About → tap Build number 7× → Developer options → USB debugging ON
adb devices                       # phone must appear as "device"
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Option C — sideload: copy `app-debug.apk` to the phone and open it; allow
"Install unknown apps" for your file manager when Android asks.

## First run / testing checklist

1. First launch shows the permission screen. Grant **All-files access** (Settings →
   "Allow access to manage all files") for full coverage, or just media permissions
   for a media-only scan.
2. Dashboard → **Start Scan** → pick categories → **Start scanning**. Watch progress;
   Cancel works mid-scan.
3. Review: swipe a few files left and right, test **Undo**, **Pause**, leave the app
   and reopen — decisions are still there.
4. Open the **Delete queue**, deselect one file, confirm deletion on a throwaway file
   first (e.g. a screenshot you don't need). Android may show its own deletion dialog
   for media it protects — approving it is what actually deletes those files.
5. Back on the dashboard, storage figures and the scanned list refresh.

Known Android limitations, handled rather than hidden:
- Without all-files access, only media (and your SAF folder) can be scanned/deleted —
  that's an Android restriction, not an app choice.
- Files owned by other apps may require the system deletion dialog on Android 11+.
- CleanOS can never touch other apps' private data or system files; Android forbids it.
