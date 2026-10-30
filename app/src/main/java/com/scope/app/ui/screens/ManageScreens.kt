package com.scope.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableIntStateOf
import top.yukonga.miuix.kmp.basic.TabRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.core.AppRepo
import com.scope.app.core.Backend
import com.scope.app.core.BackendStatus
import com.scope.app.core.Capability
import com.scope.app.core.Capabilities
import com.scope.app.core.Device
import com.scope.app.core.DeviceSnapshot
import com.scope.app.core.History
import com.scope.app.core.OpRecord
import com.scope.app.core.SpecialAccess
import com.scope.app.ui.Route
import com.scope.app.ui.components.EmptyHint
import com.scope.app.ui.components.ScreenTitle
import com.scope.app.ui.components.StatusDot
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ManageScreen(status: BackendStatus, onOpen: (Route) -> Unit) {
    val scope = LocalScope.current
    val records by History.records.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "管理", subtitle = "系统工作区与授权环境")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        MiuixArrowPreference(
                            title = "授权后端",
                            summary = "${status.active.label} · ${status.identity}",
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.green) },
                            onClick = { onOpen(Route.Auth) },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "系统能力诊断",
                            summary = "逐项检查当前设备可用的系统能力",
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.blue) },
                            onClick = { onOpen(Route.Capabilities) },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column {
                        MiuixArrowPreference(
                            title = "权限中心",
                            summary = "运行时权限与特殊访问总览",
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.blue) },
                            onClick = { onOpen(Route.PermissionCenter) },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "AppOps 管理",
                            summary = "选择应用后读取并修改 AppOps",
                            startAction = { PrefIcon(MiuixIcons.Tune, scope.amber) },
                            onClick = { onOpen(Route.Picker("AppOps", "选择一个应用", com.scope.app.ui.PickerTarget.APPOPS)) },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "组件管理",
                            summary = "Activity · Service · Receiver · Provider",
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.blue) },
                            onClick = { onOpen(Route.Picker("组件管理", "选择一个应用", com.scope.app.ui.PickerTarget.COMPONENTS)) },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "场景引擎",
                            summary = "创建并执行自定义系统方案",
                            startAction = { PrefIcon(MiuixIcons.Tasks, scope.green) },
                            onClick = { onOpen(Route.Scenarios) },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    MiuixArrowPreference(
                        title = "最近操作",
                        summary = records.firstOrNull()?.let {
                            "${it.title} · ${it.timeText()} · ${if (it.success) "成功" else "失败"}"
                        } ?: "还没有系统操作记录",
                        startAction = { PrefIcon(MiuixIcons.Backup, scope.textSecondary) },
                        onClick = { onOpen(Route.History) },
                    )
                }
            }
        }
    }
}

@Composable
fun PrefIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier
            .padding(end = 12.dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = tint)
    }
}

