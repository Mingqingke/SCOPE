package com.scope.app.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.BuildConfig
import com.scope.app.core.Backup
import com.scope.app.core.BackendStatus
import com.scope.app.core.Device
import com.scope.app.core.DeviceSnapshot
import com.scope.app.core.History
import com.scope.app.core.Prefs
import com.scope.app.core.ScenarioStore
import com.scope.app.ui.Route
import com.scope.app.ui.components.ScreenTitle
import com.scope.app.ui.components.pressFeedback
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/* ---------------- 设置首页：只有分类入口 ---------------- */

@Composable
fun SettingsHomeScreen(onOpen: (Route) -> Unit) {
    val scope = LocalScope.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "设置", subtitle = "SCOPE 系统控制中心")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CategoryEntry(
                    icon = MiuixIcons.Lock,
                    tint = scope.green,
                    title = "授权与系统",
                    summary = "授权后端、系统能力与服务状态",
                    onClick = { onOpen(Route.SettingsSystem) },
                )
            }
            item {
                CategoryEntry(
                    icon = MiuixIcons.Theme,
                    tint = scope.blue,
                    title = "外观与交互",
                    summary = "主题、卡片反馈与动画效果",
                    onClick = { onOpen(Route.SettingsAppearance) },
                )
            }
            item {
                CategoryEntry(
                    icon = MiuixIcons.Tune,
                    tint = scope.amber,
                    title = "安全与操作",
                    summary = "操作确认、执行策略与风险控制",
                    onClick = { onOpen(Route.SettingsSafety) },
                )
            }
            item {
                CategoryEntry(
                    icon = MiuixIcons.Backup,
                    tint = scope.blue,
                    title = "数据与维护",
                    summary = "操作记录、数据备份与运行诊断",
                    onClick = { onOpen(Route.SettingsData) },
                )
            }
            item {
                CategoryEntry(
                    icon = MiuixIcons.Info,
                    tint = scope.textSecondary,
                    title = "关于与设备",
                    summary = "设备信息、版本更新与项目说明",
                    onClick = { onOpen(Route.SettingsAboutDevice) },
                )
            }
            item {
                Text(
                    text = "SCOPE ${BuildConfig.VERSION_NAME} · 作者 青",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CategoryEntry(
    icon: ImageVector,
    tint: Color,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val scope = LocalScope.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = scope.card),
        pressFeedbackType = pressFeedback(),
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = tint)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = scope.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                MiuixIcons.ChevronForward,
                null,
                Modifier.size(15.dp),
                tint = scope.textSecondary.copy(alpha = 0.5f),
            )
        }
    }
}

/* ---------------- 授权与系统 ---------------- */

