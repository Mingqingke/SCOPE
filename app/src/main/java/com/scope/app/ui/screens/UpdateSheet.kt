package com.scope.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.BuildConfig
import com.scope.app.core.UpdateInfo
import com.scope.app.core.Updater
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.io.File
import java.util.Locale

/** Miuix 风格的更新面板：下载进度、校验、安装都在这里完成。 */
@Composable
fun UpdateSheet(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var file by remember { mutableStateOf<File?>(null) }

    OverlayBottomSheet(
        show = true,
        title = "发现新版本 ${info.versionName}",
        onDismissRequest = { if (!downloading) onDismiss() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = "当前 ${BuildConfig.VERSION_NAME} → ${info.versionName}",
                fontSize = 13.sp,
                color = scope.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = info.notes.ifBlank { "修复了一些已知问题" },
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            if (info.size > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "安装包 ${String.format(Locale.ROOT, "%.1f MB", info.size / 1024.0 / 1024.0)}",
                    fontSize = 12.sp,
                    color = scope.textSecondary,
                )
            }
            if (downloading || progress > 0f) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (downloading) "正在下载 ${(progress * 100).toInt()}%" else "下载完成",
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = "稍后",
                    onClick = onDismiss,
                    enabled = !downloading,
                    modifier = Modifier.weight(1f),
                )
                val target = file
                if (target != null) {
                    Button(
                        onClick = {
                            if (!Updater.install(context, target)) {
                                notify("无法唤起系统安装器，请检查安装未知应用权限")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("安装更新", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Button(
                        onClick = {
                            if (downloading) return@Button
                            downloading = true
                            progress = 0f
                            coroutine.launch {
                                runCatching {
                                    Updater.download(context, info) { progress = it }
                                }.onSuccess {
                                    file = it
                                    notify("下载完成，可以安装")
                                }.onFailure {
                                    notify("更新下载失败：${it.message ?: "请检查网络"}")
                                }
                                downloading = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !downloading,
                    ) {
                        Text(if (downloading) "下载中" else "立即更新", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "从多个更新源下载，安装包会校验 SHA-256",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp,
                color = scope.textSecondary,
            )
        }
    }
}

/** 关于页里的手动检查入口。 */
@Composable
fun UpdateCheckRow(notify: (String) -> Unit) {
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<UpdateInfo?>(null) }

    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = "检查更新",
        summary = if (checking) "正在检查多个更新源" else "当前版本 ${BuildConfig.VERSION_NAME}",
        startAction = {
            PrefIcon(icon = MiuixIcons.Refresh, tint = scope.blue)
        },
        onClick = {
            if (!checking) {
            checking = true
            coroutine.launch {
                when (val result = com.scope.app.core.Updater.check()) {
                    is com.scope.app.core.UpdateCheck.Available -> available = result.info
                    com.scope.app.core.UpdateCheck.Latest -> notify("已是最新版本")
                    is com.scope.app.core.UpdateCheck.Failed -> notify(result.reason)
                }
                checking = false
            }
            }
        },
    )

    available?.let { info ->
        UpdateSheet(info = info, onDismiss = { available = null }, notify = notify)
    }
}
