package com.scope.app.core

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val pkg: String,
    val label: String,
    val system: Boolean,
    val enabled: Boolean,
    val hasLauncher: Boolean,
    val versionName: String,
    val uid: Int,
)

data class PermItem(
    val name: String,
    val label: String,
    val granted: Boolean,
    val dangerous: Boolean,
)

data class OpItem(val op: String, val mode: String) {
    val name: String get() = op.removePrefix("android:").removePrefix("android:")
}

data class CompItem(
    val name: String,
    val type: String,
    val exported: Boolean,
    val enabled: Boolean,
    val state: Int,
    val permission: String?,
) {
    val simpleName: String get() = name.substringAfterLast('.')
    val stateLabel: String
        get() = when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> "已启用"
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> "已禁用"
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> "用户禁用"
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> "使用时启用"
            else -> if (enabled) "默认启用" else "默认禁用"
        }
}

data class SpecialAccess(
    val id: String,
    val name: String,
    val granted: Boolean,
    val mode: String,
)

data class AppDetail(
    val pkg: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val uid: Int,
    val system: Boolean,
    val enabled: Boolean,
    val firstInstall: Long,
    val lastUpdate: Long,
    val installer: String,
    val signature: String,
    val apkPath: String,
    val apkSize: Long,
    val dataDir: String,
    val permissions: List<PermItem>,
    val special: List<SpecialAccess>,
)

data class OpResult(val ok: Boolean, val message: String) {
    companion object {
        fun success(message: String) = OpResult(true, message)
        fun failure(message: String) = OpResult(false, message)
    }
}

object AppRepo {

    // 部分平台 android.jar 未导出这些常量，直接使用系统 op 字符串
    private const val OP_INSTALL_PACKAGES = "android:request_install_packages"
    private const val OP_MANAGE_EXTERNAL_STORAGE = "android:manage_external_storage"

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps
    private var loaded = false

    fun cached(): List<AppEntry> = _apps.value

    fun markStale() {
        loaded = false
    }

    suspend fun load(context: Context, force: Boolean = false): List<AppEntry> = withContext(Dispatchers.IO) {
        if (loaded && !force) return@withContext _apps.value
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val packages = runCatching { pm.getInstalledPackages(0) }.getOrDefault(emptyList())
        val collator = Collator.getInstance(Locale.CHINA)
        val list = packages.mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val pkg = info.packageName ?: return@mapNotNull null
            val label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(pkg)
            AppEntry(
                pkg = pkg,
                label = label,
                system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                enabled = app.enabled,
                hasLauncher = runCatching { pm.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false),
                versionName = info.versionName.orEmpty(),
                uid = app.uid,
            )
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        _apps.value = list
        loaded = true
        list
    }

    fun icon(context: Context, pkg: String): android.graphics.drawable.Drawable? = runCatching {
        context.packageManager.getApplicationIcon(pkg)
    }.getOrNull()

    suspend fun detail(context: Context, pkg: String): AppDetail? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info: PackageInfo = runCatching {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        }.getOrNull() ?: return@withContext null
        val app = info.applicationInfo ?: return@withContext null

