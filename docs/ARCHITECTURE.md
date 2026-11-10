# Architecture

## Single-package TV app
SAS TV is one Android application and one installable APK. The current module is `app` with package `com.sas.tv`.

The app is launched explicitly from the TV's normal apps list through `LEANBACK_LAUNCHER` (and from a phone-style launcher through `LAUNCHER`). It intentionally has no `CATEGORY_HOME` filter. Therefore Android does not register it as a replacement Home app, and installation cannot change the manufacturer's default launcher.

## UrTV reference
The upstream UrTV project separates its home launcher and screensaver into distinct packages. SAS TV uses its user-facing TV-app approach as a reference, but the product requirement here is a single package. Do not import upstream installation scripts that switch the HOME role, disable the stock launcher, alter saved Wi-Fi, or require ADB/root.

## Constraints
- No companion APK or screensaver APK is installed.
- No package installer automation or privileged system modifications.
- No removal/disablement of stock applications.
- Optional online features must use explicit, documented data sources and graceful offline states.
