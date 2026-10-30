package com.scope.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.core.Backend
import com.scope.app.core.BackendStatus
import com.scope.app.core.DeviceSnapshot
import com.scope.app.ui.components.AuthVisual
import com.scope.app.ui.components.CheckBadge
import com.scope.app.ui.components.EntryCard
import com.scope.app.ui.components.StatusDot
import com.scope.app.ui.components.pressFeedback
import com.scope.app.ui.theme.LocalScope
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class AuthPresentation(
    val title: String,
    val detail: String,
    val version: String,
    val visual: AuthVisual,
)

private fun present(status: BackendStatus, shizukuVersion: String?): AuthPresentation = when {
    status.rootGranted -> AuthPresentation(
        title = "Root 已授权",
        detail = "SCOPE 以 root 身份运行",
        version = "KernelSU / Magisk · su",
        visual = AuthVisual.OK,
    )
    status.shizukuReady -> AuthPresentation(
        title = "Shizuku 已连接",
        detail = "Shizuku 已授权",
        version = shizukuVersion?.let { "Shizuku $it" } ?: "Shizuku 服务可用",
        visual = AuthVisual.OK,
    )
    status.shizukuRunning -> AuthPresentation(
        title = "Shizuku 未授权",
        detail = "服务运行中 · 点击完成授权",
        version = "点击进入授权页",
        visual = AuthVisual.WARN,
    )
    status.shizukuInstalled -> AuthPresentation(
        title = "Shizuku 未连接",
        detail = "服务未运行 · 点击检查",
        version = "已安装，等待启动服务",
        visual = AuthVisual.WARN,
    )
    else -> AuthPresentation(
        title = "普通模式",
        detail = "未检测到 Shizuku 与 Root",
        version = "点击配置高级授权",
        visual = AuthVisual.OFF,
    )
}

@Composable
fun HomeScreen(
    status: BackendStatus,
    device: DeviceSnapshot,
    onOpenAuth: () -> Unit,
    onOpenCapabilities: () -> Unit,
    onOpenScenarios: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scope = LocalScope.current
    val shizukuVersion = rememberShizukuVersion(status)
    val presentation = present(status, shizukuVersion)
    val accent = when {
        status.rootGranted || status.shizukuReady -> scope.green
        status.shizukuInstalled -> scope.amber
        else -> scope.textSecondary
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "首页",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickableNoIndication(onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.Refresh, "重新检测", Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(238.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 左列：授权大卡片
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                cornerRadius = 26.dp,
                colors = CardDefaults.defaultColors(color = scope.mint),
                pressFeedbackType = pressFeedback(),
                showIndication = true,
                onClick = onOpenAuth,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 18.dp, top = 18.dp, end = 16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = presentation.title,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.padding(top = 6.dp)) {
                                StatusDot(accent, 9.dp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = presentation.detail,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = presentation.version,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            lineHeight = 17.sp,
                        )
                    }
                    CheckBadge(
                        kind = presentation.visual,
                        modifier = Modifier
                            .size(134.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 30.dp, y = 34.dp),
                    )
                }
            }

            // 右列：上下两张状态小卡片
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniStatusCard(
                    title = "Android 版本",
                    value = if (device.androidRelease.isBlank()) "未知" else "Android ${device.androidRelease}",
                    sub = if (device.sdk > 0) "API ${device.sdk}" else null,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAuth,
                )
                MiniStatusCard(
                    title = "系统版本",
                    value = if (device.osName.isBlank()) "未知" else device.osName,
                    sub = device.osVersion.ifBlank { device.buildIncremental.ifBlank { null } },
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAuth,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        EntryCard(
            title = "系统能力",
            subtitle = "权限 · AppOps · 组件管理",
            onClick = onOpenCapabilities,
            interactive = false,
        )
        Spacer(Modifier.height(10.dp))
        EntryCard(
            title = "场景管理",
            subtitle = "创建与执行自定义系统方案",
            onClick = onOpenScenarios,
            interactive = false,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MiniStatusCard(
    title: String,
    value: String,
    sub: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scope = LocalScope.current
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = scope.card),
        pressFeedbackType = pressFeedback(),
        showIndication = true,
        onClick = onClick,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = scope.textSecondary, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!sub.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun rememberShizukuVersion(status: BackendStatus): String? {
    val version by androidx.compose.runtime.produceState<String?>(
        initialValue = null,
        status.shizukuRunning,
        status.shizukuGranted,
    ) {
        value = if (status.shizukuReady) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Backend.shizukuVersion() }
        } else null
    }
    return version
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = null, indication = null, onClick = onClick)
