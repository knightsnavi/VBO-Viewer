# VBO-Viewer

Lets you view and browse your VBO vehicle data files — a single-page web app for Racelogic VBOX `.vbo` logs. No install, no server, no upload; everything runs in your browser.

**Features**

- Track map coloured by speed, with the start/finish gate drawn on it
- Speed, lateral/longitudinal G (derived from GPS speed and heading), and height traces
- Lap timing from the file's `[laptiming]` start line, with best-lap highlighting and per-lap max/avg speed and peak lateral G
- Click a lap to isolate it on the map and charts; switch the X axis between time and distance
- Colour the racing line by speed or by lap — lap colouring makes line variation between laps obvious at a glance
- Hover the charts or the map to see every channel at that instant, including the matching VBOX video timestamp
- "Set start/finish at cursor" to place a gate on files that have no lap line
- Replay with play/pause, scrub, 0.25×–4× speed, and a chase cam that keeps the car centred
- A heading-up satellite inset beside the G-G gauge — the track immediately around the car, rotated so you are always driving upward, with a rotating north marker
- **Fly-by** — a driver's-eye view built by projecting satellite imagery onto the ground plane and rotating it to your heading, so you see the real track surface with the racing line running ahead into the next corner
- Synchronised video: load the VBOX `.mp4` recordings and the footage tracks the data both ways, with a speed/G overlay — **use Safari for this, see below**
- Basemap switcher — satellite imagery, OSM street, or dark

## Keyboard

`space` play/pause · `←`/`→` step one sample (hold shift for ten) · `home`/`end` jump to the ends of the current view

## Use

Open `index.html` (or the GitHub Pages site) and drop a `.vbo` file onto the page, or click **Load sample**.

## Notes on the format

The fly-by view needs no 3D model and no extra imagery: it is the same aerial tiles as the map, tilted with a CSS 3D transform and spun to a speed-weighted smoothing of the GPS heading. It works because the circuit is flat — the trick degrades on tracks with real elevation change, since aerial imagery carries no height.

The `avitime` column is the position in the recording, in milliseconds, for every logged row, so video sync needs no manual alignment. Your video never leaves the browser. If a clip does not cover the span the log points at, the viewer says so and falls back to free-running playback instead of stalling.

### Known issue: video playback on Chrome (macOS)

**Video sync works in Safari. It does not currently work in Chrome on macOS — use Safari when you want footage.** Everything else in the app works in both.

Chrome is not failing to decode the file. `chrome://media-internals` shows it selecting the hardware `VideoToolboxVideoDecoder`, reporting the correct H.264 High profile and 1920x1080 size, and completing seeks through `kSeeking -> kPlaying` normally. Frames are decoded and then never reach the screen; the element stays black while `totalVideoFrames` stays at zero. Painting frames to a canvas with `drawImage` instead of displaying the video element did not help either, which rules out simple compositing of the element itself.

Unresolved. Anyone picking this up should start from `chrome://media-internals` rather than the codec, which is a dead end — the file is an ordinary H.264/AAC MP4 that plays everywhere else.

`.vbo` files are plain text. Latitude and longitude are stored in **minutes**, with positive longitude meaning **West**. Time is UTC `HHMMSS.ss`. The `[laptiming]` line gives two points; this viewer anchors the gate at their midpoint and lays it across the track, 50 m wide, perpendicular to the direction of travel at that spot.

Leaflet 1.9.4 and Chart.js 4.5.1 are vendored in `vendor/` so the page works offline and no third-party CDN can change the code it runs; map tiles come from Esri, OpenStreetMap and CARTO when online.

## Security

Files you open are parsed in the browser and never uploaded. Because a `.vbo` is untrusted input, the app is built so that content from a file cannot become code:

- **Content-Security-Policy** (`index.html`): scripts and styles load only from the app's own origin, with no inline script, no inline styles and no `eval`. Images are allowed from the three tile servers, video only from files you pick. Nothing from a parsed file is written into the page as markup, and the policy is the backstop if that ever changes.
- **Android** (`android/`): the app is served from `https://appassets.androidplatform.net` through `WebViewAssetLoader` rather than `file://`, so WebView file access is switched off. The WebView never navigates away from the app; links such as the map attribution open in your browser instead. Backups are disabled, and a crashed renderer restarts the screen instead of the whole app.
- **Build pipeline** (`.github/workflows/android.yml`): runs no third-party actions. The build job has read-only access; only a separate publish step, which runs nothing but the GitHub CLI, can write to the repository. The Gradle distribution is pinned to its SHA-256.

### One-time setup: a stable signing key for the APK

Android installs an update only if it is signed with the same key as the installed copy. Without the secrets below, each CI build signs with a new throwaway key, so every update needs an uninstall first, which also wipes the app's saved settings.

1. Create a keystore once, on a machine with a JDK, and keep it somewhere safe. Losing it means users must uninstall to take future updates.

   ```
   keytool -genkeypair -v -keystore vbo-release.jks -alias vbo -keyalg RSA -keysize 4096 -validity 10000
   base64 -i vbo-release.jks | tr -d '\n' > vbo-release.b64
   ```

2. In the repository's **Settings → Secrets and variables → Actions**, add:

   | Secret | Value |
   |---|---|
   | `SIGNING_KEYSTORE_BASE64` | contents of `vbo-release.b64` |
   | `SIGNING_STORE_PASSWORD` | the keystore password |
   | `SIGNING_KEY_ALIAS` | `vbo` |
   | `SIGNING_KEY_PASSWORD` | the key password (the same as the keystore password unless you set a separate one) |

3. Re-run the **Build APK** workflow. Uninstall the current app one last time and install the new APK; every later build installs over it as a normal update.
