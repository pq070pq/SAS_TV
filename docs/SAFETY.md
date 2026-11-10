# Safety constraints

1. Keep SAS TV as a normal launchable app; do not add an Android HOME intent filter.
2. Never change the device's default Home role or disable the manufacturer's launcher.
3. Never use root, ADB setup scripts, WRITE_SECURE_SETTINGS, or privileged package-management commands.
4. Never uninstall or disable system applications.
5. Deliver one APK only. Do not require a separate screensaver or phone companion package.
6. Do not claim installation/device testing unless it was actually performed.
