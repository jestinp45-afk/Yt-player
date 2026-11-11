# YouTube Player — Android starter

A premium-dark Android starter project.

## Build from GitHub using only your phone

1. Download and extract the project ZIP.
2. Create a new repository on GitHub named `youtube-player`.
3. Upload **all files and folders** from this project, including the hidden `.github` folder.
4. Commit the files to the `main` branch.
5. Open the repository's **Actions** tab. If GitHub asks, enable Actions.
6. Open **Build Android APK** and choose **Run workflow**, or push a commit to `main`.
7. When the run finishes successfully, open the run and download the `youtube-player-debug-apk` artifact.
8. Extract the downloaded artifact ZIP to get `app-debug.apk`.

## What's included

- Jetpack Compose premium-dark interface.
- Official YouTube embedded player for foreground video viewing.
- Audio file picker for files you own or are licensed to play.
- AndroidX Media3 background audio service and media-session integration.
- GitHub Actions workflow to build a debug APK automatically.

## Important playback boundaries

YouTube videos use the official embedded player. This project does not extract YouTube audio,
remove ads, download videos, or enable YouTube background playback. Background playback is
for user-selected audio files the user owns or is licensed to play.

The initial source is a starter, not a release-ready app. It has not yet been compiled or
device-tested in this environment. Test the generated APK on your Android device.

## Next development steps

- Add authorized YouTube Data API search.
- Add persistent playlists and a playback queue.
- Improve licensed-audio metadata and artwork.
- Add PiP only for content whose terms permit it.
- Add accessibility, privacy policy, app icon, device testing, and release signing.
