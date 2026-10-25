<p align="center">
  <img src="docs/assets/soundgram-icon.png" width="128" height="128" alt="Иконка SoundGram">
</p>

<h1 align="center">SoundGram</h1>

<p align="center">
  <a href="README.md">English</a> · <strong>Русский</strong> · <a href="README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  Открытый неофициальный клиент Telegram для Android с мощными музыкальными возможностями, улучшением звука, расширенной кастомизацией и инструментами приватности.
</p>

<p align="center">
  <img alt="SoundGram 13.1.1" src="https://img.shields.io/badge/SoundGram-13.1.1-ff4fa3">
  <a href="LICENSE"><img alt="Лицензия GPL-2.0" src="https://img.shields.io/badge/license-GPL--2.0-6f42c1"></a>
  <img alt="Android 7.0 и новее" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Telegram 13.1.1" src="https://img.shields.io/badge/Telegram-13.1.1-26A5E4?logo=telegram&logoColor=white">
</p>

<p align="center">
  <a href="#возможности">Возможности</a> ·
  <a href="#сборка-из-исходного-кода">Сборка</a> ·
  <a href="#платформа-плагинов">Плагины</a> ·
  <a href="CONTRIBUTING.md">Участие</a> ·
  <a href="https://github.com/soundgram-project/SoundGram/issues">Ошибки</a>
</p>

> [!IMPORTANT]
> SoundGram — независимый проект на основе официального исходного кода Telegram для Android. Проект не связан с Telegram Messenger Inc. и не одобрен этой компанией.

## О проекте

SoundGram расширяет возможности [Telegram для Android](https://github.com/DrKLO/Telegram), сохраняя привычную логику официального клиента. Основные направления проекта — продвинутый музыкальный опыт, аудиохуки и эквалайзер, гибкая настройка интерфейса, улучшенная работа камеры и медиа, инструменты приватности и сети, а также поддержка плагинов.

## Возможности

- **Музыка и звук:** Встроенный продвинутый плеер, хуки для аудио, поиск треков, виджеты музыки в профиле с живой волной и гибкая настройка звука.
- **Интерфейс и темы:** Цвета Monet, наборы иконок, центрированные заголовки, плавные анимации, настраиваемые вкладки, заголовки чатов и кастомизация профиля.
- **Приватность и безопасность:** Режим призрака, управление защитой от скриншотов, скрытие номеров телефонов, выборочные отчеты о прочтении и история удаленных сообщений.
- **Медиа:** Расширенный CameraX, видеосообщения, истории, высококачественная запись и воспроизведение аудио.
- **Сеть и прокси:** Поддержка WebSocket-прокси, MTProto и гибкая маршрутизация.
- **Плагины:** Поддержка Python и DEX плагинов с мостом хуков, управлением зависимостями и безопасным жизненным циклом.
- **Архитектура:** Единый fat/standalone APK с поддержкой ARM64 и ARMv7.

## Сборка из исходного кода

### Требования

| Компонент | Версия |
| --- | --- |
| JDK | 17 |
| Android SDK / Build Tools | 36 / 36.0.0 |
| Android NDK | 26.3.11579264 |
| CMake | 3.22.1 |
| Python | 3.11 для сборки Chaquopy |

Клонируйте репозиторий со всеми субмодулями:

```bash
git clone --recursive https://github.com/soundgram-project/SoundGram.git
cd SoundGram
```

Создайте локальную конфигурацию:

```bash
cp private.properties.example private.properties
```

Укажите ваши `TELEGRAM_API_ID` и `TELEGRAM_API_HASH` с сайта [my.telegram.org](https://my.telegram.org).

Соберите релизный APK:

```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

Собранный файл будет находиться в:

```text
TMessagesProj_App/build/outputs/apk/afat/release/app.apk
```

## Платформа плагинов

Реализация плагинов находится в `TMessagesProj/src/main/java/app/nimarkogram/messenger/plugins` и `TMessagesProj/src/main/python`. Она включает среду выполнения Python, мост перехватов, управление пакетами, UI-модели плагинов, загрузку DEX и координацию жизненного цикла.

## Telegram API

Каждая независимая сборка должна использовать собственные ключи Telegram API. Ознакомьтесь с документацией по [получению API ID](https://core.telegram.org/api/obtaining_api_id), [Telegram API](https://core.telegram.org/api) и [MTProto](https://core.telegram.org/mtproto).

## Структура проекта

| Путь | Назначение |
| --- | --- |
| `TMessagesProj` | Ядро Telegram, функционал SoundGram и ресурсы Android |
| `TMessagesProj/src/main/python` | API и рантайм плагинов на Python |
| `TMessagesProj_App` | Основное приложение SoundGram |
| `TMessagesProj/jni` | Нативный код и сторонние библиотеки (tgcalls, webrtc и др.) |
| `third_party/pine` | Движок хуков Pine |

## Участие и безопасность

Сообщения об ошибках и пулл-реквесты приветствуются. Перед отправкой изменений ознакомьтесь с [CONTRIBUTING.md](CONTRIBUTING.md).

## Благодарности

- [Telegram for Android](https://github.com/DrKLO/Telegram), официальный клиент.
- [AyuGram](https://github.com/AyuGram/AyuGram4A)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Nagram](https://github.com/NextAlone/Nagram) / [Nekogram](https://github.com/Nekogram/Nekogram)
- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Pine](https://github.com/canyie/pine), движок хуков.

## Лицензия

SoundGram распространяется на условиях [GNU General Public License v2.0](LICENSE). Telegram и сторонние библиотеки сохраняют свои уведомления об авторских правах и лицензиях.
