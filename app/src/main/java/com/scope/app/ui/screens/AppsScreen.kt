package com.scope.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.core.AppEntry
import com.scope.app.core.AppRepo
import com.scope.app.ui.components.AppIcon
import com.scope.app.ui.components.EmptyHint
import com.scope.app.ui.components.ScreenTitle
import com.scope.app.ui.components.pressFeedback
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppsScreen(onOpenApp: (String, String) -> Unit) {
    val context = LocalContext.current
    val apps by AppRepo.apps.collectAsState()
    var loading by remember { mutableStateOf(apps.isEmpty()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = apps.isEmpty()
        withContext(Dispatchers.IO) { AppRepo.load(context, force = reloadKey > 0) }
        loading = false
    }

    val shown = remember(apps, query, filter) {
        val q = query.trim().lowercase()
        apps.filter { app ->
            val byQuery = q.isEmpty() || app.label.lowercase().contains(q) || app.pkg.lowercase().contains(q)
            val byFilter = when (filter) {
                1 -> !app.system
                2 -> app.system
                3 -> !app.enabled
                else -> true
            }
            byQuery && byFilter
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "应用",
            subtitle = if (apps.isEmpty()) "正在读取已安装应用" else "${apps.size} 个应用 · ${apps.count { !it.enabled }} 个已冻结",
            actions = {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { reloadKey++ },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MiuixIcons.Refresh, "刷新", Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onBackgroundVariant)
                }
            },
        )

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = "搜索应用或包名",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                labelColor = LocalScope.current.textSecondary,
            ),
        )

        Spacer(Modifier.height(10.dp))

        TabRow(
            tabs = listOf("全部", "用户", "系统", "已冻结"),
            selectedTabIndex = filter,
            onTabSelected = { filter = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (loading) {
                item { EmptyHint("正在读取应用列表…") }
            } else if (shown.isEmpty()) {
                item { EmptyHint(if (query.isBlank()) "没有符合条件的应用" else "没有匹配「$query」的应用") }
            } else {
                items(shown, key = { it.pkg }) { app ->
                    AppRow(app) { onOpenApp(app.pkg, app.label) }
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppEntry, onClick: () -> Unit) {
    val scope = LocalScope.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.pkg, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.pkg,
                fontSize = 11.sp,
                color = scope.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (!app.enabled) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(scope.greenSoft)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(text = "已冻结", fontSize = 10.sp, color = scope.green, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = app.versionName.ifBlank { "—" },
            fontSize = 11.sp,
            color = scope.textSecondary,
            maxLines = 1,
        )
    }
}

/** 选择应用（供 AppOps / 组件管理 / 场景构建使用）。 */
@Composable
fun AppPickerScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onPick: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val apps by AppRepo.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { if (apps.isEmpty()) withContext(Dispatchers.IO) { AppRepo.load(context) } }
    val shown = remember(apps, query) {
        val q = query.trim().lowercase()
        apps.filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(title = title, subtitle = subtitle, onBack = onBack)
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = "搜索应用",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                labelColor = LocalScope.current.textSecondary,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (apps.isEmpty()) item { EmptyHint("正在读取应用列表…") }
            items(shown, key = { it.pkg }) { app ->
                AppRow(app) { onPick(app.pkg, app.label) }
            }
        }
    }
}

/** 供场景构建复选应用。 */
@Composable
fun AppMultiPickerScreen(
    title: String,
    onBack: () -> Unit,
    onConfirm: (List<AppEntry>) -> Unit,
) {
    val context = LocalContext.current
    val apps by AppRepo.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) { if (apps.isEmpty()) withContext(Dispatchers.IO) { AppRepo.load(context) } }
    val shown = remember(apps, query) {
        val q = query.trim().lowercase()
        apps.filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = title,
            subtitle = "已选择 ${selected.size} 个应用",
            onBack = onBack,
            actions = {
                Card(
                    cornerRadius = 16.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primary),
                    pressFeedbackType = pressFeedback(),
                    onClick = { onConfirm(apps.filter { selected.contains(it.pkg) }) },
                ) {
                    Text(
                        text = "完成",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 13.sp,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = "搜索应用",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
                labelColor = LocalScope.current.textSecondary,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (apps.isEmpty()) item { EmptyHint("正在读取应用列表…") }
            items(shown, key = { it.pkg }) { app ->
                val checked = selected.contains(app.pkg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (checked) selected.remove(app.pkg) else selected.add(app.pkg)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.pkg, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.pkg, fontSize = 11.sp, color = LocalScope.current.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (checked) MiuixTheme.colorScheme.primary
                                else LocalScope.current.hairline,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checked) {
                            Icon(
                                MiuixIcons.Ok,
                                null,
                                Modifier.size(14.dp),
                                tint = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
