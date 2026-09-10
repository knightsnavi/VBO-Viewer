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
- Synchronised video: load the VBOX `.mp4` recordings and the footage tracks the data both ways, with a speed/G overlay
- Basemap switcher — satellite imagery, OSM street, or dark

## Keyboard

`space` play/pause · `←`/`→` step one sample (hold shift for ten) · `home`/`end` jump to the ends of the current view

## Use

Open `index.html` (or the GitHub Pages site) and drop a `.vbo` file onto the page, or click **Load sample**.

## Notes on the format

The fly-by view needs no 3D model and no extra imagery: it is the same aerial tiles as the map, tilted with a CSS 3D transform and spun to a speed-weighted smoothing of the GPS heading. It works because the circuit is flat — the trick degrades on tracks with real elevation change, since aerial imagery carries no height.

The `avitime` column is the position in the recording, in milliseconds, for every logged row, so video sync needs no manual alignment. Your video never leaves the browser. If a clip does not cover the span the log points at, the viewer says so and falls back to free-running playback instead of stalling.

`.vbo` files are plain text. Latitude and longitude are stored in **minutes**, with positive longitude meaning **West**. Time is UTC `HHMMSS.ss`. The `[laptiming]` line gives two points; this viewer anchors the gate at their midpoint and lays it across the track, 50 m wide, perpendicular to the direction of travel at that spot.

Leaflet and Chart.js are vendored in `vendor/` so the page works offline; map tiles come from CARTO/OpenStreetMap when online.
