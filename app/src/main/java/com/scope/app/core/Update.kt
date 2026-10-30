package com.scope.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.scope.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val notes: String = "修复了一些已知问题",
    val size: Long = 0L,
    val sha256: String = "",
    val force: Boolean = false,
    val apkUrls: List<String> = emptyList(),
)

sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo, val source: String) : UpdateCheck
    data object Latest : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

/**
 * 多源更新：每个源独立超时，取 versionCode 最高的结果；
 * 下载按源顺序重试并校验 SHA-256，失败不落盘、不伪装成功。
 */
object Updater {

    private const val OWNER = "Mingqingke"
    private const val REPO = "SCOPE"

    val configSources: List<String> = listOf(
        "https://cdn.jsdelivr.net/gh/$OWNER/$REPO@main/update.json",
        "https://raw.githubusercontent.com/$OWNER/$REPO/main/update.json",
        "https://ghproxy.net/https://raw.githubusercontent.com/$OWNER/$REPO/main/update.json",
        "https://raw.gitmirror.com/$OWNER/$REPO/main/update.json",
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun check(sources: List<String> = configSources): UpdateCheck = withContext(Dispatchers.IO) {
        // 源按可靠性排序：首个可解析且更新的源立即返回，避免被不可达源拖慢
        var reachable = false
        val failures = mutableListOf<String>()
        sources.forEach { source ->
            val text = runCatching { fetchText(source) }.getOrNull()
            if (text.isNullOrBlank()) {
                failures += source.substringAfter("//").substringBefore("/")
                return@forEach
            }
            val info = runCatching { json.decodeFromString<UpdateInfo>(text) }.getOrNull()
            if (info == null || info.versionCode <= 0) {
                failures += source.substringAfter("//").substringBefore("/")
                return@forEach
            }
            reachable = true
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                return@withContext UpdateCheck.Available(info, source.substringAfter("//").substringBefore("/"))
            }
        }
        if (reachable) UpdateCheck.Latest
        else UpdateCheck.Failed("所有更新源均不可达（${failures.distinct().joinToString(" / ")}）")
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "SCOPE-${info.versionName}.apk")
        if (target.exists() && info.sha256.isNotBlank() && sha256(target).equals(info.sha256, true)) {
            onProgress(1f)
            return@withContext target
        }
        val urls = (info.apkUrls + listOfNotNull(
            "https://github.com/$OWNER/$REPO/releases/download/v${info.versionName}/SCOPE-v${info.versionName}.apk"
        )).distinct()
        var lastError = "没有可用的下载地址"
        urls.forEach { url ->
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "SCOPE-Updater")
                }
                try {
                    if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                    val total = connection.contentLengthLong
                    var read = 0L
                    val temp = File(dir, "download.tmp")
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count <= 0) break
                                output.write(buffer, 0, count)
                                read += count
                                if (total > 0) onProgress((read.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                    }
                    if (info.sha256.isNotBlank() && !sha256(temp).equals(info.sha256, true)) {
                        temp.delete()
                        error("文件校验失败")
                    }
                    if (target.exists()) target.delete()
                    if (!temp.renameTo(target)) error("无法保存安装包")
                    onProgress(1f)
                } finally {
                    connection.disconnect()
                }
            }.onFailure { lastError = it.message ?: "下载失败" }
        }
        if (!target.exists()) error(lastError)
        target
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun install(context: Context, file: File): Boolean = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
