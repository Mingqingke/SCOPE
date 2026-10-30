package com.scope.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.core.AppDetail
import com.scope.app.core.AppRepo
import com.scope.app.core.Backend
import com.scope.app.core.CompItem
import com.scope.app.core.Device
import com.scope.app.core.OpItem
import com.scope.app.core.Ops
import com.scope.app.core.PermItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.scope.app.ui.components.AppIcon
import com.scope.app.ui.components.EmptyHint
import com.scope.app.ui.components.KeyValueRow
import com.scope.app.ui.components.ScreenTitle
import com.scope.app.ui.theme.LocalScope

/** 分组卡片：内部放 Miuix Preference 组件，风格与系统设置保持一致。 */
@Composable
fun PrefCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = LocalScope.current.card),
    ) {
        content()
    }
}

@Composable
fun PrefDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 18.dp, end = 18.dp),
        color = LocalScope.current.hairline,
    )
}

@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = confirmText,
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun AppDetailScreen(
    pkg: String,
    fallbackLabel: String,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAppOps: () -> Unit,
    onOpenComponents: () -> Unit,
    onOpenSpecial: () -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val detail by produceState<AppDetail?>(initialValue = null, pkg, refresh) {
        value = withContext(Dispatchers.IO) { AppRepo.detail(context, pkg) }
    }
    var confirmFreeze by remember { mutableStateOf(false) }
    val privileged = Backend.snapshot().privileged

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = detail?.label ?: fallbackLabel, subtitle = pkg, onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(pkg, 58.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = detail?.label ?: fallbackLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "版本 ${detail?.versionName?.ifBlank { "—" } ?: "—"}",
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                        )
                        Text(
                            text = if (detail?.enabled == false) "已冻结" else "运行中",
                            fontSize = 12.sp,
                            color = if (detail?.enabled == false) scope.amber else scope.green,
                        )
                    }
                }
            }

            item {
                PrefCard {
                    Column {
                        SwitchPreference(
                            title = "冻结应用",
                            summary = when {
                                !privileged -> "需要 Shizuku 或 Root 授权"
                                detail?.enabled == false -> "已冻结 · 数据保留"
                                else -> "保留数据，仅停用应用"
                            },
                            checked = detail?.enabled == false,
                            enabled = privileged && detail != null,
                            onCheckedChange = { wantFrozen ->
                                if (!wantFrozen) {
                                    coroutine.launch {
                                        val r = Ops.setAppEnabled(context, pkg, detail?.label ?: fallbackLabel, true)
                                        notify(r.message)
                                        refresh++
                                    }
                                } else if (com.scope.app.core.Prefs.confirmRisky.value) {
                                    confirmFreeze = true
                                } else {
                                    coroutine.launch {
                                        val r = Ops.setAppEnabled(context, pkg, detail?.label ?: fallbackLabel, false)
                                        notify(r.message)
                                        refresh++
                                    }
                                }
                            },
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "系统应用信息",
                            summary = "在系统设置中查看该应用",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")),
                                    )
                                }.onFailure { notify("无法打开系统应用信息") }
                            },
                        )
                    }
                }
            }

            item {
                PrefCard {
                    Column {
                        ArrowPreference(
                            title = "权限",
                            summary = detail?.let {
                                "${it.permissions.size} 项 · 已授予 ${it.permissions.count { p -> p.granted }}"
                            } ?: "读取中",
                            onClick = onOpenPermissions,
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "AppOps",
                            summary = if (privileged) "查看并修改应用操作权限" else "需要 Shizuku 或 Root 授权",
                            onClick = onOpenAppOps,
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "组件管理",
                            summary = "Activity · Service · Receiver · Provider",
                            onClick = onOpenComponents,
                        )
                        PrefDivider()
                        ArrowPreference(
                            title = "特殊访问",
                            summary = detail?.let {
                                "${it.special.count { s -> s.granted }} 项已授权"
                            } ?: "读取中",
                            onClick = onOpenSpecial,
                        )
                    }
                }
            }

            item {
                PrefCard {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = "基本信息",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = scope.textSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            KeyValueRow("包名", pkg)
                            KeyValueRow("版本号", detail?.versionCode?.toString() ?: "—")
                            KeyValueRow("Target SDK", detail?.targetSdk?.toString() ?: "—")
                            KeyValueRow("Min SDK", detail?.minSdk?.toString() ?: "—")
                            KeyValueRow("UID", detail?.uid?.toString() ?: "—")
                            KeyValueRow("类型", if (detail?.system == true) "系统应用" else "用户应用")
                            KeyValueRow("安装来源", detail?.installer ?: "—")
                            KeyValueRow("APK 大小", detail?.let { Device.formatBytes(it.apkSize) } ?: "—")
                            KeyValueRow("签名 SHA-256", detail?.signature?.take(48)?.plus("…") ?: "—")
                            KeyValueRow("数据目录", detail?.dataDir ?: "—")
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    ConfirmDialog(
        show = confirmFreeze,
        title = "冻结 ${detail?.label ?: fallbackLabel}",
        summary = "应用会被停用，数据保留。可在本页或操作记录中恢复。",
        confirmText = "冻结",
        onDismiss = { confirmFreeze = false },
        onConfirm = {
            coroutine.launch {
                val r = Ops.setAppEnabled(context, pkg, detail?.label ?: fallbackLabel, false)
                notify(r.message)
                refresh++
            }
        },
    )
}

@Composable
fun PermissionsScreen(pkg: String, label: String, onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val detail by produceState<AppDetail?>(initialValue = null, pkg, refresh) {
        value = withContext(Dispatchers.IO) { AppRepo.detail(context, pkg) }
    }
    val privileged = Backend.snapshot().privileged
    var pending by remember { mutableStateOf<Triple<PermItem, Boolean, Boolean>?>(null) }
    val perms = detail?.permissions.orEmpty()
    val dangerous = perms.filter { it.dangerous }
    val others = perms.filterNot { it.dangerous }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "权限",
            subtitle = "$label · 已授予 ${perms.count { it.granted }}/${perms.size}",
            onBack = onBack,
        )
        if (!privileged) {
            HintBar("当前为普通模式，只能查看权限状态，授予或撤销需要 Shizuku 或 Root")
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (perms.isEmpty()) {
                item { EmptyHint("正在读取权限或该应用未申请权限") }
            }
            if (dangerous.isNotEmpty()) {
                item {
                    PermissionGroupCard("运行时权限", dangerous, privileged, pkg, label, coroutine, notify, onRequest = { perm, want ->
                        if (com.scope.app.core.Prefs.confirmPermission.value) pending = Triple(perm, want, true)
                        else coroutine.launch {
                            val r = Ops.setPermission(context, pkg, label, perm.name, want)
                            notify(r.message)
                            refresh++
                        }
                    }) { refresh++ }
                }
            }
            if (others.isNotEmpty()) {
                item {
                    PermissionGroupCard("其它权限", others, privileged, pkg, label, coroutine, notify, onRequest = { perm, want ->
                        if (com.scope.app.core.Prefs.confirmPermission.value) pending = Triple(perm, want, false)
                        else coroutine.launch {
                            val r = Ops.setPermission(context, pkg, label, perm.name, want)
                            notify(r.message)
                            refresh++
                        }
                    }) { refresh++ }
                }
            }
        }
    }

    val target = pending
    ConfirmDialog(
        show = target != null,
        title = if (target?.second == true) "授予权限" else "撤销权限",
        summary = target?.first?.label.orEmpty() + "\n" + (target?.first?.name.orEmpty()),
        confirmText = if (target?.second == true) "授予" else "撤销",
        onDismiss = { pending = null },
        onConfirm = {
            val (perm, want, _) = target ?: return@ConfirmDialog
            coroutine.launch {
                val r = Ops.setPermission(context, pkg, label, perm.name, want)
                notify(r.message)
                refresh++
            }
        },
    )
}

