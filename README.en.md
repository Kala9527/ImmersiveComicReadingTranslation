# Immersive Comic Reading Translation

Immersive Comic Reading Translation is an Android MVP for floating comic translation while reading. It uses overlay windows, screen capture, OCR, and OpenAI-compatible translation endpoints to show translated results in a side panel.

## Highlights

- Native Android Java project, ready for Android Studio.
- Separate OCR and translation model configuration.
- OpenAI-compatible `/chat/completions` style API support.
- API keys stored with Android Keystore.
- Overlay permission, screen capture permission, and foreground service flow.
- Tap the floating button to capture the screen and run OCR -> correction/translation.
- Side panel with expand/collapse, copy, retry, and quick settings.
- Stage-aware errors for permissions, capture, OCR network/parsing, and translation network/parsing.

## Structure

```text
.
├─ app/
│  ├─ build.gradle
│  └─ src/
│     ├─ main/
│     └─ debug/
├─ gradle/wrapper/
├─ build.gradle
├─ settings.gradle
└─ gradlew.bat
```

## Development

Requirements:

- Android Studio
- JDK 17
- Android SDK with compileSdk 36

Open the project root in Android Studio, let Gradle sync, then run the `app` module.

## Build APK

Windows:

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Install the APK.
2. Configure OCR and translation Base URL, model ID, and API Key.
3. Test both OCR and translation models.
4. Start floating translation.
5. Grant overlay and screen capture permissions.
6. Switch to a comic app and tap the floating button to translate.

## MVP Scope

- No cloud API key is bundled.
- Screenshots are captured only after user action.
- The current version shows a side translation panel instead of bubble-level image overlays.
- Android blocks capture on `FLAG_SECURE` pages.

## Notes

- `.gradle/`, `build/`, `.idea/`, APKs, and local outputs are ignored.
- `local.properties` contains local SDK paths and should not be committed.
- Please respect platform rules, model service policies, and content copyright.

## Thanks

Thank you for checking out this MVP. If this direction feels useful, a Star, Fork, issue, or suggestion would mean a lot and will help me keep improving it.
