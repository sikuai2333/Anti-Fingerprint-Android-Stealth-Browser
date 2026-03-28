<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=7C4DFF&height=250&section=header&text=PhantomMAX&fontSize=80&fontColor=FFFFFF&animation=fadeIn&fontAlignY=38&desc=Advanced%20Kernel-Level%20Android%20Stealth%20Browser&descAlignY=55&descSize=20" width="100%"/>


[![Android](https://img.shields.io/badge/Платформа-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Язык-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/Лицензия-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/Версия-2.0.0_Pro-success?style=for-the-badge)](https://github.com/Genuys/PhantomMAX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-Канал-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TgUnlock2026)

> **PhantomMAX** — это stealth-браузер нового поколения для Android. Он фундаментально изменяет механизмы рендеринга WebView и контекст JavaScript, делая ваше мобильное устройство абсолютно неотличимым от мощного ПК на Windows.
> Обходите продвинутые алгоритмы фингерпринтинга (Cloudflare Turnstile, Akamai, DataDome) с помощью реалистичной подмены WebGL на уровне ядра и уникального криптографического шума Canvas, сохраняя 100% совместимость со сложными SPA-фреймворками (React, Svelte, Vue).

[![Download APK](https://img.shields.io/badge/⬇️%20Скачать%20APK-v2.0.0-7C4DFF?style=for-the-badge)](https://github.com/Genuys/PhantomMAX-Android-Stealth-Browser/releases/tag/1.0.0)

</div>

---

## ⚡ Технологии Stealth-ядра

### 🎭 Deep Spoofing Engine (Zero-Leak)
В отличие от обычных решений, которые просто меняют User-Agent, PhantomMAX напрямую манипулирует JavaScript-прототипами браузера:
- **Крипто-шум Canvas & WebGL** — Внедряет математически выверенные, невидимые глазу псевдослучайные отклонения в `toDataURL` и `getImageData`. Отпечаток уникален для каждой сессии и не дает "грязных" артефактов.
- **Маскировка AudioContext** — Умное искажение буферов `getChannelData` и `AnalyserNode` для защиты от аудио-фингерпринтинга.
- **SPA Compatibility Mode** — Продвинутые `writable: true` proxy-перехватчики. Даже самые требовательные React/Svelte сайты не упадут при проверке фейковых API браузера.
- **Hard & Device Spoofing** — Эмулирует 8-ядерный CPU, 8ГБ RAM, точные WebGL сигнатуры Intel UHD 630, а также жестко заданные безопасные часовые пояса и локали.

### 🌐 Универсальное ядро туннелирования
Встроенный, надежный сетевой стек:
- **Интеграция VLESS / Xray** — Подключение напрямую к продвинутым XTLS протоколам для обхода самых жестких блокировок DPI (ТМЧУ/Роскомнадзор).
- **Нативная поддержка SOCKS5 & HTTP** — Полный перехват и туннелирование всего трафика внутри Chromium.
- **Распознавание ссылок Telegram** — Просто нажмите на любую ссылку `tg://proxy?server=...` для мгновенного подключения к MTProto.
- **Пинг-аналитика (Ping-Test)** — Моментальная проверка валидности и задержки ICMP до прокси прямо в приложении.

### ⚔️ Защита нулевого кольца (Zero Compromise)
Абсолютная изоляция аппаратной части Android:
- **WebRTC Kill-Switch** — Полное уничтожение прототипов `RTCPeerConnection` для предотвращения утечек реального IP через STUN/TURN протоколы.
- **Сенсорная стерилизация** — Удаляет `DeviceMotionEvent` и `DeviceOrientationEvent` — трекеры не узнают, как вы держите телефон.
- **Жёсткая блокировка Media** — Перехват `navigator` API для глухого отказа доступа к камере, микрофону и GPS (геолокации). Сайты получают тихий отказ и даже не показывают нативные запросы разрешений Android.
- **Очистка X-Android заголовков** — Ядро автоматически внедряется в `fetch` и `XMLHttpRequest`, бесшумно удаляя любые системные заголовки вроде `X-Android-Package`, выдающие WebView.

---

## 🛠 Сборка и установка для разработчиков

| Инструмент | Поддерживаемые версии |
|---|---|
| **Среда разработки** | Android Studio Jellyfish (или новее) |
| **Java** | JDK 17 |
| **SDK** | Android SDK 26+ (Target 34) |
| **Сборщик** | Gradle 8.2+ |

### Быстрый старт (CLI)

```bash
git clone https://github.com/Genuys/PhantomMAX.git
cd PhantomMAX
./gradlew assembleRelease
```

*Готовый APK будет лежать в директории: `app/build/outputs/apk/release/`*

---

## 🧪 Как PhantomMAX обманывает трекеры?

Обычные браузеры (и другие Anti-Detect WebView) оставляют массивный след системной идентификации Android. Взгляните на разницу:

```text
[Tracker] запрашивает navigator.userAgentData.getHighEntropyValues()
   ↓
[PhantomMAX JS Core] нативно перехватывает промис проверки
   ↓
Возвращает: Promise<Object> {
  architecture: "x86",
  bitness: "64",
  brands: ["Google Chrome", "Chromium"],
  mobile: false,
  platform: "Windows", ...
}
   ↓
[Tracker] анализирует пиксели CanvasRenderingContext2D.getImageData()
   ↓
[PhantomMAX JS Core] на лету применяет алгоритмический Xorshift32 крипто-шум
   ↓
[Tracker] фиксирует "чистый", но 100% фейковый и уникальный отпечаток устройства.
```

---

## 📂 Структура системы

```text
PhantomMAX/app/src/main/java/com/phantommax/app/
├── SpoofingEngine.kt        # Сердце защиты: инъекции JS-ядра и SPA-безопасные proxy-отделы
├── PhantomWebViewClient.kt  # Перехватчик трафика, ошибок HTTP и сборщик WebRTC
├── HeaderManager.kt         # Эмуляция Client Hints заголовков Chrome 134 Десктоп
├── ProxyManager.kt          # Подсистема туннелей VLESS, SOCKS5 и HTTP
├── ProxyConfig.kt           # Интеллектуальный парсер прокси-ссылок и TG-URI
├── MainActivity.kt          # Контроллер UI графического интерфейса
└── PhantomApp.kt            # Изолированный Application, управление сидами и флагами сессий
```

---

## 🤝 Сообщество и поддержка

<div align="center">

[![Telegram](https://img.shields.io/badge/Telegram-TgUnlock2026-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TgUnlock2026)
[![GitHub](https://img.shields.io/badge/GitHub-Genuys-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Genuys)

</div>

<div align="center">
<br/>
<img src="https://capsule-render.vercel.app/api?type=waving&color=7C4DFF&height=100&section=footer" width="100%"/>
<sub>Engineered for absolute privacy. <b>PhantomMAX Team</b> 2026</sub>
</div>