@Composable
fun AuthScreen(
    status: BackendStatus,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()

    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, _ -> onRefresh() }
        runCatching { rikka.shizuku.Shizuku.addRequestPermissionResultListener(listener) }
        onDispose { runCatching { rikka.shizuku.Shizuku.removeRequestPermissionResultListener(listener) } }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "授权与环境", subtitle = "当前身份 ${status.identity}", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        StateRow("Shizuku 安装", status.shizukuInstalled, if (status.shizukuInstalled) "已安装" else "未安装")
                        StateRow("Shizuku 服务", status.shizukuRunning, if (status.shizukuRunning) "Binder 已连接" else "未运行")
                        StateRow("Shizuku 授权", status.shizukuGranted, if (status.shizukuGranted) "已授权 SCOPE" else "未授权")
                        StateRow("Root 授权", status.rootGranted, if (status.rootGranted) "已获得 root" else "未获得")
                        StateRow(
                            "可执行系统操作",
                            status.privileged,
                            if (status.privileged) "以 ${status.active.label} 执行" else "普通模式受限",
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column {
                        MiuixArrowPreference(
                            title = "申请 Shizuku 授权",
                            summary = if (status.shizukuRunning) "在系统弹窗中允许 SCOPE" else "先启动 Shizuku 服务",
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.blue) },
                            onClick = {
                                if (!status.shizukuRunning) notify("Shizuku 服务未运行，请先启动服务")
                                else if (Backend.requestShizukuPermission()) notify("已发起授权请求，完成后点击重新检测")
                                else notify("无法发起授权请求")
                            },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "打开 Shizuku",
                            summary = "启动服务或调整授权",
                            startAction = { PrefIcon(MiuixIcons.Settings, scope.textSecondary) },
                            onClick = {
                                val intent = context.packageManager.getLaunchIntentForPackage(Backend.SHIZUKU_PKG)
                                if (intent == null) notify("未安装 Shizuku，可从应用商店或官网获取")
                                else runCatching { context.startActivity(intent) }
                            },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "申请 Root",
                            summary = "由 KernelSU / Magisk 弹出授权",
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.amber) },
                            onClick = {
                                coroutine.launch {
                                    val granted = withContext(Dispatchers.IO) {
                                        runCatching {
                                            // 主动获取 shell 以触发 KernelSU / Magisk 授权弹窗
                                            com.topjohnwu.superuser.Shell.getShell()
                                            com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true
                                        }.getOrDefault(false)
                                    }
                                    if (granted) notify("已获得 Root 授权")
                                    else notify("未获得 Root 授权，请确认设备已 Root")
                                    onRefresh()
                                }
                            },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "重新检测",
                            summary = "重新读取安装、服务、授权与身份",
                            startAction = { PrefIcon(MiuixIcons.Refresh, scope.green) },
                            onClick = onRefresh,
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column(Modifier.padding(18.dp)) {
                        Text("说明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Shizuku 提供 ADB 级 shell 权限，Root 提供 root 权限，两者不等同。SCOPE 只做平台允许的操作，系统拒绝时如实显示失败原因，不会伪造成功。",
                            fontSize = 13.sp,
                            color = scope.textSecondary,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateRow(label: String, ok: Boolean, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(if (ok) LocalScope.current.green else LocalScope.current.textSecondary.copy(alpha = 0.4f), 8.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, color = LocalScope.current.textSecondary)
    }
}

@Composable
fun CapabilitiesScreen(status: BackendStatus, device: DeviceSnapshot, onBack: () -> Unit) {
    val scope = LocalScope.current
    val context = LocalContext.current
    val capabilities = remember(status, device) { Capabilities.evaluate(context, status, device) }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "系统能力", subtitle = "Android ${device.androidRelease} · API ${device.sdk}", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            capabilities.groupBy { it.group }.forEach { (group, items) ->
                item(key = "h-$group") {
                    Text(
                        text = group,
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                        fontSize = 13.sp,
                        color = scope.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                item(key = "g-$group") {
                    PrefCard {
                        Column {
                            items.forEachIndexed { index, capability ->
                                if (index > 0) PrefDivider()
                                CapabilityRow(capability)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(capability: Capability) {
    val scope = LocalScope.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 5.dp)) {
            StatusDot(if (capability.available) scope.green else scope.textSecondary.copy(alpha = 0.35f), 8.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(capability.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(capability.desc, fontSize = 12.sp, color = scope.textSecondary, lineHeight = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${capability.requirement} · ${capability.reason}",
                fontSize = 11.sp,
                color = if (capability.available) scope.green else scope.amber,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    val records by History.records.collectAsState()
    var type by remember { mutableIntStateOf(0) }
    var onlyFailed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val types = listOf("全部", "冻结", "AppOps", "组件", "权限", "其他")
    val filtered = remember(records, type, onlyFailed, query) {
        val q = query.trim().lowercase()
        records.filter { record ->
            val typeOk = when (type) {
                1 -> record.action == "FREEZE" || record.action == "UNFREEZE"
                2 -> record.action == "APPOP"
                3 -> record.action == "COMPONENT"
                4 -> record.action == "PERMISSION"
                5 -> record.action == "SPECIAL"
                else -> true
            }
            val failOk = !onlyFailed || !record.success
            val queryOk = q.isEmpty() || record.title.lowercase().contains(q) ||
                record.target.lowercase().contains(q) || record.detail.lowercase().contains(q)
            typeOk && failOk && queryOk
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "操作记录",
            subtitle = "${filtered.size}/${records.size} 条 · 可恢复的操作提供回滚",
            onBack = onBack,
            actions = {
                top.yukonga.miuix.kmp.basic.TextButton(
                    text = "清空",
                    onClick = { coroutine.launch { History.clear() } },
                )
            },
        )
        TabRow(
            tabs = types,
            selectedTabIndex = type,
            onTabSelected = { type = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            top.yukonga.miuix.kmp.basic.TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = "筛选应用或操作",
                useLabelAsPlaceholder = true,
                singleLine = true,
                cornerRadius = 14.dp,
                colors = top.yukonga.miuix.kmp.basic.TextFieldDefaults.textFieldColors(
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                    labelColor = scope.textSecondary,
                ),
            )
            Spacer(Modifier.width(8.dp))
            top.yukonga.miuix.kmp.basic.TextButton(
                text = if (onlyFailed) "仅失败" else "全部结果",
                onClick = { onlyFailed = !onlyFailed },
                minHeight = 36.dp,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (filtered.isEmpty()) item { EmptyHint("没有符合条件的操作记录") }
            items(filtered, key = { it.id }) { record ->
                HistoryCard(record) {
                    coroutine.launch {
                        val r = History.restore(context, record)
                        notify(r.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(record: OpRecord, onRestore: () -> Unit) {
    val scope = LocalScope.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = scope.card),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${record.timeText()} · ${record.backend}",
                        fontSize = 11.sp,
                        color = scope.textSecondary,
                    )
                }
                Box(
                    Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(if (record.success) scope.greenSoft else Color(0x22E0655B))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (record.success) "成功" else "失败",
                        fontSize = 11.sp,
                        color = if (record.success) scope.green else Color(0xFFE0655B),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${record.target}${if (record.detail.isNotBlank()) " · ${record.detail}" else ""}",
                fontSize = 11.sp,
                color = scope.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (record.error.isNotBlank()) {
                Text(record.error, fontSize = 11.sp, color = Color(0xFFE0655B))
            }
            if (record.before.isNotBlank() || record.after.isNotBlank()) {
                Text(
                    text = "${record.before.ifBlank { "—" }} → ${record.after.ifBlank { "—" }}",
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                )
            }
            if (record.restorable) {
                Spacer(Modifier.height(6.dp))
                top.yukonga.miuix.kmp.basic.TextButton(
                    text = "恢复到修改前",
                    onClick = onRestore,
                    minHeight = 34.dp,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun PermissionCenterScreen(onBack: () -> Unit, onOpen: (Route) -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val counts by produceState<Map<String, Int>>(initialValue = emptyMap()) {
        value = withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, Int>()
            AppRepo.cached().forEach { app ->
                runCatching {
                    AppRepo.specialAccess(context, app.pkg, app.uid).filter { it.granted }.forEach {
                        map[it.id] = (map[it.id] ?: 0) + 1
                    }
                }
            }
            map
        }
    }
    val entries = listOf(
        Triple("overlay", "悬浮窗", "可覆盖其它应用界面"),
        Triple("usage", "使用情况访问", "可读取应用使用记录"),
        Triple("install", "安装未知应用", "允许安装 APK"),
        Triple("all_files", "所有文件访问", "访问共享存储全部文件"),
        Triple("write_settings", "修改系统设置", "修改亮度、音量等系统开关"),
        Triple("notification", "通知使用权", "读取通知内容"),
        Triple("accessibility", "无障碍服务", "读取界面并执行操作"),
        Triple("battery", "电池优化白名单", "不受后台限制"),
    )
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "权限中心", subtitle = "特殊访问的真实授权情况", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column {
                        MiuixArrowPreference(
                            title = "按应用查看特殊访问",
                            summary = "选择应用后逐项查看与修改",
                            startAction = { PrefIcon(MiuixIcons.Layers, scope.blue) },
                            onClick = {
                                onOpen(Route.Picker("特殊访问", "选择应用", com.scope.app.ui.PickerTarget.SPECIAL))
                            },
                        )
                        PrefDivider()
                        MiuixArrowPreference(
                            title = "按应用查看运行时权限",
                            summary = "授予或撤销危险权限",
                            startAction = { PrefIcon(MiuixIcons.Lock, scope.green) },
                            onClick = {
                                onOpen(Route.Picker("运行时权限", "选择应用", com.scope.app.ui.PickerTarget.PERMISSIONS))
                            },
                        )
                    }
                }
            }
            item {
                PrefCard {
                    Column {
                        entries.forEachIndexed { index, (id, name, desc) ->
                            if (index > 0) PrefDivider()
                            MiuixArrowPreference(
                                title = name,
                                summary = "${counts[id] ?: 0} 个应用已授权 · $desc",
                                startAction = { PrefIcon(MiuixIcons.Info, scope.amber) },
                                onClick = { openSystemAccess(context, id, "com.scope.app", notify) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(status: BackendStatus, device: DeviceSnapshot, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val checks = listOf(
        "Android 版本" to "Android ${device.androidRelease} · API ${device.sdk}",
        "厂商系统" to "${device.osName} ${device.osVersion}".trim(),
        "ABI" to device.abi,
        "Kernel" to device.kernel,
        "Root 状态" to if (status.rootGranted) "已授权" else "未授权",
        "Shizuku 服务" to if (status.shizukuRunning) "已连接" else "未运行",
        "Shizuku 授权" to if (status.shizukuGranted) "已授权" else "未授权",
        "应用查询权限" to if (AppRepo.cached().isNotEmpty()) "正常（${AppRepo.cached().size} 个应用）" else "未读取",
        "数据目录" to context.filesDir.parent.orEmpty(),
        "操作记录" to "${History.records.value.size} 条",
        "场景" to "${com.scope.app.core.ScenarioStore.list.value.size} 个",
    )
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "运行诊断", subtitle = "当前设备与权限环境", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        checks.forEach { (label, value) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(label, fontSize = 14.sp, modifier = Modifier.width(110.dp))
                                Text(
                                    text = value.ifBlank { "—" },
                                    fontSize = 13.sp,
                                    color = scope.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
