# Architecture

## Single-package TV app
SAS TV remains **one Android application and one installable APK** with package `com.sas.tv`.
Gradle separates implementation into:
- `:app`: the existing SAS TV launcher, installed-app discovery, real app icons, favorites, clock, and system shortcuts.
- `:feature:screensaver`: an Android library module that contributes an Android `DreamService`, city settings, clock/date view, and optional live data.

The screensaver library is merged into the `:app` manifest and APK at build time. It does not create a second package, companion APK, or separate installer flow.

The main activity retains `MAIN + HOME + DEFAULT` so Android can offer SAS TV as a Home app. It also retains `LEANBACK_LAUNCHER` and `LAUNCHER`. Android still controls the user's choice; no silent Home change is performed, and the manufacturer's launcher is never disabled or removed.

## UrTV reference and clean-room boundary
The upstream [tshi-dev/UrTV](https://github.com/tshi-dev/UrTV) repository was reviewed as a functional reference. Its project root does not include a general LICENSE file; the MIT notice at `android/launcher/LICENSE-tv-home` is scoped to the upstream launcher origin and does not license every UrTV addition or the screensaver implementation. SAS TV therefore does not copy UrTV application source, compiled APKs, or proprietary fonts. The bundled five landscape photographs are separately identified in [PHOTO-CREDITS.md](PHOTO-CREDITS.md) and sourced under the Unsplash License.

## Screensaver behavior
- The Android system starts the service as a normal DreamService when the user enables it.
- A local settings screen lets the user choose a supported Saudi city.
- The clock refreshes every second and shows Gregorian and Hijri dates using the Riyadh time zone.
- Weather comes from Open-Meteo; prayer times come from Aladhan (Umm al-Qura method).
- When data is unavailable, the screen says so rather than inventing values.
- Background photos are decoded at reduced size and rotated on a 30-minute refresh interval to limit memory use.

## Constraints
- One installable APK; no separate screensaver or phone companion package.
- No root, ADB setup, privileged package changes, or package installer automation.
- Never remove or disable stock applications.
- Optional online features must have clear sources and graceful offline states.
- Device behavior is not called tested until checked on a physical TV.
