# SCOPE 工程备注（v1.1.1）

## 导航
- `ui/Route.kt`：`@Serializable sealed interface Route : NavKey`，全应用唯一路由表。
- `ui/AppRoot.kt`：四个主入口各一条 `NavController` 栈，只有当前栈由 `NavDisplay` 渲染。
- 转场 `NavTransitions.MiuixDefault`；减少动效或系统动画缩放为 0 时用 `NavTransitions.None`。
- 返回优先级：Dialog / BottomSheet → 当前栈 pop → 非首页标签根页面回首页 → 首页交系统退出。

## 设置
五个分类：授权与系统 / 外观与交互 / 安全与操作 / 数据与维护 / 关于与设备；三级页面含设备信息、关于、许可。

## 权限在应用内完成
- AppOps 类：`appops set`。
- 通知使用权 / 无障碍：`settings put secure enabled_*`，自动定位应用声明的服务组件。
- 电池优化：`cmd deviceidle whitelist +/-pkg`。
- 全部修改后重新读取校验，失败如实返回；普通模式下点击卡片提示授权，不再跳系统设置。

## 更新
- `core/Update.kt` 多源检查（jsDelivr 优先，raw / ghproxy / gitmirror 兜底），启动自动检查并弹 Miuix 更新面板。
- 下载按 URL 顺序重试，SHA-256 校验通过才允许安装；FileProvider + 系统安装器。
- 版本规则：小修复 0.01，大版本重构才用 2.00 编号；小版本说明统一「修复了一些已知问题」。
- 发布仓库：https://github.com/Mingqingke/SCOPE （源码 + update.json + releases）

## 图标
自适应图标：白底 + 黑色 S 形笔画（`ic_launcher_foreground.xml`，stroke 14.5、圆角端点），monochrome 复用同一前景。

## 未验证
- 无真机 / 模拟器：转场、预测性返回、Insets、真实授权与更新安装流程均未实机验证。
- Shizuku 私有 `newProcess` 反射调用未在授权设备实测。
- 场景编辑仍以冻结步骤为主；应用运行状态未实现。
