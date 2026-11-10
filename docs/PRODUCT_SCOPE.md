# SAS TV — Product Scope

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
- Implemented in the current branch: five daily prayer times, next prayer selection and live countdown on the screensaver; the latest GitHub Actions build produced an APK successfully.
- Not yet implemented or not yet validated: full prayer calculation engine and tomorrow-boundary handling, iqama, adhan playback/scheduling, Quran catalog/player/radio, verified religious-content pipeline, occasions/adhkar, forecast cards, sports/markets/tasks, comprehensive settings, night dimming, burn-in movement, robust offline cache, and physical TV validation.

## 6. Release acceptance gate — complete UrTV functional parity
A release is **not complete** merely because the APK builds or installs. The final SAS TV release must implement and pass tests for all applicable UrTV user-facing functions, including:
- Screensaver: configurable Saudi/world city and time zone, clock and date formats, Umm al-Qura Hijri date, five prayers, next-prayer countdown, iqama adjustments, Friday label, Ramadan/occasion countdowns, landscape/photo packs and rotation, user photos where supported, night dimming, burn-in movement, weather/current conditions/forecast, humidity/wind/air-quality where a reliable source exists, and selectable information cards.
- Islamic experience: Quran reciters and all 114 surahs, Quran live radio/stations, playback/resume/progress, adhan times and previews, selectable muezzin recordings, automatic adhan settings and volume, Fajr-specific options, pause/resume coordination between Quran and adhan, and reliable alarm recovery after reboot/time changes. Exact-alarm and notification permissions must be explained and handled safely.
- Verified content: morning/evening adhkar, Quran verses, hadith and Islamic occasions with explicit references/source metadata; never generate religious quotations or claim verification without a traceable source.
- Home/launcher: Arabic RTL layout, favorites and their ordering, all TV apps and icons, inputs, Wi-Fi, system settings shortcuts, remote/D-pad focus, optional cards/widgets and direct navigation to each feature. Do not take over the system HOME role or disable the stock launcher without a separate explicit user choice.
- Smart cards and personalization: sports teams/leagues and fixtures, stock watchlist/market status, tasks/reminders, weather forecast, card placement/style/visibility, clock size/digits, background selection, language and city/prayer preferences. Unsupported or unavailable external feeds must show a clear unavailable state rather than fabricated data.
- Reliability and delivery: persistent settings across updates, bounded image/audio/data caches, offline/stale-data handling, accessibility from a TV remote, no unexpected audio at startup, stable signing key for in-place updates, versioned release APK, install/update/rollback instructions, automated tests, and a documented physical-TV test report.

**Delivery rule:** do not label or present a build as the final complete release until every required feature above is implemented or explicitly marked not applicable with a reason, automated tests pass, and the target TV has been physically tested. Deliver one SAS TV APK for direct installation wherever Android packaging permits; never require the user to run ADB commands for ordinary installation.
