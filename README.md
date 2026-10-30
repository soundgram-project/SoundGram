<p align="center">
  <img src="docs/assets/soundgram-icon.png" width="128" height="128" alt="SoundGram icon">
</p>

<h1 align="center">SoundGram</h1>

<p align="center">
  <strong>English</strong> · <a href="README.ru.md">Русский</a> 

<p align="center">
  An open-source, unofficial Telegram client for Android with powerful music capabilities, audio enhancement, advanced customization and privacy controls.
</p>

<p align="center">
  <img alt="SoundGram 13.1.1" src="https://img.shields.io/badge/SoundGram-13.1.1-ff4fa3">
  <a href="LICENSE"><img alt="GPL-2.0 license" src="https://img.shields.io/badge/license-GPL--2.0-6f42c1"></a>
  <img alt="Android 7.0 or newer" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Telegram 13.1.1" src="https://img.shields.io/badge/Telegram-13.1.1-26A5E4?logo=telegram&logoColor=white">
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="#plugin-platform">Plugins</a> ·
  <a href="CONTRIBUTING.md">Contributing</a> ·
  <a href="https://github.com/soundgram-project/SoundGram/issues">Issues</a>
</p>

> [!IMPORTANT]
> SoundGram is an independent project based on the official Telegram for Android source code. It is not affiliated with or endorsed by Telegram Messenger Inc.

## About

SoundGram extends [Telegram for Android](https://github.com/DrKLO/Telegram) while preserving the familiar Telegram experience. The project focuses on a powerful integrated music experience, rich audio hooks and equalizer, flexible appearance controls, improved camera and media workflows, privacy features, network tools.

## Features

- **Music & Sound:** Built-in advanced music player, audio hooks, track search, profile music widgets with live waveforms and sound customization.
- **Interface & Themes:** Extensive appearance controls, Monet dynamic colors, icon packs, centered titles, smooth animations, customizable tabs, chat headers and profile customization.
- **Privacy & Security:** Ghost mode options, screenshot protection controls, hiding phone numbers, selective read receipts and anti-recall history.
- **Media Tools:** Enhanced CameraX, video-messages, stories, high-quality audio recording and playback.
- **Network & Proxies:** WebSocket proxy integration, MTProto, and flexible endpoint routing.
- **Architecture:** ARM64 and ARMv7 support in a single standalone/afat APK.

## Build from source

### Requirements

| Component | Version |
| --- | --- |
| JDK | 17 |
| Android SDK / Build Tools | 36 / 36.0.0 |
| Android NDK | 26.3.11579264 |
| CMake | 3.22.1 |

Clone the repository and all native submodules:

```bash
git clone --recursive https://github.com/soundgram-project/SoundGram.git
cd SoundGram
```

Create the local configuration:

```bash
cp private.properties.example private.properties
```

Add your own `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` from [my.telegram.org](https://my.telegram.org).

Build the release APK:

```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

The APK is written to:

```text
TMessagesProj_App/build/outputs/apk/afat/release/app.apk
```

## Telegram API

Every independent build must use its own Telegram API credentials. Read Telegram's documentation for [obtaining an API ID](https://core.telegram.org/api/obtaining_api_id), the [Telegram API](https://core.telegram.org/api) and [MTProto](https://core.telegram.org/mtproto).

## Project layout

| Path | Purpose |
| --- | --- |
| `TMessagesProj` | Telegram core, SoundGram features and Android resources 
| `TMessagesProj_App` | SoundGram application target |
| `TMessagesProj/jni` | Native code and third-party native libraries (tgcalls, webrtc, etc.) |

## Contributing and security

Bug reports and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change.

## Credits

- [Telegram for Android](https://github.com/DrKLO/Telegram), the upstream client.
- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Nagram](https://github.com/NextAlone/Nagram) / [Nekogram](https://github.com/Nekogram/Nekogram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)

## License

SoundGram is distributed under the [GNU General Public License v2.0](LICENSE). Telegram and bundled third-party components retain their respective copyright and license notices.