@Composable
fun SettingsSystemScreen(
    status: BackendStatus,
    device: DeviceSnapshot,
    onBack: () -> Unit,
    onOpen: (Route) -> Unit,
    onRefresh: () -> Unit,
    notify: (String) -> Unit,
) {
    val scope = LocalScope.current
    val backendPref by Prefs.backendPreference.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val capabilityCount = remember(status, device) {
        com.scope.app.core.Capabilities.evaluate(context, status, device).count { it.available }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "授权与系统", subtitle = "当前身份 ${status.identity}", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    colors = CardDefaults.defaultColors(color = scope.mint),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(status.active.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                text = when {
                                    status.active == com.scope.app.core.BackendType.ROOT -> "root"
                                    status.active == com.scope.app.core.BackendType.SHIZUKU -> "shell"
                                    else -> "受限"
                                },
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(status.identity, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            MiniMetric("可用能力", "$capabilityCount 项")
                            MiniMetric("Shizuku", if (status.shizukuReady) "已连接" else "未就绪")
                            MiniMetric("Root", if (status.rootGranted) "已授权" else "未授权")
                        }
                        if (status.lastCheckedAt > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "最近检测 ${android.text.format.DateFormat.format("HH:mm:ss", status.lastCheckedAt)}",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        }
                    }
                }
            }
            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "后端优先级",
                            summary = when (backendPref) {
                                1 -> "优先使用 Root"
                                2 -> "优先使用 Shizuku"
                                else -> "自动（Root 优先）"
                            },
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.green) },
                            onClick = { dialog = true },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "授权详情",
                            summary = "Shizuku / Root 真实状态与授权申请",
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.blue) },
                            onClick = { onOpen(Route.Auth) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "系统能力",
                            summary = "逐项检查当前设备可执行的操作",
                            startAction = { PrefIcon(MiuixIcons.Tune, scope.amber) },
                            onClick = { onOpen(Route.Capabilities) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "重新检测",
                            summary = "重新读取安装、服务、授权与身份",
                            startAction = { PrefIcon(MiuixIcons.Refresh, scope.textSecondary) },
                            onClick = onRefresh,
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        StatusLine("Shizuku 安装", status.shizukuInstalled, if (status.shizukuInstalled) "已安装" else "未安装")
                        StatusLine("Shizuku 服务", status.shizukuRunning, if (status.shizukuRunning) "Binder 已连接" else "未运行")
                        StatusLine("Shizuku 授权", status.shizukuGranted, if (status.shizukuGranted) "已授权 SCOPE" else "未授权")
                        StatusLine("Root 授权", status.rootGranted, if (status.rootGranted) "已获得 root" else "未获得")
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("说明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Shizuku 提供 ADB 级 shell 权限，Root 提供 root 权限，二者不等同。SCOPE 只执行平台允许的操作，系统拒绝时如实显示失败原因。",
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = dialog,
        title = "后端优先级",
        summary = "只影响已授权的后端顺序，不会提升权限",
        onDismissRequest = { dialog = false },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            listOf("自动（Root 优先）" to 0, "优先 Root" to 1, "优先 Shizuku" to 2).forEach { (label, value) ->
                TextButton(
                    text = label,
                    onClick = {
                        Prefs.setBackendPreference(value)
                        dialog = false
                        notify("已切换为 $label")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean, value: String) {
    val scope = LocalScope.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (ok) scope.green else scope.textSecondary.copy(alpha = 0.35f)),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, color = scope.textSecondary)
    }
}

/* ---------------- 外观与交互 ---------------- */

@Composable
fun SettingsAppearanceScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val themeMode by Prefs.themeMode.collectAsState()
    val cardFeedback by Prefs.cardFeedback.collectAsState()
    val reduceMotion by Prefs.reduceMotion.collectAsState()
    var themeDialog by remember { mutableStateOf(false) }
    var feedbackDialog by remember { mutableStateOf(false) }
    val animatorScale = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "外观与交互", subtitle = "主题、卡片反馈与动画效果", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "深色模式",
                            summary = when (themeMode) {
                                1 -> "浅色"
                                2 -> "深色"
                                else -> "跟随系统"
                            },
                            startAction = { PrefIcon(MiuixIcons.Theme, scope.blue) },
                            onClick = { themeDialog = true },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "卡片按压反馈",
                            summary = when (cardFeedback) {
                                1 -> "Tilt 倾斜"
                                2 -> "关闭"
                                else -> "Sink 下沉"
                            },
                            startAction = { PrefIcon(MiuixIcons.Tune, scope.amber) },
                            onClick = { feedbackDialog = true },
                        )
                        PrefDivider()
                        SwitchPreference(
                            title = "减少动效",
                            summary = "关闭页面滑动转场与装饰动画",
                            checked = reduceMotion,
                            startAction = { PrefIcon(MiuixIcons.Refresh, scope.textSecondary) },
                            onCheckedChange = {
                                Prefs.setReduceMotion(it)
                                notify(if (it) "已开启减少动效" else "已恢复标准动效")
                            },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        StatusLine(
                            "系统动画缩放",
                            animatorScale > 0f,
                            if (animatorScale == 0f) "系统已关闭动画" else "${"%.1f".format(animatorScale)}×",
                        )
                        StatusLine(
                            "页面转场",
                            !reduceMotion,
                            if (reduceMotion) "已精简" else "Miuix 默认转场",
                        )
                        StatusLine("卡片反馈范围", cardFeedback != 2, if (cardFeedback == 2) "已关闭" else "首页卡片与分类入口")
                    }
                }
            }
            item {
                HintBar("开启减少动效后，页面切换不播放滑动转场，Tilt 反馈同时停用；系统已关闭动画时应用同样不播放转场。")
            }
        }
    }

    OverlayDialog(
        show = themeDialog,
        title = "深色模式",
        summary = "选择后立即生效，并同步系统栏图标明暗",
        onDismissRequest = { themeDialog = false },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            listOf("跟随系统" to 0, "浅色" to 1, "深色" to 2).forEach { (label, value) ->
                TextButton(
                    text = label,
                    onClick = {
                        Prefs.setThemeMode(value)
                        themeDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    OverlayDialog(
        show = feedbackDialog,
        title = "卡片按压反馈",
        summary = "只作用于使用 Miuix Card 的卡片，不影响设置行",
        onDismissRequest = { feedbackDialog = false },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            listOf("Sink 下沉" to 0, "Tilt 倾斜" to 1, "关闭" to 2).forEach { (label, value) ->
                TextButton(
                    text = label,
                    onClick = {
                        Prefs.setCardFeedback(value)
                        feedbackDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/* ---------------- 安全与操作 ---------------- */

@Composable
fun SettingsSafetyScreen(onBack: () -> Unit) {
    val scope = LocalScope.current
    val confirmRisky by Prefs.confirmRisky.collectAsState()
    val confirmPermission by Prefs.confirmPermission.collectAsState()
    val confirmBatch by Prefs.confirmBatch.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "安全与操作", subtitle = "操作确认、执行策略与风险控制", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        SwitchPreference(
                            title = "冻结 / 停用确认",
                            summary = "冻结应用前显示影响范围并二次确认",
                            checked = confirmRisky,
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.amber) },
                            onCheckedChange = { Prefs.setConfirmRisky(it) },
                        )
                        PrefDivider()
                        SwitchPreference(
                            title = "权限与 AppOps 修改确认",
                            summary = "修改权限、AppOps、特殊访问前二次确认",
                            checked = confirmPermission,
                            startAction = { PrefIcon(MiuixIcons.Tune, scope.blue) },
                            onCheckedChange = { Prefs.setConfirmPermission(it) },
                        )
                        PrefDivider()
                        SwitchPreference(
                            title = "批量执行确认",
                            summary = "执行场景前显示步骤与影响范围",
                            checked = confirmBatch,
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.green) },
                            onCheckedChange = { Prefs.setConfirmBatch(it) },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "执行策略（始终启用）",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                        )
                        StatusLine("操作前读取原始状态", true, "已启用")
                        StatusLine("操作后重新验证结果", true, "已启用")
                        StatusLine("写入操作记录", true, "已启用")
                        StatusLine("可恢复操作保存恢复信息", true, "已启用")
                    }
                }
            }
            item {
                HintBar("确认开关只影响是否弹出二次确认，不会跳过状态读取、结果验证与记录；不可恢复的操作仍会明确提示。")
            }
        }
    }
}

