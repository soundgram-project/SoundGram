<p align="center">
  <img src="docs/assets/soundgram-icon.png" width="128" height="128" alt="SoundGram 图标">
</p>

<h1 align="center">SoundGram</h1>

<p align="center">
  <a href="README.md">English</a> · <a href="README.ru.md">Русский</a> · <strong>简体中文</strong>
</p>

<p align="center">
  面向 Android 的开源非官方 Telegram 客户端，提供强大的音乐体验、音频增强、深度定制与隐私工具。
</p>

<p align="center">
  <img alt="SoundGram 13.1.1" src="https://img.shields.io/badge/SoundGram-13.1.1-ff4fa3">
  <a href="LICENSE"><img alt="GPL-2.0 许可证" src="https://img.shields.io/badge/license-GPL--2.0-6f42c1"></a>
  <img alt="Android 7.0 及更高版本" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Telegram 13.1.1" src="https://img.shields.io/badge/Telegram-13.1.1-26A5E4?logo=telegram&logoColor=white">
</p>

<p align="center">
  <a href="#功能">功能</a> ·
  <a href="#从源码构建">构建</a> ·
  <a href="#插件平台">插件</a> ·
  <a href="CONTRIBUTING.md">参与贡献</a> ·
  <a href="https://github.com/soundgram-project/SoundGram/issues">问题反馈</a>
</p>

> [!IMPORTANT]
> SoundGram 是基于 Telegram for Android 官方源代码的独立项目，与 Telegram Messenger Inc. 无关联且未获得其认可。

## 关于项目

SoundGram 在保留 Telegram 经典体验的同时，对 [Telegram for Android](https://github.com/DrKLO/Telegram) 进行扩展。项目重点关注强大的内置音乐体验、音频钩子与音效调节、灵活的界面外观、增强的相机与媒体工作流、隐私控制以及插件扩展支持。

## 功能

- **音乐与音效：** 内置高级播放器、音频钩子、曲目搜索、带动态波形的个人资料音乐小组件及音效自定义。
- **界面与主题：** 丰富的视觉自定义、Monet 动态取色、图标包、居中标题、平滑动画、可配置标签页及对话头部。
- **隐私与安全：** 幽灵模式、防截图控制、隐藏手机号、已读回执控制和防撤回历史记录。
- **媒体增强：** 改进的 CameraX、视频留言、故事、高品质音频录制与播放。
- **网络与代理：** 集成 WebSocket 代理、MTProto 及灵活的端点路由。
- **插件扩展：** 支持 Python 和 DEX 插件、钩子桥接、依赖管理及安全的生命周期控制。
- **架构支持：** 在单个 fat/standalone APK 中同时支持 ARM64 与 ARMv7。

## 从源码构建

### 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Android SDK / Build Tools | 36 / 36.0.0 |
| Android NDK | 26.3.11579264 |
| CMake | 3.22.1 |
| Python | 用于 Chaquopy 的 3.11 环境 |

克隆仓库及原生子模块：

```bash
git clone --recursive https://github.com/soundgram-project/SoundGram.git
cd SoundGram
```

创建本地配置：

```bash
cp private.properties.example private.properties
```

添加来自 [my.telegram.org](https://my.telegram.org) 的 `TELEGRAM_API_ID` 与 `TELEGRAM_API_HASH`。

构建发布版 APK：

```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

输出的 APK 路径为：

```text
TMessagesProj_App/build/outputs/apk/afat/release/app.apk
```

## 插件平台

插件实现位于 `TMessagesProj/src/main/java/app/nimarkogram/messenger/plugins` 和 `TMessagesProj/src/main/python`，涵盖 Python 运行环境、钩子桥接、包管理、插件 UI 模型、DEX 加载以及生命周期调度。

## Telegram API

每个独立构建均须使用自己的 Telegram API 凭据。请参阅 Telegram 文档了解 [获取 API ID](https://core.telegram.org/api/obtaining_api_id)、[Telegram API](https://core.telegram.org/api) 和 [MTProto](https://core.telegram.org/mtproto)。

## 项目结构

| 路径 | 用途 |
| --- | --- |
| `TMessagesProj` | Telegram 核心、SoundGram 特性与 Android 资源 |
| `TMessagesProj/src/main/python` | Python 插件 API 与运行时 |
| `TMessagesProj_App` | SoundGram 应用程序目标 |
| `TMessagesProj/jni` | 原生代码与第三方库 (tgcalls, webrtc 等) |
| `third_party/pine` | 固定的 Pine 钩子引擎源码 |

## 鸣谢

- [Telegram for Android](https://github.com/DrKLO/Telegram)
- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Nagram](https://github.com/NextAlone/Nagram) / [Nekogram](https://github.com/Nekogram/Nekogram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Pine](https://github.com/canyie/pine)

## 许可证

SoundGram 基于 [GNU General Public License v2.0](LICENSE) 分发。Telegram 及捆绑的第三方组件保留各自的版权与许可声明。
