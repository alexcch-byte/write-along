# Write Along

Write Along teaches elementary kids to form print and cursive letters the right way: where each
stroke starts, which way it goes, and how letters sit on the writing lines.

- **Use it in a browser:** https://alexcch-byte.github.io/write-along/
- **Android app:** [download WriteAlong.apk](https://github.com/alexcch-byte/write-along/releases/latest/download/WriteAlong.apk)
  (on your phone or tablet, open the file and allow installing from your browser when asked)

## What's in it

- **Print, Cursive and Calligraphy letters** (a–z, A–Z). Calligraphy is an italic hand written with a
  flat broad-edge pen: strokes across the nib come out thick and strokes along it hair thin. Each letter has three steps:
  1. **Watch**: the letter draws itself stroke by stroke while the steps are read aloud.
  2. **Trace**: follow a highlighted path with a numbered green start dot, arrows and a red stop dot.
     Each stroke is checked as soon as the finger lifts (wrong start, wrong way, off the path, too short).
  3. **Write**: a dotted letter, a faint letter, then empty spaces. **Check** gives 0–3 stars per
     letter plus a tip ("Sit your writing down on the bottom line", "Follow the numbers"...).
- **Big / Small lines** on every writing surface: one large set of lines, or 3–4 rows of small ones.
- **Assignments**: short tasks to copy letters, words, sentences and the child's own name.
  Line 1 is dotted to trace, the next line is for writing alone. Check scores each line, and the
  next task unlocks after checking.
- **Free Write**: lined paper, optionally with a word to trace, pen colors, undo and clear.
- Spoken instructions (can be muted), stars saved on the device, palm rejection once a stylus is used.

## Project layout

| Path | What it is |
| --- | --- |
| `app/` | Android app (Kotlin, Jetpack Compose) |
| `app/src/main/java/.../glyph/` | Letter shapes and stroke order (`PrintGlyphs.kt`, `CursiveGlyphs.kt`, `CalligraphyGlyphs.kt`), cursive joining |
| `app/src/main/java/.../practice/` | Line layouts, tracing/writing checks, assignment list |
| `docs/` | Web version, served by GitHub Pages (plain HTML/CSS/JS, no build step) |
| `docs/js/data.js` | Letter data for the web, **generated** from the Android code (don't edit by hand) |
| `web-tests/` | Node tests for the web engine |

## Build and test

Android (open in Android Studio, or from a terminal):

```
gradlew :app:assembleRelease
gradlew :app:testDebugUnitTest
```

The unit tests also regenerate `docs/js/data.js` from the Android letter data, and write a contact
sheet of every letter to `app/build/reports/glyphs.html`.

Web: open `docs/index.html` through any static file server, for example
`python -m http.server -d docs`, then check the engine with `node web-tests/engine.test.js`.

The release APK is signed with the local Android debug key so it installs directly; use a
dedicated release key before publishing to an app store.