        val perms = (info.requestedPermissions ?: emptyArray()).mapNotNull { name ->
            val granted = runCatching {
                pm.checkPermission(name, pkg) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            val permInfo = runCatching { pm.getPermissionInfo(name, 0) }.getOrNull()
            val protection = permInfo?.protectionLevel ?: 0
            val dangerous = (protection and 0xF) == 1
            PermItem(
                name = name,
                label = runCatching { permInfo?.loadLabel(pm)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: name.substringAfterLast('.'),
                granted = granted,
                dangerous = dangerous,
            )
        }.sortedWith(compareByDescending<PermItem> { it.granted }.thenBy { it.name })

        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName.orEmpty()
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg).orEmpty()
            }
        }.getOrDefault("")

        val apkPath = app.publicSourceDir.orEmpty()
        val signature = runCatching { signatureSha256(pm, pkg) }.getOrDefault("")

        AppDetail(
            pkg = pkg,
            label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(pkg),
            versionName = info.versionName.orEmpty(),
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong(),
            minSdk = app.minSdkVersion,
            targetSdk = app.targetSdkVersion,
            uid = app.uid,
            system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            enabled = app.enabled,
            firstInstall = info.firstInstallTime,
            lastUpdate = info.lastUpdateTime,
            installer = installer.ifBlank { "未知" },
            signature = signature,
            apkPath = apkPath,
            apkSize = runCatching { File(apkPath).length() }.getOrDefault(0L),
            dataDir = app.dataDir.orEmpty(),
            permissions = perms,
            special = specialAccess(context, pkg, app.uid),
        )
    }

    private fun signatureSha256(pm: PackageManager, pkg: String): String {
        @Suppress("DEPRECATION")
        val signatures = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
        val bytes = signatures?.firstOrNull()?.toByteArray() ?: return ""
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(":") { "%02x".format(it) }
    }

    // ---------- 权限操作 ----------

    suspend fun grant(context: Context, pkg: String, permission: String): OpResult {
        if (!Backend.canRun()) return OpResult.failure("需要 Shizuku 或 Root 授权")
        val r = Backend.exec("pm grant $pkg $permission")
        return verifyPermission(context, pkg, permission, true, r)
    }

    suspend fun revoke(context: Context, pkg: String, permission: String): OpResult {
        if (!Backend.canRun()) return OpResult.failure("需要 Shizuku 或 Root 授权")
        val r = Backend.exec("pm revoke $pkg $permission")
        return verifyPermission(context, pkg, permission, false, r)
    }

    private suspend fun verifyPermission(
        context: Context,
        pkg: String,
        permission: String,
        expectGranted: Boolean,
        r: ShellResult,
    ): OpResult {
        val now = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        }
        return if (now == expectGranted) {
            OpResult.success(if (expectGranted) "已授予 $permission" else "已撤销 $permission")
        } else {
            val why = r.text.ifBlank { "系统未接受该修改" }
            OpResult.failure("修改未生效：$why")
        }
    }

    // ---------- 应用启停 ----------

    suspend fun setAppEnabled(context: Context, pkg: String, enabled: Boolean, uid: Int = -1): OpResult {
        if (!Backend.canRun()) return OpResult.failure("需要 Shizuku 或 Root 授权")
        val cmd = if (enabled) "pm enable $pkg" else "pm disable-user --user 0 $pkg"
        val r = Backend.exec(cmd)
        val actual = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled
            }.getOrDefault(!enabled)
        }
        return if (actual == enabled) {
            OpResult.success(if (enabled) "已启用 $pkg" else "已冻结 $pkg")
        } else {
            OpResult.failure(r.text.ifBlank { "系统拒绝修改应用状态" })
        }
    }

    // ---------- AppOps ----------

    suspend fun readAppOps(pkg: String): List<OpItem> = withContext(Dispatchers.IO) {
        if (!Backend.canRun()) return@withContext emptyList()
        val r = Backend.exec("appops get $pkg")
        if (!r.ok) return@withContext emptyList()
        parseAppOpsOutput(r.out)
    }

    /** 解析 `appops get <pkg>` 的输出，兼容附加的 time= 后缀。 */
    fun parseAppOpsOutput(output: String): List<OpItem> = output.lineSequence().mapNotNull { line ->
        val trimmed = line.trim()
        val match = Regex("^([A-Za-z0-9_.:]+):\\s*([a-zA-Z_]+)").find(trimmed) ?: return@mapNotNull null
        OpItem(match.groupValues[1], match.groupValues[2])
    }.toList()

    suspend fun setAppOp(pkg: String, op: String, mode: String): OpResult = withContext(Dispatchers.IO) {
        if (!Backend.canRun()) return@withContext OpResult.failure("需要 Shizuku 或 Root 授权")
        val r = Backend.exec("appops set $pkg $op $mode")
        if (!r.ok) return@withContext OpResult.failure(r.text.ifBlank { "appops 执行失败" })
        val after = readAppOps(pkg).firstOrNull { it.op == op }
        if (after?.mode == mode) OpResult.success("$op → $mode")
        else OpResult.failure("修改未生效，当前为 ${after?.mode ?: "未知"}")
    }

    // ---------- 组件 ----------

    suspend fun components(context: Context, pkg: String): List<CompItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        @Suppress("DEPRECATION")
        val info = runCatching { pm.getPackageInfo(pkg, flags) }.getOrNull() ?: return@withContext emptyList()
        val out = mutableListOf<CompItem>()
        fun add(list: Array<out ComponentInfo>?, type: String) {
            list?.forEach { component ->
                val state = runCatching {
                    pm.getComponentEnabledSetting(ComponentName(pkg, component.name))
                }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                val enabled = when (state) {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    else -> component.enabled
                }
                val declaredPermission = when (component) {
                    is android.content.pm.ActivityInfo -> component.permission
                    is android.content.pm.ServiceInfo -> component.permission
                    else -> null
                }
                out += CompItem(
                    name = component.name,
                    type = type,
                    exported = component.exported,
                    enabled = enabled,
                    state = state,
                    permission = declaredPermission,
                )
            }
        }
        add(info.activities, "Activity")
        add(info.services, "Service")
        add(info.receivers, "Receiver")
        add(info.providers, "Provider")
        out.sortedWith(compareBy({ it.type }, { it.name }))
    }

    suspend fun setComponent(
        context: Context,
        pkg: String,
        component: String,
        enabled: Boolean,
        beforeEnabled: Boolean = !enabled,
    ): OpResult =
        withContext(Dispatchers.IO) {
            if (!Backend.canRun()) return@withContext OpResult.failure("需要 Shizuku 或 Root 授权")
            val target = "$pkg/$component"
            val cmd = if (enabled) "pm enable $target" else "pm disable $target"
            val r = Backend.exec(cmd)
            if (!r.ok) return@withContext OpResult.failure(r.text.ifBlank { "组件状态未改变" })
            val state = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getComponentEnabledSetting(ComponentName(pkg, component))
                }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            }
            val actual = when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
                else -> !enabled
            }
            if (actual == enabled) OpResult.success(if (enabled) "组件已启用" else "组件已禁用")
            else OpResult.failure(r.text.ifBlank { "组件状态未改变" })
        }

    // ---------- 特殊访问 ----------

    fun specialAccess(context: Context, pkg: String, uid: Int): List<SpecialAccess> {
        val ops = context.getSystemService(AppOpsManager::class.java)
        fun check(op: String): String {
            val mode = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ops.unsafeCheckOpNoThrow(op, uid, pkg)
                else @Suppress("DEPRECATION") ops.checkOpNoThrow(op, uid, pkg)
            }.getOrDefault(AppOpsManager.MODE_DEFAULT)
            return when (mode) {
                AppOpsManager.MODE_ALLOWED -> "allow"
                AppOpsManager.MODE_IGNORED -> "ignore"
                AppOpsManager.MODE_ERRORED -> "deny"
                AppOpsManager.MODE_DEFAULT -> "default"
                AppOpsManager.MODE_FOREGROUND -> "foreground"
                else -> "unknown"
            }
        }

        val list = mutableListOf(
            SpecialAccess("overlay", "悬浮窗", check(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW) == "allow", check(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW)),
            SpecialAccess("usage", "使用情况访问", check(AppOpsManager.OPSTR_GET_USAGE_STATS) == "allow", check(AppOpsManager.OPSTR_GET_USAGE_STATS)),
            SpecialAccess("install", "安装未知应用", check(OP_INSTALL_PACKAGES) == "allow", check(OP_INSTALL_PACKAGES)),
            SpecialAccess("write_settings", "修改系统设置", check(AppOpsManager.OPSTR_WRITE_SETTINGS) == "allow", check(AppOpsManager.OPSTR_WRITE_SETTINGS)),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            list += SpecialAccess(
                "all_files",
                "所有文件访问",
                check(OP_MANAGE_EXTERNAL_STORAGE) == "allow",
                check(OP_MANAGE_EXTERNAL_STORAGE),
            )
        }
        val power = context.getSystemService(PowerManager::class.java)
        val ignoring = runCatching { power?.isIgnoringBatteryOptimizations(pkg) == true }.getOrDefault(false)
        list += SpecialAccess("battery", "电池优化白名单", ignoring, if (ignoring) "allow" else "default")
        val listeners = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        }.getOrDefault("")
        list += SpecialAccess("notification", "通知使用权", listeners.contains(pkg), if (listeners.contains(pkg)) "allow" else "default")
        val accessibility = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services").orEmpty()
        }.getOrDefault("")
        list += SpecialAccess("accessibility", "无障碍服务", accessibility.contains(pkg), if (accessibility.contains(pkg)) "allow" else "default")
        return list
    }

    /** 特殊访问的后端可执行性：AppOps 类可经 Shizuku/Root 直接改，监听类必须进系统页面。 */
    fun specialOpName(id: String): String? = when (id) {
        "overlay" -> AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW
        "usage" -> AppOpsManager.OPSTR_GET_USAGE_STATS
        "install" -> OP_INSTALL_PACKAGES
        "write_settings" -> AppOpsManager.OPSTR_WRITE_SETTINGS
        "all_files" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) OP_MANAGE_EXTERNAL_STORAGE else null
        else -> null
    }

    suspend fun setSpecial(context: Context, pkg: String, id: String, granted: Boolean): OpResult {
        if (!Backend.canRun()) return OpResult.failure("需要 Shizuku 或 Root 授权")
        val result = when (id) {
            "notification" -> setSecureService(context, pkg, granted, "android.service.notification.NotificationListenerService", "enabled_notification_listeners")
            "accessibility" -> setSecureService(context, pkg, granted, "android.accessibilityservice.AccessibilityService", "enabled_accessibility_services")
            "battery" -> setBatteryWhitelist(pkg, granted)
            else -> {
                val op = specialOpName(id) ?: return OpResult.failure("不支持直接修改该项目")
                val mode = if (granted) "allow" else "default"
                val r = Backend.exec("appops set $pkg $op $mode")
                if (!r.ok) OpResult.failure(r.text.ifBlank { "appops 执行失败" }) else OpResult.success("已更新")
            }
        }
        if (!result.ok) return result
        val now = specialAccess(context, pkg, uidOf(context, pkg)).firstOrNull { it.id == id }?.granted ?: false
        return if (now == granted) result else OpResult.failure("修改未生效，系统未接受该变更")
    }

    private fun uidOf(context: Context, pkg: String): Int = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(pkg, 0).uid
    }.getOrDefault(-1)

    /** 在应用内修改「通知使用权 / 无障碍」：写 Secure 设置并重新读取校验。 */
    private suspend fun setSecureService(
        context: Context,
        pkg: String,
        granted: Boolean,
        serviceAction: String,
        key: String,
    ): OpResult {
        val component = findServiceComponent(context, pkg, serviceAction)
            ?: return OpResult.failure("该应用没有声明对应的服务组件")
        val raw = runCatching { Settings.Secure.getString(context.contentResolver, key) }.getOrNull().orEmpty()
        val list = raw.split(':').map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        list.removeAll { it.startsWith("$pkg/") }
        if (granted) list.add("$pkg/$component")
        val command = if (key == "enabled_accessibility_services") {
            val enabled = if (granted) "1" else if (list.isEmpty()) "0" else "1"
            "settings put secure $key '${list.joinToString(":")}' && settings put secure accessibility_enabled $enabled"
        } else {
            "settings put secure $key '${list.joinToString(":")}'"
        }
        val r = Backend.exec(command)
        if (!r.ok) return OpResult.failure(r.text.ifBlank { "写入系统设置失败" })
        val after = runCatching { Settings.Secure.getString(context.contentResolver, key) }.getOrNull().orEmpty()
        val present = after.split(':').any { it.trim().startsWith("$pkg/") }
        return if (present == granted) OpResult.success(if (granted) "已授权" else "已取消授权")
        else OpResult.failure("系统未接受该变更")
    }

    /** 电池优化白名单：deviceidle whitelist，可在应用内直接增删。 */
    private suspend fun setBatteryWhitelist(pkg: String, granted: Boolean): OpResult {
        val r = Backend.exec("cmd deviceidle whitelist ${if (granted) "+" else "-"}$pkg")
        if (!r.ok) return OpResult.failure(r.text.ifBlank { "系统拒绝修改白名单" })
        return OpResult.success(if (granted) "已加入白名单" else "已移出白名单")
    }

    fun findServiceComponent(context: Context, pkg: String, serviceAction: String): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val info = runCatching { pm.getPackageInfo(pkg, PackageManager.GET_SERVICES) }.getOrNull() ?: return null
        val candidates = info.services.orEmpty()
        val action = if (serviceAction == "android.service.notification.NotificationListenerService") {
            "android.service.notification.NotificationListenerService"
        } else {
            "android.accessibilityservice.AccessibilityService"
        }
        @Suppress("DEPRECATION")
        val resolved = runCatching {
            pm.queryIntentServices(android.content.Intent(action).setPackage(pkg), 0)
        }.getOrNull().orEmpty()
        resolved.firstOrNull()?.serviceInfo?.name?.let { return it }
        return candidates.firstOrNull()?.name
    }
}
