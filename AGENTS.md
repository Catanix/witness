# AGENTS.md — conventions for contributors and AI agents

Witness — Android + Wear OS voice recorder (Kotlin / Jetpack Compose, Catppuccin Mocha).
Read this before making changes.

## 1. Modules & build

- `:core` — shared engine (library): audio, data, net, pipeline, theme, ui.
- `:mobile` — phone (`applicationId com.witness`, namespace `com.witness.mobile`).
- `:wear` — watch (`applicationId com.witness`, namespace `com.witness.wear`).
- JDK 17, compileSdk 35, minSdk 26 (wear 30). Gradle wrapper included.

```bash
./gradlew :mobile:assembleDevDebug   # dev preset from .env.dev
./gradlew :mobile:assembleGitDebug   # clean, no preset
./gradlew :wear:assembleDebug        # APK: wear/build/outputs/apk/debug/wear-debug.apk
```

- The `dev` flavor embeds creds from `.env.dev` into `BuildConfig`; `git` — empty strings.
- `.env.dev` is git-ignored. **Never commit or print secrets.**

## 2. Architectural invariants (do not break)

- **`RecordingsStore` is a singleton** (`RecordingsStore.get(context)`). The UI (`app.store.items`)
  and the pipeline must see one StateFlow. Do not create `RecordingsStore(...)` directly — the
  constructor is private. Otherwise UI statuses do not update.
- **Settings preset**: `SettingsRepo.preset` is set in `WitnessApp.onCreate` from `BuildConfig`
  BEFORE the first read. The `config` Flow uses the preset as a fallback for missing keys. The
  pipeline creates `SettingsRepo(context)` in core and relies on the companion preset.
- **`applicationId` is identical** (`com.witness`) on phone and watch — the Data Layer routes by
  package + signature. Do not change it.
- Pipeline: `TranscriptionPipeline.run(context, rec, onStage)`, stages
  `RECEIVED → TRANSCRIBING → ANALYZING → DONE/FAILED`. Clients `AsrClient`/`LlmClient` work on `ApiConfig`.
- Watch → phone transfer: `DataItem` `/recording`, keys `ts` (Long), `duration` (Long), WAV asset.
  The phone `WearReceiveService` reads `map.getLong("duration")` — keys are aligned.
- Watch recording — `WearRecordingService` (foreground type microphone + PARTIAL_WAKE_LOCK):
  screen off — recording continues. The UI collects companion StateFlows `recorder/saving/lastSaved`.
- **Auto-delivery**: `SyncQueue.flush` puts the `DataItem` into the GMS data layer **even offline**
  (no connectedNodes gate) — GMS stores the item+asset locally and syncs it to the phone when a
  connection appears. The file is removed from the queue after a successful put. `WatchSyncService`
  (`WearableListenerService`: onCapabilityChanged/onDataChanged) re-triggers flush.

## 3. Design constraints (strict)

- Theme **Catppuccin Mocha**, all dark. Palette in `core/theme/Catppuccin.kt`.
- **Preserve the existing look and behavior.** Edits change logic, not design. Do not invent
  styling; reproduce the approved look from git history and screenshots (`docs/screenshots/`).
- Watch (approved concept v3): idle — gradient mauve-lavender emblem button with a **dark circle**
  inside (no microphone) and two rings, **the whole screen is the tap zone**; recording — large
  timer + full-width equalizer + label; stop — **fullscreen overlay** (dim 0.96) with large
  Pause / Stop and Save / Cancel buttons, **no back hint**; pause — yellow frozen timer; saving —
  instant "Saved · will send automatically"; queue chip at the bottom.
  `wear/MainActivity.kt` is the canonical approved design — **do not rewrite it**.
- Settings — **phone only**. The phone recording screen shows an animated equalizer.
- Distinguish the FAB menu by border/background. UI language EN by default, RU available in settings.
- i18n: `strings.xml` in `values/` and `values-ru/`; switching via `attachBaseContext`
  (SharedPreferences `witness_locale`).

