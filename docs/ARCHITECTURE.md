# Architecture

## Single-package TV Home app
SAS TV is one Android application and one installable APK. The current module is `app` with package `com.sas.tv`.

The main activity declares `MAIN + HOME + DEFAULT`, allowing Android TV to offer SAS TV as a Home application. It also retains `LEANBACK_LAUNCHER` and `LAUNCHER` so it remains discoverable in app lists.

Android/device policy may require the user to select SAS TV through the Home-app chooser or default-app settings. Installing the APK alone is not guaranteed to silently change the default Home app. The manufacturer's launcher must remain installed and enabled so the user can switch back.

## UrTV reference
The upstream UrTV project separates its home launcher and screensaver into distinct packages. SAS TV targets one installable package. Do not import upstream installation scripts that disable the stock launcher, alter saved Wi-Fi, or require ADB/root.

## Constraints
- One APK; no companion APK or separately installed screensaver package.
- No package installer automation or privileged system modifications.
- Never remove or disable stock applications.
- Optional online features must use explicit, documented data sources and graceful offline states.
