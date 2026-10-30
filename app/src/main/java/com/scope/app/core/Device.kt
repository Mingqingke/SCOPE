package com.scope.app.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import java.io.File
import java.util.Locale
import kotlin.math.sqrt

/** 设备信息全部来自真实读取，读不到的字段保持为空，不填示例值。 */
data class DeviceSnapshot(
    val model: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val androidRelease: String = "",
    val sdk: Int = 0,
    val osName: String = "",
    val osVersion: String = "",
    val buildIncremental: String = "",
    val buildDisplay: String = "",
    val securityPatch: String = "",
    val kernel: String = "",
    val cpu: String = "",
    val abi: String = "",
    val ramTotal: Long = 0L,
    val storageTotal: Long = 0L,
    val storageFree: Long = 0L,
    val batteryCapacityMah: Int? = null,
    val screenInches: Float? = null,
    val resolution: String = "",
    val deviceCode: String = "",
    val socVendor: String = "",
    val densityDpi: Int = 0,
    val buildId: String = "",
)

object Device {

    fun snapshot(context: Context): DeviceSnapshot {
        val os = detectOs()
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val storageTotal = stat?.let { it.blockCountLong * it.blockSizeLong } ?: 0L
        val storageFree = stat?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L
        val am = context.getSystemService(ActivityManager::class.java)
        val ram = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }?.totalMem ?: 0L

        return DeviceSnapshot(
            model = Build.MODEL.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            osName = os.first,
            osVersion = os.second,
            buildIncremental = Build.VERSION.INCREMENTAL.orEmpty(),
            buildDisplay = Build.DISPLAY.orEmpty(),
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            kernel = System.getProperty("os.version").orEmpty(),
            cpu = cpuName(),
            abi = Build.SUPPORTED_ABIS?.firstOrNull().orEmpty(),
            ramTotal = ram,
            storageTotal = storageTotal,
            storageFree = storageFree,
            batteryCapacityMah = batteryCapacity(),
            screenInches = screenInches(metrics),
            resolution = if (metrics.widthPixels > 0) "${minOf(metrics.widthPixels, metrics.heightPixels)}×${maxOf(metrics.widthPixels, metrics.heightPixels)}" else "",
            deviceCode = Build.DEVICE.orEmpty(),
            socVendor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER.orEmpty() else "",
            densityDpi = metrics.densityDpi,
            buildId = Build.ID.orEmpty(),
        )
    }

    /**
     * 厂商系统识别只用于展示，不作为能力判断依据。
     * 版本号优先解析真实的 incremental / display 字段，解析不到就留空，不填示例值。
     */
    fun detectOs(): Pair<String, String> {
        val man = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
        val incremental = Build.VERSION.INCREMENTAL.orEmpty()
        val display = Build.DISPLAY.orEmpty()
        val hay = "$incremental $display ${Build.ID.orEmpty()}"
        val version = parseOsVersion(incremental)
            ?: parseOsVersion(display)
            ?: parseOsVersion(Build.ID.orEmpty())
            ?: ""

        val name = when {
            man.contains("xiaomi") || man.contains("redmi") || man.contains("poco") -> {
                val hyper = hay.contains("hyperos", true) ||
                    Regex("(^|\\s)(OS|V8[0-9][0-9])", RegexOption.IGNORE_CASE).containsMatchIn(incremental)
                if (hyper) "HyperOS" else "MIUI"
            }
            man.contains("samsung") -> "One UI"
            man.contains("oneplus") -> "OxygenOS"
            man.contains("oppo") || man.contains("realme") -> "ColorOS"
            man.contains("vivo") || man.contains("iqoo") -> "OriginOS"
            man.contains("honor") -> "MagicOS"
            man.contains("huawei") -> "HarmonyOS"
            man.contains("meizu") -> "Flyme"
            man.contains("asus") -> "ZenUI"
            else -> "Android"
        }
        val fallbackVersion = if (name == "Android") Build.VERSION.RELEASE.orEmpty() else ""
        return name to version.ifBlank { fallbackVersion }
    }

    /** 从系统字符串里取出形如 1.0.3.0.TKXCNXM / OS2.0.5.0.VNBCNXM 的版本号。 */
    fun parseOsVersion(raw: String): String? {
        if (raw.isBlank()) return null
        val os = Regex("OS\\s*(\\d+(?:\\.\\d+){1,3}(?:\\.[A-Za-z0-9]+)?)", RegexOption.IGNORE_CASE).find(raw)
        if (os != null) return os.groupValues[1]
        val v = Regex("V(\\d+(?:\\.\\d+){1,3}(?:\\.[A-Za-z0-9]+)?)").find(raw)
        if (v != null) return v.groupValues[1]
        val plain = Regex("(\\d+\\.\\d+\\.\\d+(?:\\.[A-Za-z0-9]+)?)").find(raw)
        return plain?.groupValues?.get(1)
    }

    private fun cpuName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL.orEmpty()
            if (soc.isNotBlank() && soc != "unknown") return soc
        }
        return runCatching {
            File("/proc/cpuinfo").readLines().firstOrNull {
                it.startsWith("Hardware") || it.startsWith("model name")
            }?.substringAfter(':')?.trim().orEmpty()
        }.getOrDefault("")
    }

    private fun batteryCapacity(): Int? {
        val candidates = listOf("charge_full_design", "charge_full")
        for (name in candidates) {
            val value = runCatching {
                File("/sys/class/power_supply/battery/$name").readText().trim().toLong()
            }.getOrNull() ?: continue
            if (value > 0) return (value / 1000).toInt()
        }
        return null
    }

    private fun screenInches(metrics: DisplayMetrics): Float? {
        if (metrics.xdpi <= 0f || metrics.ydpi <= 0f) return null
        val w = metrics.widthPixels / metrics.xdpi
        val h = metrics.heightPixels / metrics.ydpi
        val diagonal = sqrt(w * w + h * h)
        return if (diagonal.isFinite() && diagonal > 0f) diagonal else null
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "—"
        val gb = bytes / 1024.0 / 1024.0 / 1024.0
        return if (gb >= 1) String.format(Locale.ROOT, "%.1f GB", gb)
        else String.format(Locale.ROOT, "%.0f MB", bytes / 1024.0 / 1024.0)
    }
}
