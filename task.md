# 整改任务追踪（对照《指纹浏览器专项整改指南》）

> 规则：每完成一个任务立即标注 ✅ 已完成。

## P0 安全基线

- [x] ✅ 已完成：关闭 WebView SSL 错误默认放行（`onReceivedSslError` 从 `proceed` 改为 `cancel`）。
- [x] ✅ 已完成：移除 VLESS trust-all 证书策略，改为系统默认 TLS 信任链并增加 Hostname 校验。
- [x] ✅ 已完成：关闭应用全局明文流量（Manifest `usesCleartextTraffic=false`）。
- [x] ✅ 已完成：关闭 network security config 的 base-config 明文放行（`cleartextTrafficPermitted=false`）。

## P1 可维护性与行为一致性

- [x] ✅ 已完成：接入 `TrackerBlocker` 到 `shouldInterceptRequest`，对命中追踪请求返回空响应。
- [x] ✅ 已完成：主入口加载 URL 时注入 HeaderManager 生成的请求头（`loadUrl(url, headers)`）。
- [x] ✅ 已完成：将 `MainActivity` 拆分为控制器/VM，降低耦合。
  - [x] ✅ 子任务完成：提取 WebView 生命周期控制器（`WebViewLifecycleController`）。
  - [x] ✅ 子任务完成：提取设置页状态管理到 ViewModel（`SettingsStateViewModel`，统一状态拼装）。
- [x] ✅ 已完成：将 Thread 模型迁移至 Coroutines + 可取消任务（`ProxyManager` 网络任务协程化 + `disconnect` 取消子任务）。

## P2 指纹策略工程化

- [x] ✅ 已完成：拆分 `SpoofingEngine` 巨型脚本为模块化注入单元。
  - [x] ✅ 子任务完成：拆分 UA/ClientHints 模块。
  - [x] ✅ 子任务完成：拆分 Canvas/WebGL 模块。
  - [x] ✅ 子任务完成：拆分 RTC/Permissions/Timezone 模块。
- [x] ✅ 已完成：建立注入成功率/异常率监控指标（`SpoofMetrics` + 设置页日志展示）。
- [x] ✅ 已完成：建立检测站点回归基线并加入发布前检查（`regression-baseline.md`）。

## P3 扩展能力

- [x] ✅ 已完成：按域名策略加载不同 spoof profile（`SpoofProfileManager`，含 STRICT/BALANCED/SAFE_NO_SPOOF）。
- [x] ✅ 已完成：代理分流策略（直连/代理）与可观测面板（`ProxyManager` 路由规则 + 状态页 Route 展示）。

## 验证与发布门禁

- [x] ✅ 已完成：已在可联网环境配置 Android SDK（API 34 / Build Tools 34.0.0）并执行 `:app:assembleDebug` 编译通过。
- [x] ✅ 已完成：执行 `:app:testDebugUnitTest`（当前项目无单测源码，任务为 `NO-SOURCE`，构建通过）。
