# 整改任务追踪（收尾归档版）

> 状态：**已收尾**（P0/P1/P2/P3 与验证门禁全部完成）。

## 1) 任务总览

- [x] P0 安全基线完成
- [x] P1 可维护性与行为一致性完成
- [x] P2 指纹策略工程化完成
- [x] P3 扩展能力完成
- [x] 验证与发布门禁完成

## 2) 完成清单（摘要）

### P0 安全基线
- [x] 关闭 WebView SSL 错误默认放行（`cancel`）
- [x] 移除 VLESS trust-all，改为系统 TLS + Hostname 校验
- [x] 关闭全局 cleartext

### P1 可维护性
- [x] TrackerBlocker 接入请求拦截
- [x] HeaderManager 接入主入口加载
- [x] `MainActivity` 拆分（`WebViewLifecycleController` + `SettingsStateViewModel`）
- [x] `ProxyManager` 线程模型迁移到 Coroutines（可取消）

### P2 指纹策略工程化
- [x] `SpoofingEngine` 模块化脚本拼装（UA/CH、Canvas/WebGL、RTC/Permissions/Timezone）
- [x] `SpoofMetrics` 注入成功率/失败率监控
- [x] 回归基线文档落地（见 `regression-baseline.md`）

### P3 扩展能力
- [x] `SpoofProfileManager` 域名策略（STRICT/BALANCED/SAFE_NO_SPOOF）
- [x] 代理分流规则与状态可观测（DIRECT/PROXY）

## 3) 归档与文档清理

为避免根目录文档过多，历史分析文档已归档到 `docs/archive/`：

- `docs/archive/项目分析与优化建议.md`
- `docs/archive/指纹浏览器专项整改指南.md`

新增收尾文档：

- `变更影响与回归建议.md`
- `release-checklist.md`

## 4) 后续维护建议（非阻塞）

- 保持每次功能改动后执行 `assembleDebug` + 回归清单抽样。
- 在新增站点策略时，优先扩展 `SpoofProfileManager` 并补充回归记录。
- 若进入正式发布，建议补充最小单元测试集（当前为 `NO-SOURCE`）。
