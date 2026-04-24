<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=7C4DFF&height=250&section=header&text=PhantomMAX&fontSize=80&fontColor=FFFFFF&animation=fadeIn&fontAlignY=38&desc=Advanced%20Kernel-Level%20Android%20Stealth%20Browser&descAlignY=55&descSize=20" width="100%"/>


[![Android](https://img.shields.io/badge/平台-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/语言-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/许可证-MIT-blue?style=for-the-badge)](LICENSE)
[![Version](https://img.shields.io/badge/版本-2.0.0_Pro-success?style=for-the-badge)](https://github.com/Genuys/PhantomMAX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-频道-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TgUnlock2026)

> **PhantomMAX** 是面向 Android 的新一代隐身浏览器。它从底层重构 WebView 渲染机制与 JavaScript 上下文，让移动设备在指纹特征上高度拟真为一台高性能 Windows PC。
> 通过内核级 WebGL 伪装与独特的 Canvas 加密噪声，可绕过高级指纹检测算法（如 Cloudflare Turnstile、Akamai、DataDome），同时保持对复杂 SPA 框架（React、Svelte、Vue）的 100% 兼容性。

[![Download APK](https://img.shields.io/badge/⬇️%20下载%20APK-v2.0.0-7C4DFF?style=for-the-badge)](https://github.com/Genuys/PhantomMAX-Android-Stealth-Browser/releases/tag/1.0.0)

</div>

---

## ⚡ 隐身内核技术

### 🎭 深度伪装引擎（Zero-Leak）
与仅修改 User-Agent 的常规方案不同，PhantomMAX 会直接操控浏览器 JavaScript 原型链：
- **Canvas & WebGL 加密噪声** —— 在 `toDataURL` 与 `getImageData` 中注入经过数学校准、肉眼不可见的伪随机扰动。每次会话指纹都唯一，且不会产生“脏”伪影。
- **AudioContext 掩码处理** —— 对 `getChannelData` 与 `AnalyserNode` 缓冲区进行智能失真，抵御音频指纹追踪。
- **SPA 兼容模式** —— 基于 `writable: true` 的高级 proxy 拦截机制。即便是苛刻的 React/Svelte 站点，在校验伪造浏览器 API 时也不会崩溃。
- **硬件与设备伪装** —— 模拟 8 核 CPU、8GB RAM、精确的 Intel UHD 630 WebGL 特征，并强制使用安全时区与区域设置。

### 🌐 通用隧道内核
内置稳定可靠的网络栈：
- **VLESS / Xray 集成** —— 直接连接高级 XTLS 协议，绕过高强度 DPI 封锁。
- **原生支持 SOCKS5 & HTTP** —— 在 Chromium 内实现全流量拦截与隧道转发。
- **Telegram 链接识别** —— 点击任意 `tg://proxy?server=...` 链接即可即时接入 MTProto。
- **延迟分析（Ping-Test）** —— 在应用内快速检测代理可用性与 ICMP 延迟。

### ⚔️ 零环防护（Zero Compromise）
对 Android 硬件层进行彻底隔离：
- **WebRTC Kill-Switch** —— 完整移除 `RTCPeerConnection` 原型，防止通过 STUN/TURN 泄露真实 IP。
- **传感器净化** —— 禁用 `DeviceMotionEvent` 与 `DeviceOrientationEvent`，避免站点推断持机姿态。
- **媒体硬阻断** —— 拦截 `navigator` API，静默拒绝相机、麦克风与 GPS（地理位置）访问；站点仅收到拒绝结果，不会触发原生 Android 权限弹窗。
- **X-Android 请求头清洗** —— 内核自动注入 `fetch` 与 `XMLHttpRequest`，静默移除 `X-Android-Package` 等暴露 WebView 身份的系统头。

---

## 🛠 面向开发者的构建与安装

| 工具 | 支持版本 |
|---|---|
| **开发环境** | Android Studio Jellyfish（或更新版本） |
| **Java** | JDK 17 |
| **SDK** | Android SDK 26+（Target 34） |
| **构建工具** | Gradle 8.2+ |

### 快速开始（CLI）

```bash
git clone https://github.com/Genuys/PhantomMAX.git
cd PhantomMAX
./gradlew assembleRelease
```

*构建完成后的 APK 位于：`app/build/outputs/apk/release/`*

---

## 🧪 PhantomMAX 如何欺骗追踪器？

普通浏览器（以及其他 Anti-Detect WebView）通常会留下大量 Android 系统识别痕迹。对比如下：

```text
[Tracker] 请求 navigator.userAgentData.getHighEntropyValues()
   ↓
[PhantomMAX JS Core] 原生拦截该检测 Promise
   ↓
返回：Promise<Object> {
  architecture: "x86",
  bitness: "64",
  brands: ["Google Chrome", "Chromium"],
  mobile: false,
  platform: "Windows", ...
}
   ↓
[Tracker] 分析 CanvasRenderingContext2D.getImageData() 像素
   ↓
[PhantomMAX JS Core] 实时应用基于 Xorshift32 的算法加密噪声
   ↓
[Tracker] 记录到“干净”、但 100% 伪造且唯一的设备指纹。
```

---

## 📂 系统结构

```text
PhantomMAX/app/src/main/java/com/phantommax/app/
├── SpoofingEngine.kt        # 防护核心：JS 内核注入与 SPA 安全 proxy 模块
├── PhantomWebViewClient.kt  # 流量与 HTTP 错误拦截器，含 WebRTC 防泄露处理
├── HeaderManager.kt         # 模拟 Chrome 134 桌面端 Client Hints 请求头
├── ProxyManager.kt          # VLESS、SOCKS5、HTTP 隧道子系统
├── ProxyConfig.kt           # 代理链接与 TG-URI 智能解析器
├── MainActivity.kt          # 图形界面 UI 控制器
└── PhantomApp.kt            # 隔离式 Application，管理随机种子与会话标志
```

---

## 🤝 社区与支持

<div align="center">

[![Telegram](https://img.shields.io/badge/Telegram-TgUnlock2026-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/TgUnlock2026)
[![GitHub](https://img.shields.io/badge/GitHub-Genuys-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Genuys)

</div>

<div align="center">
<br/>
<img src="https://capsule-render.vercel.app/api?type=waving&color=7C4DFF&height=100&section=footer" width="100%"/>
<sub>为绝对隐私而生。<b>PhantomMAX Team</b> 2026</sub>
</div>
