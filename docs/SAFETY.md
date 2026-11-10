# Safety constraints

1. SAS TV may declare Android `HOME` and `DEFAULT` categories so the user can select it as the TV's default launcher.
2. Do not silently force a default Home change; respect Android's Home-app chooser and device policy.
3. Never disable, uninstall, hide, or overwrite the manufacturer's launcher. Preserve the ability to switch back.
4. Never use root, ADB setup scripts, WRITE_SECURE_SETTINGS, or privileged package-management commands.
5. Deliver one APK only. Do not require a separate screensaver or phone companion package.
6. Do not claim installation/device testing unless it was actually performed.
