# SAS TV — Product scope inspired by UrTV

## Product goal
Build SAS TV as a complete Arabic-first Android TV experience, using UrTV as a functional reference while retaining SAS branding, a single installable APK, and safe opt-in device behavior.

This document is a product target, not a claim that every feature is already implemented.

## 1. Screensaver (primary experience)
- Full-screen Saudi landscape photography with credits, automatic rotation, night dimming, and subtle pixel movement to reduce static-image retention.
- Riyadh/local-city clock, Hijri (Umm al-Qura for Saudi Arabia) and Gregorian dates.
- City selector with Saudi cities first; store coordinates and IANA time zone. Do not hard-code Riyadh time for every selected city.
- Five prayer times, next prayer and countdown, Friday/Jumu'ah label, optional iqama offsets.
- Optional automatic adhan with selectable licensed/public-domain recordings, independent volume, preview, stop controls, and an explicit off-by-default switch.
- Ramadan and Islamic occasion countdowns, morning/evening adhkar, verified verse/hadith cards with references.
- Weather now and forecast, with source and last-updated age; stale values must expire visibly.
- Optional cards: followed teams' fixtures/results, stock watchlist/market status, tasks, verse/hadith, adhkar, forecast.
- Configurable clock size, card style, visible modules, background rotation, and language/digit style.
- Offline behavior: show cached values with timestamps; never invent current prayer/weather/market data.

## 2. Tilawah and adhan
- Quran catalog, reciter list, surah selection, playback progress, pause/resume, and live Quran radio.
- Adhan schedule and player are separate from Quran playback; an adhan pauses Quran and resumes it afterward.
- Automatic adhan is opt-in and configurable: voice, volume, Fajr voice, short/takbir-only mode, screensaver-only mode, notice, and stop behavior.
- Schedule using Android alarms with exact-alarm permission handling, reboot/timezone rescheduling, and no late catch-up after TV standby.
- Audio assets must have verified redistribution rights. Do not bundle recordings whose license is unclear.

## 3. Arabic TV home screen
- RTL home screen, favorites, all installed TV apps, real app icons, inputs, Wi-Fi, settings, and a clear screensaver preview/settings entry.
- D-pad focus behavior must be visible and consistent; support remote, mouse, and touch where hardware exists.
- Home-role selection remains user-controlled. Never silently disable or uninstall the manufacturer's launcher.

## 4. Settings and personalization
- City/location, prayer calculation method, prayer-time adjustments, adhan controls, display/card layout, clock, photos, teams, markets, tasks, and language.
- Save settings locally and preserve them across app updates.
- Each optional online feature has a source, refresh interval, cache policy, and a graceful unavailable state.

## 5. Engineering and release requirements
- Keep one installable SAS TV APK unless a second package is technically required and explicitly agreed.
- Modular Gradle structure: app, screensaver, prayer-times, audio/tilawah, data/cards, settings.
- Keep network and image work off the UI thread; use bounded executors/caches and downsampled images for low-memory TVs.
- Unit tests for prayer-date boundaries, next-prayer selection, city/time-zone handling, cache expiry, and adhan scheduling.
- CI must build the APK and run tests before release.
- Physical TV checklist: D-pad, focus, screen saver activation, sleep/wake, reboot, offline mode, exact-alarm denial, audio interruptions, and rollback.
- Never claim device compatibility until tested on the actual TV model.

## Current implementation status
- Existing: Android TV launcher foundation, one in-app DreamService, basic city selector (limited list), clock/date, basic Open-Meteo weather and Aladhan prayer-time fetch, five credited backgrounds.
- Not yet implemented: complete prayer calculation engine, countdown/iqama, adhan playback/scheduling, Quran catalog/player/radio, verified religious-content pipeline, occasions/adhkar, forecast cards, sports/markets/tasks, comprehensive settings, night dimming, burn-in movement, robust offline cache, and physical TV validation.
