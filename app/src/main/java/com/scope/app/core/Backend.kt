package com.scope.app.core

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

/** 三种运行身份。普通模式不会伪装成高级授权。 */
enum class BackendType(val label: String) {
    NORMAL("普通模式"),
    SHIZUKU("Shizuku"),
    ROOT("Root"),
}

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok: Boolean get() = code == 0
    val text: String get() = out.trim().ifEmpty { err.trim() }
}

/**
 * 授权环境快照。区分「已安装 / 服务运行 / Binder 连接 / 已授权 / 可执行」，
 * 不把「装了 Shizuku」当成「已连接」。
 */
data class BackendStatus(
    val checked: Boolean = false,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootGranted: Boolean = false,
    val active: BackendType = BackendType.NORMAL,
    val identity: String = "普通应用",
    val failure: String? = null,
    val lastCheckedAt: Long = 0L,
) {
    val privileged: Boolean get() = active != BackendType.NORMAL
    val shizukuReady: Boolean get() = shizukuRunning && shizukuGranted
}

object Backend {

    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

    private val _status = MutableStateFlow(BackendStatus())
    val status: StateFlow<BackendStatus> = _status

    fun snapshot(): BackendStatus = _status.value

    fun canRun(): Boolean = _status.value.privileged

    /** 真实检测：包是否存在、服务是否在跑、Binder 是否连上、权限是否给出、Root 是否真授权。 */
    suspend fun refresh(context: Context): BackendStatus = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = runCatching {
            pm.getPackageInfo(SHIZUKU_PKG, 0)
            true
        }.getOrDefault(false)

        var running = false
        var granted = false
        var failure: String? = null
        if (installed) {
            running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
            if (running) {
                granted = runCatching {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }.getOrDefault(false)
                if (!granted) failure = "Shizuku 服务已运行，但 SCOPE 还没有被授权"
            } else {
                failure = "Shizuku 已安装，服务未运行"
            }
        } else {
            failure = "未安装 Shizuku"
        }

        val root = runCatching { Shell.isAppGrantedRoot() == true }.getOrDefault(false)

        val active = when {
            root -> BackendType.ROOT
            granted -> BackendType.SHIZUKU
            else -> BackendType.NORMAL
        }
        val identity = when (active) {
            BackendType.ROOT -> "root (UID 0)"
            BackendType.SHIZUKU -> "shell (UID 2000)"
            BackendType.NORMAL -> "普通应用 (UID ${android.os.Process.myUid()})"
        }
        val status = BackendStatus(
            checked = true,
            shizukuInstalled = installed,
            shizukuRunning = running,
            shizukuGranted = granted,
            rootGranted = root,
            active = active,
            identity = identity,
            failure = if (active == BackendType.NORMAL) failure else null,
            lastCheckedAt = System.currentTimeMillis(),
        )
        _status.value = status
        status
    }

    /** Shizuku 版本号（服务运行且已授权时读取，失败返回 null）。 */
    fun shizukuVersion(): String? = runCatching {
        if (!Shizuku.pingBinder()) null else "v${Shizuku.getVersion()}"
    }.getOrNull()

    fun requestShizukuPermission(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) return false
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true
        Shizuku.requestPermission(0)
        true
    }.getOrDefault(false)

    /** 统一执行入口：调用前必须已确认真实后端。 */
    suspend fun exec(command: String, timeoutSeconds: Long = 20): ShellResult =
        when (selectBackend()) {
            BackendType.ROOT -> execRoot(command, timeoutSeconds)
            BackendType.SHIZUKU -> execShizuku(command, timeoutSeconds)
            BackendType.NORMAL -> ShellResult(-1, "", "需要 Shizuku 或 Root：当前为普通模式")
        }

    /** 后端优先级：自动 = Root 优先；也可强制优先 Shizuku。不可用的后端不会被假装可用。 */
    private fun selectBackend(): BackendType {
        val status = _status.value
        val preferRoot = Prefs.backendPreference.value != 2
        val order = if (preferRoot) listOf(BackendType.ROOT, BackendType.SHIZUKU)
        else listOf(BackendType.SHIZUKU, BackendType.ROOT)
        return order.firstOrNull { type ->
            when (type) {
                BackendType.ROOT -> status.rootGranted
                BackendType.SHIZUKU -> status.shizukuReady
                BackendType.NORMAL -> true
            }
        } ?: BackendType.NORMAL
    }

    private suspend fun execRoot(command: String, timeoutSeconds: Long): ShellResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = Shell.cmd(command).exec()
                ShellResult(result.code, result.out.joinToString("\n"), result.err.joinToString("\n"))
            }.getOrElse { ShellResult(-1, "", it.message ?: "Root 执行失败") }
        }

    private suspend fun execShizuku(command: String, timeoutSeconds: Long): ShellResult =
        withContext(Dispatchers.IO) {
            if (!_status.value.shizukuReady) {
                return@withContext ShellResult(-1, "", "Shizuku 未连接或未授权")
            }
            runCatching {
                val process = newProcess(arrayOf("sh", "-c", command))
                val out = process.inputStream.bufferedReader().use { it.readText() }
                val err = process.errorStream.bufferedReader().use { it.readText() }
                val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroy()
                    ShellResult(-1, out, "执行超时（${timeoutSeconds}s）")
                } else {
                    ShellResult(process.exitValue(), out, err)
                }
            }.getOrElse { ShellResult(-1, "", it.message ?: "Shizuku 执行失败") }
        }

    /**
     * Shizuku 13 把 newProcess 收为私有，官方没有公开替代的执行入口；
     * 这里通过反射调用同一个实现，失败时如实返回错误，不静默降级成假成功。
     */
    private fun newProcess(command: Array<String>): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, command, null, null) as Process
    }

    /** 单条命令的静默版本，供状态查询使用。 */
    suspend fun query(command: String): String {
        val r = exec(command)
        return if (r.ok) r.out.trim() else ""
    }
}
