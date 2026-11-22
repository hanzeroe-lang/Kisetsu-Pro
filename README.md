# Kisetsu Pro

Kisetsu Pro is a polished Android music app experience built on the BitChord/Metrolist lineage, with a focus on smooth UI, rich playback, and fast iteration.

## ✨ Highlights

- **Modern player UI** with refined controls, animations, and glass/progressive blur styling
- **Discover & Search** experience with quick browsing, recent searches, and smooth transitions
- **Library & playback** features including queue/radio flows, lyrics, and local playback support
- **In-app updates** from GitHub Releases
- **Signed release builds** published as `kisetsu-<version>-*.apk`

## 📦 Download

Get the latest APK from **Releases**:
- https://github.com/hanzeroe-lang/Kisetsu-Pro/releases/latest

## 🔄 In-App Updates

Kisetsu Pro checks:
- `https://api.github.com/repos/hanzeroe-lang/Kisetsu-Pro/releases/latest`

Install a build from this repo once to receive future in-app updates from the new release channel.

## 🛠️ Build (Android)

### Requirements
- JDK 21
- Android SDK / Android Studio environment
- Gradle wrapper (included)

### Build release APKs

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleProdRelease --no-daemon
```

APKs are generated under:
- `app/build/outputs/apk/prod/release/`

## 🧾 Release Naming

- Tag: `kisetsu-v<version>`
- Assets:
  - `kisetsu-<version>-universal.apk`
  - `kisetsu-<version>-arm64-v8a.apk`

## 🙏 Credits

Kisetsu Pro builds on work from the open-source music app ecosystem, including BitChord/Metrolist and related upstream projects. Please respect upstream licenses and attribution requirements when redistributing.