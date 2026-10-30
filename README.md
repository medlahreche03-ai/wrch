# ForumWatch Android reconstruction (source project)

This is a first Android-native reconstruction based on the recoverable metadata and symbols in the supplied `ForumWatch.exe` (.NET 8 single-file WPF application).

## Included
- Native Android settings/dashboard UI
- Forum base URL and vBulletin username/password fields
- Member ID tracking and manual scan
- Local SQLite member-state history
- Basic online/offline page heuristics
- Telegram Bot API notifications
- WhatsApp Cloud API text notifications
- Forum login request test

## Important compatibility notes
The Windows EXE does not contain portable Android code. The original vBulletin parsing, authentication/session handling, and post/thread scanning logic must be validated against the target forum. The Android implementation currently uses conservative generic vBulletin page requests and online/offline text heuristics. It does not yet reproduce the original post/thread scanner or background scheduling. Different vBulletin versions and custom themes require parser adjustments.

## Build
Open this folder in Android Studio (Giraffe or newer), allow Gradle sync, then select **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The project uses Android Gradle Plugin 8.7.3, compile SDK 35, and Java source compatible with the standard Android toolchain.

For an APK from the command line on a machine with Android SDK installed:

```bash
gradle assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Security
Credentials and API tokens are stored in app-private SharedPreferences. For production distribution, replace this with Android Keystore-backed encryption, add certificate/network hardening, and provide an explicit privacy policy. Use HTTPS forum URLs only.
