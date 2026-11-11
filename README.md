# YouTube Player (Android)

A dark-themed Android app with two parts:

- **YouTube tab:** loads a user-provided YouTube video in YouTube's official embedded player.
- **My music tab:** plays audio files selected by the user through Android's document picker, using Media3 for background playback and system media controls.
- **Picture-in-Picture:** requests Android PiP for the embedded video view.

## Important YouTube limitations

This app does not extract YouTube audio, download videos, remove ads, or bypass YouTube Premium. YouTube controls whether an embedded video can play in PiP or in the background. The embedded player may reject some videos or environments (including Error 153); the app cannot override those restrictions. Use YouTube's own app for features that require YouTube Premium.

## Build with GitHub Actions

1. Upload/replace the repository contents with this project.
2. Open **Actions** in GitHub.
3. Select **Build Android APK**.
4. Select **Run workflow** (or push a commit to `main`).
5. When the workflow succeeds, open the run and download the `youtube-player-debug-apk` artifact.

The workflow uses Java 17, Android SDK 35, and Gradle 8.9.

## Project files

- `app/src/main/java/com/example/youtubeplayer/MainActivity.kt` — Compose UI, YouTube WebView, URL parsing, and PiP request.
- `app/src/main/java/com/example/youtubeplayer/AudioPlaybackService.kt` — Media3 background audio service for user-selected audio files.
- `app/src/main/AndroidManifest.xml` — internet, media playback service, and PiP declarations.

## Supported YouTube URL formats

- `https://www.youtube.com/watch?v=VIDEO_ID`
- `https://youtu.be/VIDEO_ID`
- `https://www.youtube.com/shorts/VIDEO_ID`
- `https://www.youtube.com/live/VIDEO_ID`
- A raw 11-character video ID

The video must allow embedding. Private, age-restricted, region-restricted, or embedding-disabled videos may not play.