/* ---------------- 数据与维护 ---------------- */

@Composable
fun SettingsDataScreen(
    onBack: () -> Unit,
    onOpen: (Route) -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    val records by History.records.collectAsState()
    val scenarios by ScenarioStore.list.collectAsState()
    var pendingRestore by remember { mutableStateOf<com.scope.app.core.BackupBundle?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = Backup.write(context, uri, Backup.encode())
        notify(if (ok) "已导出 ${records.size} 条记录、${scenarios.size} 个场景" else "导出失败，无法写入所选文件")
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = Backup.read(context, uri)
        if (text == null) {
            notify("无法读取该文件")
            return@rememberLauncherForActivityResult
        }
        Backup.parse(text).fold(
            onSuccess = { pendingRestore = it },
            onFailure = { notify("备份无法解析：${it.message ?: "格式不正确"}") },
        )
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "数据与维护", subtitle = "操作记录、数据备份与运行诊断", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "操作记录",
                            summary = "${records.size} 条 · 支持筛选与恢复",
                            startAction = { PrefIcon(MiuixIcons.Backup, scope.blue) },
                            onClick = { onOpen(Route.History) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "运行诊断",
                            summary = "设备、权限、存储与模块状态",
                            startAction = { PrefIcon(MiuixIcons.Info, scope.amber) },
                            onClick = { onOpen(Route.Diagnostics) },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "导出数据",
                            summary = "保存设置、场景与操作记录为 JSON",
                            startAction = { PrefIcon(MiuixIcons.Backup, scope.green) },
                            onClick = { exportLauncher.launch(Backup.fileName()) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "从备份恢复",
                            summary = "导入前会显示内容摘要并确认",
                            startAction = { PrefIcon(MiuixIcons.Refresh, scope.blue) },
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("备份内容", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "设置 ${Prefs.exportMap().size} 项 · 场景 ${scenarios.size} 个 · 记录 ${records.size} 条\n恢复只覆盖以上数据，不会改动已安装应用、系统设置或权限。",
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
    }

    val bundle = pendingRestore
    OverlayDialog(
        show = bundle != null,
        title = "恢复备份",
        summary = bundle?.let {
            "包含 ${it.scenarios.size} 个场景、${it.history.size} 条记录，备份于 ${
                android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", it.createdAt)
            }"
        },
        onDismissRequest = { pendingRestore = null },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "恢复会覆盖当前设置、场景与操作记录，此操作不可撤销。",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = scope.textSecondary,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton("取消", onClick = { pendingRestore = null }, modifier = Modifier.weight(1f))
                TextButton(
                    text = "覆盖并恢复",
                    onClick = {
                        val target = pendingRestore ?: return@TextButton
                        pendingRestore = null
                        coroutine.launch {
                            val result = Backup.apply(context, target)
                            notify(result.message)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/* ---------------- 关于与设备 ---------------- */

@Composable
fun SettingsAboutDeviceScreen(onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val scope = LocalScope.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "关于与设备", subtitle = "设备信息、版本与项目说明", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "设备信息",
                            summary = "型号、系统、硬件与存储",
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.blue) },
                            onClick = { onOpen(Route.DeviceInfo) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "关于 SCOPE",
                            summary = "版本 ${BuildConfig.VERSION_NAME} · 作者 青",
                            startAction = { PrefIcon(MiuixIcons.Info, scope.green) },
                            onClick = { onOpen(Route.About) },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "开源许可与依赖",
                            summary = "Miuix、Shizuku、libsu 等",
                            startAction = { PrefIcon(MiuixIcons.Info, scope.amber) },
                            onClick = { onOpen(Route.Licenses) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoScreen(device: DeviceSnapshot, status: BackendStatus, onBack: () -> Unit) {
    val scope = LocalScope.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "设备信息", subtitle = "数值来自本机读取", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GroupCard("设备") {
                    DeviceRow("型号", device.model)
                    DeviceRow("厂商", device.manufacturer)
                }
            }
            item {
                GroupCard("系统") {
                    DeviceRow("Android", device.androidRelease)
                    DeviceRow("API", device.sdk.toString())
                    DeviceRow("厂商系统", device.osName)
                    DeviceRow("系统版本", device.osVersion)
                }
            }
            item {
                GroupCard("硬件") {
                    DeviceRow("处理器", device.cpu)
                    DeviceRow("内存", if (device.ramTotal > 0) Device.formatBytes(device.ramTotal) else "")
                    DeviceRow("分辨率", device.resolution)
                }
            }
            item {
                GroupCard("存储") {
                    DeviceRow("已用", Device.formatBytes(device.storageTotal - device.storageFree))
                    DeviceRow("可用", Device.formatBytes(device.storageFree))
                    DeviceRow("总容量", Device.formatBytes(device.storageTotal))
                    if (device.storageTotal > 0) {
                        val ratio = (device.storageTotal - device.storageFree).toFloat() / device.storageTotal.toFloat()
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(scope.hairline),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(scope.blue),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(title: String, content: @Composable () -> Unit) {
    val scope = LocalScope.current
    PrefCard {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = scope.textSecondary,
                fontWeight = FontWeight.Medium,
            )
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    val scope = LocalScope.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 13.sp, color = scope.textSecondary, modifier = Modifier.width(104.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun AboutScopeScreen(device: DeviceSnapshot, onBack: () -> Unit, notify: (String) -> Unit) {
    val scope = LocalScope.current
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "关于 SCOPE", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrefCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandMark()
                    Spacer(Modifier.height(14.dp))
                    Text("SCOPE", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Android System Control Center", fontSize = 12.sp, color = scope.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME} · 作者 青",
                        fontSize = 12.sp,
                        color = scope.textSecondary,
                    )
                }
            }
            PrefCard {
                Column {
                    UpdateCheckRow(notify = notify)
                    PrefDivider()
                    ArrowPreference(
                        title = "GitHub 仓库",
                        summary = "github.com/Mingqingke/SCOPE",
                        startAction = { PrefIcon(MiuixIcons.Link, scope.blue) },
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Mingqingke/SCOPE"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        },
                    )
                }
            }
            PrefCard {
                Column(Modifier.padding(18.dp)) {
                    Text("构建信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    AboutLine("包名", BuildConfig.APPLICATION_ID)
                    AboutLine("版本号", BuildConfig.VERSION_CODE.toString())
                    AboutLine("构建类型", BuildConfig.BUILD_TYPE)
                    AboutLine("运行设备", device.model.ifBlank { "—" })
                    AboutLine("系统", "${device.osName} ${device.osVersion}".trim())
                }
            }
            PrefCard {
                Column(Modifier.padding(18.dp)) {
                    Text("说明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "SCOPE 在普通模式、Shizuku 与 Root 三种身份下提供系统控制能力。所有修改都会读取原始状态、复查结果并写入操作记录；系统不允许的操作会如实提示失败原因。",
                        fontSize = 12.sp,
                        color = scope.textSecondary,
                        lineHeight = 19.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BrandMark() {
    val scope = LocalScope.current
    Canvas(Modifier.size(64.dp)) {
        val stroke = size.minDimension * 0.075f
        drawCircle(
            color = scope.green,
            radius = size.minDimension / 2f - stroke,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(color = scope.green, radius = size.minDimension * 0.1f, center = Offset(size.width / 2f, size.height / 2f))
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = LocalScope.current.textSecondary, modifier = Modifier.width(88.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val scope = LocalScope.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "开源许可", subtitle = "SCOPE 使用的组件与许可", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("第三方依赖", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        AboutLine("Miuix", "0.9.4 · Apache-2.0")
                        AboutLine("Shizuku API", "13.1.5 · Apache-2.0")
                        AboutLine("libsu", "6.0.0 · Apache-2.0")
                        AboutLine("Kotlin / Compose", "2.4.20 / BOM 2026.09.00 · Apache-2.0")
                        AboutLine("kotlinx.serialization", "1.11.0 · Apache-2.0")
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("说明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "以上项目按 Apache License 2.0 使用，未修改其品牌标识。完整许可文本随各项目源码发布。",
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
    }
}

fun shareText(context: Context, title: String, text: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newPlainText(title, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
