# SCOPE 工程备注（v1.1.0）

## 导航
- `ui/Route.kt`：`@Serializable sealed interface Route : NavKey`，全应用唯一路由定义。
- `ui/AppRoot.kt`：四个主入口各自一条 `NavController` 返回栈；只用 Miuix `NavDisplay` 渲染当前栈，页面进出、视差、预测性返回与滑动返回交给 `NavTransitions.MiuixDefault`。
- 系统返回顺序：Dialog/BottomSheet（Miuix 自身）→ 当前栈 pop（NavDisplay）→ 非首页标签根页面回首页（单个根级 BackHandler）→ 首页根页面交给系统退出。
- 减少动效或系统动画缩放为 0 时切换为 `NavTransitions.None`。

## 设置中心
- 首页只放 5 个分类入口：授权与系统 / 外观与交互 / 安全与操作 / 数据与维护 / 关于与设备。
- 二级页面：`SettingsSystemScreen`（状态卡 + 后端优先级 + 授权详情 + 能力 + 重新检测）、`SettingsAppearanceScreen`、`SettingsSafetyScreen`、`SettingsDataScreen`、`SettingsAboutDeviceScreen`。
- 三级页面：`DeviceInfoScreen`（概览/系统/硬件/存储/授权）、`AboutScopeScreen`、`LicensesScreen`。
- `Prefs` 迁移旧键 `tilt_feedback` → `card_feedback`，升级不重置设置。

## 行为与持久化
- 卡片反馈 0 Sink / 1 Tilt / 2 关闭；减少动效时 Tilt 降级为 Sink。
- 确认策略：冻结、权限/AppOps、批量执行三档，均真实拦截对应操作。
- 备份：`core/Backup.kt` 用 SAF 导出/导入 JSON（设置、场景、记录），导入前显示摘要并确认，不触碰应用与系统数据。
- 操作记录支持类型/结果/关键字筛选与逐条恢复。

## 未验证 / 限制
- 构建环境无真机与模拟器：页面转场、预测性返回、系统栏 Insets、真实授权流程均未在设备上验证。
- Shizuku 13 的 `newProcess` 为私有方法，通过反射调用，未在授权设备上实测。
- 场景步骤 UI 仍以「冻结应用」为主，`ScenarioStep` 已支持 APPOP 但暂无编辑入口。
- 应用运行状态未实现（需要 usage stats 或 root ps）。
