# Release Checklist（最终发布前清单）

## 0. 基础信息
- [ ] 记录构建版本号（versionName/versionCode）
- [ ] 记录发布日期与责任人

## 1. 构建与质量门禁
- [ ] `ANDROID_SDK_ROOT=/workspace/android-sdk ANDROID_HOME=/workspace/android-sdk gradle --no-daemon :app:assembleDebug` 成功
- [ ] `ANDROID_SDK_ROOT=/workspace/android-sdk ANDROID_HOME=/workspace/android-sdk gradle --no-daemon :app:testDebugUnitTest` 成功（当前可为 `NO-SOURCE`）
- [ ] 构建产物可安装并可启动

## 2. 安全门禁
- [ ] SSL 异常站点被拦截（非自动放行）
- [ ] 不存在 cleartext 主请求
- [ ] VLESS 在错误证书链下握手失败
- [ ] 代理断开时有清晰状态提示

## 3. 核心功能门禁
- [ ] `web.max.ru` 首屏加载成功
- [ ] 登录流程成功
- [ ] 代理连接（SOCKS5/HTTP/VLESS 至少覆盖 1~2 种）
- [ ] QR 检测/保存流程可用（如该流程仍在发布范围）

## 4. 指纹与策略门禁
- [ ] `SpoofProfileManager` 命中策略符合预期
- [ ] `SpoofMetrics` 显示 Attempts/SuccessRate 正常
- [ ] Tracker 拦截未导致核心业务不可用

## 5. 生命周期与稳定性门禁
- [ ] 前后台切换网络阻断与恢复行为正确
- [ ] `destroyOnMinimize` 开启时可重建 WebView
- [ ] 设置页状态一致（profile/route/status/log）

## 6. 发布文档门禁
- [ ] `task.md` 状态已更新为最新
- [ ] `变更影响与回归建议.md` 已审阅
- [ ] 历史文档已归档（`docs/archive/`）

## 7. 发布结论
- [ ] 结论：可发布 / 延后发布
- [ ] 风险备注：
- [ ] 回滚方案（版本号/提交号）：

