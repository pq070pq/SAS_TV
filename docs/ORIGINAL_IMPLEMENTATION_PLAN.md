# SAS TV — Original UrTV-inspired implementation plan

## Product decision
Implement an original SAS TV product with functional parity to the user-facing UrTV experience. Do not copy unlicensed source files, artwork, fonts, recordings, or bundled data. Reuse only components whose license clearly covers the exact files and preserve required notices. Recreate all other behavior independently.

## Non-negotiable release outcome
- One ordinary-install APK wherever Android's packaging and platform constraints allow it.
- Arabic-first Android TV / Google TV UI with full remote/D-pad navigation.
- Full screensaver, prayer, Quran/tilawah, adhan, smart cards, and personalization feature set below.
- Stable release signing key and monotonically increasing versionCode so upgrades preserve app data.
- No ADB required for ordinary installation. Optional advanced setup must remain opt-in and reversible.
- No release called complete until automated tests and a physical target-TV test report pass.

## Feature parity acceptance matrix

### A. Home experience
- Arabic RTL launcher, favorites (add/remove/reorder), all installed TV apps, app icons and labels.
- Inputs, Wi-Fi, Android settings shortcuts, screensaver preview/settings entry.
- Optional home cards, direct navigation to each feature, visible focus states and predictable D-pad movement.
- Never silently take over HOME, disable the stock launcher, or alter system packages.

### B. Saudi screensaver
- Clock, 12/24-hour mode, Arabic/Latin digits, Hijri Umm al-Qura and Gregorian dates.
- Saudi city selection plus custom city coordinates and IANA timezone.
- Five prayer times, next prayer and live countdown, Friday label, configurable iqama offsets.
- Ramadan and Islamic occasion countdowns.
- Saudi landscape photo packs, optional user photos, configurable rotation interval, night dimming and burn-in mitigation.
- Current weather, forecast, humidity/wind/air quality when a trustworthy source is available, visible source and freshness.
- Configurable information cards and clock/card layouts.
- Offline cache with timestamps; never present expired data as live.

### C. Prayer and adhan
- Calculation methods appropriate to selected country/city, manual per-prayer adjustment and date-boundary correctness.
- Adhan preview, selectable voice, independent volume, Fajr-specific voice, short mode, notification, stop controls.
- Automatic adhan disabled by default; explicit opt-in, exact-alarm permission handling, reboot/timezone/time-change rescheduling.
- Do not play late catch-up after standby; never play twice for the same prayer.
- Quran playback pauses for adhan and resumes safely afterward.

### D. Quran and tilawah
- Quran catalog with 114 surahs, reciter catalog, streaming playback, progress, pause/resume and last-played state.
- Quran radio/live stations with source metadata and unavailable-state handling.
- Remote-friendly reciter/surah browsing and player controls.
- Use only audio streams/assets whose redistribution or streaming terms are verified; do not bundle unlicensed recordings.

### E. Verified Islamic content
- Morning/evening adhkar, Quran verses, hadith, Friday content and Islamic occasions.
- Every item carries source/reference metadata and validation provenance.
- Never invent, paraphrase as a quotation, or label content verified without a traceable source.

### F. Smart cards
- Weather and multi-day forecast.
- Sports teams/leagues, schedules and results with freshness/source.
- Stock watchlist, market status and quote freshness; no fabricated prices.
- Tasks/reminders with local persistence.
- User-selectable card placement, order, style and visibility.

### G. Settings
- City, coordinates/timezone, prayer method and adjustments, iqama, adhan, reciters, radio, display, clock, digits, backgrounds, cards, teams, watchlist, tasks, language.
- Settings survive upgrades and are exportable/backed up where Android permits.
- Every feature has explicit network/cache/permission behavior and graceful error state.

## Delivery phases
1. **Platform foundation:** stable app identity/signing/versioning, app navigation, remote focus framework, persistent settings, CI release build and test pipeline.
2. **Screensaver parity:** city/timezone correctness, prayer engine, countdown/iqama, photo system, night/burn-in protection, weather and configurable cards.
3. **Quran and adhan:** catalog/player/radio, alarm scheduling, audio focus and resume behavior, rights-checked source integration.
4. **Content and smart cards:** verified adhkar/verse/hadith, occasions, sports, markets, weather forecast and tasks.
5. **Release hardening:** migration tests, offline tests, accessibility/D-pad tests, sleep/wake/reboot tests, permissions, stable signed APK, direct install/update instructions and target-TV validation.

## Current known gap
The existing SAS TV repository is a foundation only: a basic launcher, a DreamService screensaver, a limited Saudi city picker, clock/date, basic Open-Meteo weather, Aladhan prayer lookup, five background photos and next-prayer countdown. It is not yet a parity release. Do not claim the remaining features are implemented until code and tests exist.
