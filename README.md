# Wholphin

An open-source Jellyfin client for Android TV. This build focuses on video playback across Android TV devices.

[Download Wholphin](https://github.com/Fatal-Kode/Wholphin/releases/latest/download/Wholphin-release.apk) · [Releases](https://github.com/Fatal-Kode/Wholphin/releases) · [Version tags](https://github.com/Fatal-Kode/Wholphin/tags)

## Changes in this build

### Playback

- **ExoPlayer only.** Removed mpv and its settings, the player backend selector, external playback, and the **Play with** context-menu action. ExoPlayer settings are available directly.
- **Optional server deinterlacing for SD video.** Enable **Settings → Advanced Settings → Playback → Transcode interlaced SD video** to request server-side video transcoding for sources Jellyfin reports as interlaced, up to 1024 × 576. The toggle defaults to off. Progressive SD follows the normal playback rules. Jellyfin controls the deinterlacing method and output frame rate.
- **Separate video and audio badges.** The OSD shows each track’s playback mode beside the bottom-right controls. **Direct Play** means the file plays directly. **Original** means Jellyfin copied that track without re-encoding it, and **Transcoding** means Jellyfin re-encoded it. Video can also show **Remuxing** while details are pending. **Unconfirmed** indicates that the track status is unavailable.
- **Video FPS and display refresh rate.** A third badge shows both rates together, for example `23.976 fps → 23.976 Hz`. The reading uses the active video format when available, with source metadata as a fallback for direct or copied video.

### Browsing and settings

- Removed **More Like This** rows from detail screens and the recommendation requests that populated them.
- Removed local and remote trailer buttons and playback, including trailer library browsing.
- Removed **Recommended** library tabs and the **Random** button from library and favourites screens.
- Removed theme music, music library browsing, music results from search, and the **Now Playing** music screen. Music items are skipped when building video playlists.
- Removed audience, critic and age-rating displays from the browsing interface, along with rating-based filters and sorting. Removed the **Display Toggles** settings entry.
- Removed the profile protection setting. Existing protected profiles can be cleared with one final PIN entry or Jellyfin sign-in.
- **Opt-in crash reporting.** Enable it under **Settings → Advanced Settings → More** to be asked before sending an app crash report to your last connected Jellyfin server. Reports include the stack trace, app version, Android version and device model. The setting defaults to off.
- Disabled Seerr integration, including its settings, Discover navigation and request screens.

### Installation and updates

- The Android launcher displays **Wholphin**. The release package ID is `com.github.damontecres.wholphin`.
- GitHub Actions publishes signed Android TV APKs from `main` under versioned release tags. Releases include a universal APK and builds for ARM64, ARMv7 and x86-64. The workflow checks that the signing certificate matches the previous release.
- The in-app updater checks this repository’s latest stable release. The update URL is fixed and has no settings entry.
- When Android requires install permission, the updater opens the system screen so you can allow Wholphin to install updates.

## Install on Android TV

1. Download the universal APK from the [latest release](https://github.com/Fatal-Kode/Wholphin/releases/latest).
2. Open it on your Android TV device. If prompted, allow the app you’re using to open the APK to install unknown apps.
3. Open Wholphin and sign in to your Jellyfin server.

If Android reports a signing conflict with an installed copy of Wholphin, uninstall that copy before installing this build. Uninstalling clears its local settings. Subsequent releases signed with the same key can update this build in place.

## Licence

See [LICENSE](LICENSE).