## 4. Emulators & adb (Windows / git-bash)

- adb **always by absolute path**: `/c/tools/android-sdk/platform-tools/adb.exe`.
- Emulators: `emulator-5554` (phone, 1080x2400@420), `emulator-5556` (watch, 454x454).
- Full components: `com.witness/com.witness.mobile.MainActivity`,
  `com.witness/com.witness.wear.MainActivity`.
- Paths for native tools — `C:/...` (MSYS conversion disabled), not `/c/...`.
- Use a dedicated scratch dir for temporary files, not a bare `/tmp`.

## 5. E2E smoke test (verified recipe)

The phone pipeline is verified by **importing audio** (independent of watch pairing):

1. `adb -s emulator-5554 push <wav> /sdcard/Download/dialog.wav`
2. Launch MainActivity → FAB "add" ≈ (951,2208) → "Import audio" ≈ (722,2046).
3. SAF picker: Recent empty → "Show roots" (73,126) → "Downloads" (441,430).
   The file is in **GRID view**: clickable `item_root` ≈ [64,610][529,1211], tap center ≈ (296,910).
   Tapping text bounds misses — take the real clickable from `uiautomator dump`.
4. Wait ~20–25 s → "Transcription ready" notification + LLM title; the detail shows the summary
   and a diarized transcript.
- RU pill in settings ≈ (420,379); gear ≈ (976,200); back ≈ (103,150).
- Take coordinates from `uiautomator dump`, do not guess.

## 6. Pitfalls (verified)

- **Wear `screencap`** produces `__fortify_fatal` in logcat — that is a crash of the emulator's
  system screenshot process, **NOT the app**. The app is alive; re-take the screenshot.
- **play-services-wearable 18.2.0 `NodeClient` has no `addListener`/`NodeListener`** (only
  `getConnectedNodes` + `OnNodeMigratedListener`) — do auto-delivery via the GMS data layer +
  `WearableListenerService`, not a node listener.
- **Emulator pairing (verified live)**: the phone emulator (google_apis, PlayStore.enabled=no) has
  GMS core but **does not open a listener on :5601** — that is opened by the separate phone app
  `com.google.android.wearable.app`. Without it `companionDisconnected=true` and the phone adbd
  gives `Connection refused` on tcp:5601.
  - Correct bridge: `adb -s <phone> forward tcp:5601 tcp:5601` +
    `adb -s <watch> reverse tcp:5601 tcp:5601` (NOT forward on both — host-port conflict).
    After bridging, the watch transport really connects ("Connected, running sync loop as client"
    → "Performing handshake with peer") but drops at the phone end (no listener).
  - The APKPure APK (`d.apkpure.com/b/APK/com.google.android.wearable.app`) is the **watch variant**:
    it requires the shared lib `com.google.android.wearable` → on the phone
    `INSTALL_FAILED_MISSING_SHARED_LIBRARY`. The phone variant is not obtainable headless.
  - Conclusion: live transfer needs a **real paired phone** (Play Store + companion + pairing
    wizard) or an emulator with the phone companion installed. The transfer code is correct: the
    watch transport provably reaches the phone tunnel. A watch without a pair queues recordings
    (chip "Queued: N · auto-send") and puts them in the GMS data layer for auto-delivery on connection.
- **Empty grep ≠ success**: `exit_code 0` from a pipe with grep on empty output means "not found",
  not "works". Pipeline/Asr/Llm tags may be absent in logcat — rely on the UI + notifications.
- **SAF grid view**: the item is an `item_root` card, not a list row.
- `pm clear com.witness` resets DataStore and language (EN by default) — the preset is re-seeded
  from BuildConfig.

## 7. Commits & secrets

- Keep commits focused. Do not commit secrets, build outputs or `local.properties`.
- `.env.dev` (git-ignored) holds dev presets; the release `git` flavor has an empty `BuildConfig`.
  **Never commit or print secrets.**
