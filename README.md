# Wholphin

An open-source Jellyfin client for Android TV. This build focuses on video playback across Android TV devices.

[Download Wholphin](https://github.com/Fatal-Kode/Wholphin/releases/latest/download/Wholphin-release.apk) · [Releases](https://github.com/Fatal-Kode/Wholphin/releases) · [Version tags](https://github.com/Fatal-Kode/Wholphin/tags)

## Changes in this build

### Playback

- **ExoPlayer only.** Removed mpv from the build, the player backend selector, external playback, and the **Play with** context-menu action.
- **Optional server deinterlacing for SD video.** Enable **Settings → Advanced Settings → Playback → Transcode interlaced SD video** to request server-side video transcoding for sources Jellyfin reports as interlaced, up to 1024 × 576. Progressive SD follows the normal playback rules. Jellyfin controls the deinterlacing method and output frame rate.
- **Playback indicators beside the bottom-right controls.** The video and audio badges report each track’s playback mode separately, including **Direct Play**, **Original** for streams copied by the server, and **Transcoding**.
- **Frame rate and display refresh rate in one indicator.** The OSD shows readings such as `23.976 fps → 23.976 Hz` or `25 fps → 50 Hz` when those rates are available.

### Browsing and settings

- Removed trailer buttons and trailer playback from browsing.
- Removed the **Recommended** library tabs and the **Random** button from library and favourites screens.
- Removed theme music, music library browsing, and the **Now Playing** music screen. Audio tracks are excluded from video playlist playback.
- Removed rating displays and rating-based browsing options.
- Removed the profile protection setting. Existing protected profiles can be cleared with one final PIN entry or Jellyfin sign-in.
- Removed automatic crash reporting and its setting.
- Disabled Seerr integration and removed its settings and Discover screens.

### Installation and updates

- The Android launcher displays **Wholphin**. The release package ID is `com.github.damontecres.wholphin`.
- Signed APKs are built from `main` and published under versioned release tags.
- The in-app updater checks this repository’s latest stable release. The update URL is fixed and has no settings entry.
- When Android requires install permission, the updater opens the system screen so you can allow Wholphin to install updates.

## Install on Android TV

1. Download the universal APK from the [latest release](https://github.com/Fatal-Kode/Wholphin/releases/latest).
2. Open it on your Android TV device. If prompted, allow the app you’re using to open the APK to install unknown apps.
3. Open Wholphin and sign in to your Jellyfin server.

If Android reports a signing conflict with an installed copy of Wholphin, uninstall that copy before installing this build. Uninstalling clears its local settings. Subsequent releases signed with the same key can update this build in place.

## Licence

See [LICENSE](LICENSE).