@Composable
private fun PermissionGroupCard(
    title: String,
    items: List<PermItem>,
    privileged: Boolean,
    pkg: String,
    label: String,
    coroutine: kotlinx.coroutines.CoroutineScope,
    notify: (String) -> Unit,
    onRequest: (PermItem, Boolean) -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    PrefCard {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                fontSize = 13.sp,
                color = LocalScope.current.textSecondary,
                fontWeight = FontWeight.Medium,
            )
            items.forEachIndexed { index, perm ->
                if (index > 0) PrefDivider()
                SwitchPreference(
                    title = perm.label,
                    summary = perm.name,
                    checked = perm.granted,
                    enabled = privileged,
                    onCheckedChange = { want -> onRequest(perm, want) },
                )
            }
        }
    }
}

@Composable
fun AppOpsScreen(pkg: String, label: String, onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val privileged = Backend.snapshot().privileged
    var refresh by remember { mutableIntStateOf(0) }
    val ops by produceState<List<OpItem>>(initialValue = emptyList(), pkg, refresh) {
        value = withContext(Dispatchers.IO) { AppRepo.readAppOps(pkg) }
    }
    var editing by remember { mutableStateOf<OpItem?>(null) }
    var confirmTarget by remember { mutableStateOf<Pair<OpItem, String>?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "AppOps", subtitle = "$label · ${ops.size} 项", onBack = onBack)
        if (!privileged) {
            HintBar("AppOps 读取与修改需要 Shizuku 或 Root 授权")
        } else if (ops.isEmpty()) {
            HintBar("系统没有返回该应用的 AppOps 记录")
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ops, key = { it.op }) { op ->
                PrefCard {
                    ArrowPreference(
                        title = op.name,
                        summary = "当前 ${op.mode}",
                        enabled = privileged,
                        onClick = { editing = op },
                    )
                }
            }
        }
    }

    val current = editing
    OverlayDialog(
        show = current != null,
        title = current?.name ?: "",
        summary = "选择 AppOps 模式，执行后会重新读取确认",
        onDismissRequest = { editing = null },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("allow", "ignore", "default").forEach { mode ->
                TextButton(
                    text = mode,
                    onClick = {
                        val op = current ?: return@TextButton
                        editing = null
                        if (com.scope.app.core.Prefs.confirmPermission.value) {
                            confirmTarget = op to mode
                        } else {
                            coroutine.launch {
                                val r = Ops.setAppOp(context, pkg, label, op.op, mode)
                                notify(r.message)
                                refresh++
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val pendingOp = confirmTarget
    ConfirmDialog(
        show = pendingOp != null,
        title = "修改 AppOps",
        summary = "${pendingOp?.first?.name.orEmpty()} → ${pendingOp?.second.orEmpty()}",
        confirmText = "修改",
        onDismiss = { confirmTarget = null },
        onConfirm = {
            val (op, mode) = pendingOp ?: return@ConfirmDialog
            coroutine.launch {
                val r = Ops.setAppOp(context, pkg, label, op.op, mode)
                notify(r.message)
                refresh++
            }
        },
    )
}

@Composable
fun ComponentsScreen(pkg: String, label: String, onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    val privileged = Backend.snapshot().privileged
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    val comps by produceState<List<CompItem>>(initialValue = emptyList(), pkg, refresh) {
        value = withContext(Dispatchers.IO) { AppRepo.components(context, pkg) }
    }
    val types = listOf("Activity", "Service", "Receiver", "Provider")
    val filtered = comps.filter { it.type == types[tab] }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "组件", subtitle = "$label · ${comps.size} 个组件", onBack = onBack)
        TabRow(
            tabs = types,
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(6.dp))
        if (!privileged) HintBar("组件启停需要 Shizuku 或 Root 授权")
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) item { EmptyHint("该应用没有 ${types[tab]} 组件") }
            items(filtered, key = { it.name }) { comp ->
                PrefCard {
                    SwitchPreference(
                        title = comp.simpleName,
                        summary = "${comp.stateLabel}${if (comp.exported) " · 已导出" else ""}${comp.permission?.let { " · $it" } ?: ""}",
                        checked = comp.enabled,
                        enabled = privileged,
                        onCheckedChange = { want ->
                            coroutine.launch {
                                val r = Ops.setComponent(context, pkg, label, comp.name, want, comp.enabled)
                                notify(r.message)
                                refresh++
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SpecialAccessScreen(
    pkg: String,
    label: String,
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var needAuth by remember { mutableStateOf(false) }
    val special by produceState(initialValue = emptyList<com.scope.app.core.SpecialAccess>(), pkg, refresh) {
        value = withContext(Dispatchers.IO) { AppRepo.detail(context, pkg)?.special.orEmpty() }
    }
    val privileged = Backend.snapshot().privileged

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = "特殊访问", subtitle = label, onBack = onBack)
        HintBar(
            if (privileged) "全部项目都可在应用内直接开关，修改后会重新读取确认"
            else "当前为普通模式，授权 Shizuku 或 Root 后可在应用内直接开关",
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(special, key = { it.id }) { item ->
                PrefCard {
                    SwitchPreference(
                        title = item.name,
                        summary = "当前 ${item.mode}${if (privileged) "" else " · 未授权后端"}",
                        checked = item.granted,
                        onCheckedChange = { want ->
                            if (!privileged) {
                                needAuth = true
                            } else {
                                coroutine.launch {
                                    val r = Ops.setSpecial(context, pkg, label, item.id, item.name, want)
                                    notify(r.message)
                                    refresh++
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        show = needAuth,
        title = "需要高级授权",
        summary = "在应用内直接修改特殊访问需要 Shizuku 或 Root，完成授权后无需跳转系统设置",
        confirmText = "去授权",
        onDismiss = { needAuth = false },
        onConfirm = {
            needAuth = false
            onOpenAuth()
        },
    )
}

fun openSystemAccess(context: Context, id: String, pkg: String, notify: (String) -> Unit) {
    val intent = when (id) {
        "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))
        "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "install" -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$pkg"))
        "write_settings" -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$pkg"))
        "all_files" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$pkg"))
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        "notification" -> Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        notify("系统没有提供对应设置页面")
    }
}

@Composable
fun HintBar(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LocalScope.current.amber),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = LocalScope.current.textSecondary, lineHeight = 17.sp)
    }
}
