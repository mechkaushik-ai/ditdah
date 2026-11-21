# Dit Dah — Morse Code

An Android app that turns text into International Morse Code you can **hear**, **see** (flashlight) and **feel** (vibration), and decodes Morse back into text.

- **Send:** type a message and play it as beeps, flashes and/or vibration, with each letter highlighted as it plays. Speed 5–40 WPM, tone 400–1000 Hz, optional Farnsworth spacing for learners.
- **Decode:** type dots and dashes, or use the on-screen tap key.
- **Chart:** every ITU Morse character; tap to hear it.
- Offline, no ads, no tracking, no data collected.

Built with Kotlin and Jetpack Compose. Minimum Android 8.0 (API 26), targets Android 16 (API 36).

## Download

Get the latest APK from the [Releases](../../releases) page.

## How it's built

Every push to `main` runs the tests and builds an APK on GitHub Actions (see `.github/workflows/build.yml`). Download it from the latest run's **Artifacts**.

Signed release builds need these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

To build on your own computer, open the folder in Android Studio and press **Run**.

## Code map

| File | What it does |
|---|---|
| `app/src/main/java/com/kaushikc/ditdah/MorseCode.kt` | Morse table, encoding, decoding, PARIS and Farnsworth timing |
| `app/src/main/java/com/kaushikc/ditdah/MorsePlayer.kt` | Sound (generated sine tone), flashlight and vibration, kept in sync |
| `app/src/main/java/com/kaushikc/ditdah/MainActivity.kt` | App start-up, navigation, playback control |
| `app/src/main/java/com/kaushikc/ditdah/Screens.kt` | Send, Decode and Chart screens |
| `app/src/test/.../MorseCodeTest.kt` | Automatic tests for the Morse logic |
| `docs/index.md` | Privacy policy (published with GitHub Pages) |
| `fastlane/metadata/` | Store listing text and graphics |

## Licence

MIT © 2026 Kaushik Chandrasekar
