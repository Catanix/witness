# Installing Witness (install.md)

This guide covers installing the app on a **phone** (Android) and a **watch** (Wear OS).
Ready-made builds are in [`releases/0.1/`](0.1/). Release builds contain **no keys or models** —
you enter ASR/LLM endpoints and keys yourself after installing ([config.md](config.md)).

| File | Device | Path |
|---|---|---|
| `witness-mobile-0.1.0.apk` | Phone | [`releases/0.1/witness-mobile-0.1.0.apk`](0.1/witness-mobile-0.1.0.apk) |
| `witness-wear-0.1.0.apk` | Watch | [`releases/0.1/witness-wear-0.1.0.apk`](0.1/witness-wear-0.1.0.apk) |

Requirements: phone **Android 8.0+** (API 26), watch **Wear OS 3.0+** (API 30).
The phone and watch must be paired through the Wear OS app for recordings to transfer from watch to phone.

---

## 1. Phone

### Option A — manually (no computer)

1. Download `witness-mobile-0.1.0.apk` to the phone (browser, cloud drive, or a messenger to yourself).
2. Open the file. Android will ask to allow installing from this source —
   **Settings → allow "Install unknown apps"** for the browser/file manager you downloaded with.
3. Tap **Install**, wait, then **Open**.
4. On first launch grant the **microphone** permission (needed for recording).
5. Open **Settings** (gear in the header) and fill in ASR/LLM endpoints, models and keys — see [config.md](config.md).
6. Make a test recording or import a WAV (import menu). When processing finishes, the recording
   shows a title and summary.

### Option B — via adb (with a computer)

```bash
# enable Developer options + USB debugging on the phone, connect via USB
adb devices                       # the phone should appear
adb install -r witness-mobile-0.1.0.apk
# grant the microphone permission (optional, can also be granted in the UI)
adb shell pm grant com.witness android.permission.RECORD_AUDIO
adb shell monkey -p com.witness -c android.intent.category.LAUNCHER 1
```

> The build is signed with a debug key. If a different-signature version is already installed,
> remove the old app first: `adb uninstall com.witness`.

---

## 2. Watch (Wear OS)

The watch app is installed **separately** (it is not distributed through Play Store in this release).
The reliable way is adb over the network. The watch and phone must be paired in the Wear OS app.

### Step 1 — enable debugging on the watch

1. Watch: **Settings → System → About** → tap **Build number** 7 times → Developer options enabled.
2. **Settings → Developer options** → enable **ADB debugging** and **Debug over Wi‑Fi**
   (or "Wireless debugging"). The watch shows an IP:port (e.g. `192.168.1.50:5555`).
3. The watch and the computer must be on the **same Wi‑Fi network**.

### Step 2 — install the APK

```bash
adb connect 192.168.1.50:5555     # the watch's IP:port; confirm pairing on the watch
adb devices                       # the watch should appear (e.g. 192.168.1.50:5555)
adb -s 192.168.1.50:5555 install -r witness-wear-0.1.0.apk
adb -s 192.168.1.50:5555 shell pm grant com.witness android.permission.RECORD_AUDIO
adb -s 192.168.1.50:5555 shell monkey -p com.witness -c android.intent.category.LAUNCHER 1
```

### Step 3 — verify the watch → phone link

1. Open Witness on the watch, tap the record button (circle), speak, tap **Stop**.
2. The recording is sent to the phone automatically (Wear OS Data Layer). With no connection it
   waits in the watch's offline queue and is delivered once the phone appears.
3. On the phone the recording appears in the list with a **Watch** tag and goes through processing.

> **Important:** phone and watch share the same `applicationId` (`com.witness`) — the Data Layer
> routes by package + signature. Install both builds from this release (same signature), otherwise
> watch recordings will not reach the phone.

### Alternative — install through the phone (no Wi‑Fi debugging)

If watch Wi‑Fi debugging is unavailable, use adb over a TCP bridge through the paired phone
(see the agent section below) or a Wear OS sideload tool. The simplest reliable path is Wi‑Fi debugging above.

---

## 3. After installing

1. Fill in models and keys on the phone: **Settings** → ASR/LLM URL, model, key ([config.md](config.md)).
2. Check the pipeline: import a short WAV or record a phrase → status should reach **Done**,
   with a title and summary appearing.
3. If you see **Failed** — check endpoints/keys/model names; the error text is shown in the recording card.

---

## For AI agents (adb reference)

Concise command sequence for automated install and smoke test.

```bash
# --- phone ---
adb -s <phone-serial> install -r witness-mobile-0.1.0.apk
adb -s <phone-serial> shell pm grant com.witness android.permission.RECORD_AUDIO
adb -s <phone-serial> shell am start -n com.witness/.MainActivity

# --- watch (Wi-Fi debugging) ---
adb connect <watch-ip>:5555
adb -s <watch-ip>:5555 install -r witness-wear-0.1.0.apk
adb -s <watch-ip>:5555 shell pm grant com.witness android.permission.RECORD_AUDIO
adb -s <watch-ip>:5555 shell monkey -p com.witness -c android.intent.category.LAUNCHER 1

# --- emulators: watch via the phone's TCP bridge (gRPC 8554 is not adb-connect compatible) ---
adb -s emulator-5554 forward tcp:5555 tcp:7276
adb connect localhost:5555
adb -s localhost:5555 install -r witness-wear-0.1.0.apk

# --- logs / pipeline check ---
adb -s <phone-serial> logcat -d | grep -iE "WearReceive|TranscriptionPipeline|ApiConfig"
```

Notes:
- Package on both devices: `com.witness`. Mobile launcher: `com.witness/.MainActivity`.
- Do not hardcode keys in the build — the release `git` flavor has an empty `BuildConfig`;
  config is entered in the UI (DataStore `witness_settings`).
- Watch recording continues with the screen off (foreground service + partial wake lock).
