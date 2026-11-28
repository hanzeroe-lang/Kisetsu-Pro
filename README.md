<div align="center">

# Kisetsu Pro

### A music player that follows the season — fully open, no membership gate.

<br/>

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/hanzeroe-lang/Kisetsu-Pro/total?style=for-the-badge&labelColor=0d1117)](https://github.com/hanzeroe-lang/Kisetsu-Pro/releases)

</div>

> **Kisetsu Pro is an independent, modified derivative of [METH](https://github.com/Baraka-kileo/meth-releases) and [BitChord](https://github.com/kushagrasinghx/BitChord), both GPL-3.0. It is not affiliated with, endorsed by, or connected to YouTube, Google, Spotify, Apple, Discord, or any other service mentioned here.**

---

## What Kisetsu Pro is

A free Android music player built around lossless-first playback. It plays your local library, YouTube Music, and optional lossless addons, with library, downloads, lyrics, scrobbling and optional Discord presence.

**No account, no "member only" area, no Discord link required.** Every feature is available to everyone. Discord sign-in is purely optional and only used for Rich Presence.

The app icon is a white note on Apple Music red, supplied by the project maintainer.

## Download

Grab `kisetsu-pro.apk` from the [v1 release](https://github.com/hanzeroe-lang/Kisetsu-Pro/releases/tag/v1).

- Package: `com.hanzeroe.kisetsupro` — a separate identity, installable alongside METH.
- Version: `1.0` (`versionCode 1`).
- Requires Android 8.0 (API 26) or newer.
- Sideloading requires enabling "Install unknown apps" for the app you download with.

## Build from source

JDK 21 and the Android SDK (compileSdk 37) are required. Copy `keystore.properties.example` to `keystore.properties` and point it at your own keystore for a signed build.

```bash
./gradlew :app:assembleProdRelease
```

The signed universal APK is written to `app/build/outputs/apk/prod/release/`.

## License and attribution

Kisetsu Pro is free software under the **GNU General Public License v3.0**. You may use, study, share and modify it, including commercially, but any distribution must keep the licence and ship the corresponding source under the same terms. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

This derivative is based on:

- **METH** by Baraka-kileo and contributors (GPL-3.0) — <https://github.com/Baraka-kileo/meth-releases>
- **BitChord** by Kushagra Singh and contributors (GPL-3.0) — <https://github.com/kushagrasinghx/BitChord>
- **Myspoti / Orpheon** by kota-zo (GPL-3.0), **Melo** by aarush-io (GPL-3.0), **Mochy Music** by realidkroo (GPL-3.0), **BitChord-X** by AnGorRoc (GPL-3.0), **Open-Unmix** by sigsep (MIT), **Freeverb** by Jezar (public domain), and the service/data projects credited in METH's release notes.

Modifications from METH: renamed and repackaged as a standalone app, new launcher icon, and no member-only/Discord-link gate.

## Disclaimer

Kisetsu Pro is an independent player. It does not host, upload or store copyrighted music. You are responsible for complying with your local copyright law and the terms of any service you connect. Provided as is, without warranty.
