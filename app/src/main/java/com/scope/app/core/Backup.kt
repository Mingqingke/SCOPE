package com.scope.app.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupSettings(
    val themeMode: Int = 0,
    val cardFeedback: Int = 0,
    val reduceMotion: Boolean = false,
    val confirmRisky: Boolean = true,
    val confirmPermission: Boolean = true,
    val confirmBatch: Boolean = true,
    val backendPreference: Int = 0,
)

@Serializable
data class BackupBundle(
    val app: String = "SCOPE",
    val schema: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: BackupSettings = BackupSettings(),
    val scenarios: List<Scenario> = emptyList(),
    val history: List<OpRecord> = emptyList(),
)

/** 备份只包含设置、场景与操作记录，不触碰应用与系统数据。 */
object Backup {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun snapshot(): BackupBundle = BackupBundle(
        settings = BackupSettings(
            themeMode = Prefs.themeMode.value,
            cardFeedback = Prefs.cardFeedback.value,
            reduceMotion = Prefs.reduceMotion.value,
            confirmRisky = Prefs.confirmRisky.value,
            confirmPermission = Prefs.confirmPermission.value,
            confirmBatch = Prefs.confirmBatch.value,
            backendPreference = Prefs.backendPreference.value,
        ),
        scenarios = ScenarioStore.list.value,
        history = History.records.value,
    )

    fun encode(bundle: BackupBundle = snapshot()): String = json.encodeToString(bundle)

    fun fileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())
        return "SCOPE-backup-$stamp.json"
    }

    fun parse(text: String): Result<BackupBundle> = runCatching {
        val bundle = json.decodeFromString<BackupBundle>(text)
        require(bundle.app == "SCOPE") { "不是 SCOPE 的备份文件" }
        require(bundle.schema <= 1) { "备份来自更新的版本，无法导入" }
        bundle
    }

    suspend fun apply(context: Context, bundle: BackupBundle): OpResult = withContext(Dispatchers.IO) {
        runCatching {
            val s = bundle.settings
            Prefs.setThemeMode(s.themeMode)
            Prefs.setCardFeedback(s.cardFeedback)
            Prefs.setReduceMotion(s.reduceMotion)
            Prefs.setConfirmRisky(s.confirmRisky)
            Prefs.setConfirmPermission(s.confirmPermission)
            Prefs.setConfirmBatch(s.confirmBatch)
            Prefs.setBackendPreference(s.backendPreference)
            ScenarioStore.replaceAll(bundle.scenarios)
            History.replaceAll(bundle.history)
            OpResult.success(
                "已恢复 ${bundle.scenarios.size} 个场景、${bundle.history.size} 条记录" +
                    "${if (bundle.history.isNotEmpty()) "（${bundle.history.first().timeText()}）" else ""}",
            )
        }.getOrElse { OpResult.failure("恢复失败：${it.message ?: "文件无法解析"}") }
    }

    fun write(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
    }.getOrDefault(false)

    fun read(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    }.getOrNull()
}
