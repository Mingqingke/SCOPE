package com.scope.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class OpRecord(
    val id: String = UUID.randomUUID().toString(),
    val time: Long = System.currentTimeMillis(),
    val title: String,
    val target: String,
    val action: String,
    val detail: String = "",
    val before: String = "",
    val after: String = "",
    val backend: String = "",
    val success: Boolean = true,
    val error: String = "",
    val restorable: Boolean = false,
) {
    fun timeText(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))
}

object History {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private var file: File? = null

    private val _records = MutableStateFlow<List<OpRecord>>(emptyList())
    val records: StateFlow<List<OpRecord>> = _records

    fun init(context: Context) {
        if (file != null) return
        file = File(context.filesDir, "history.json")
        runCatching {
            val text = file?.takeIf { it.exists() }?.readText().orEmpty()
            if (text.isNotBlank()) _records.value = json.decodeFromString<List<OpRecord>>(text)
        }
    }

    suspend fun add(record: OpRecord) = mutex.withLock {
        val list = (listOf(record) + _records.value).take(500)
        _records.value = list
        withContext(Dispatchers.IO) { persist(list) }
    }

    suspend fun replaceAll(records: List<OpRecord>) = mutex.withLock {
        _records.value = records.take(500)
        withContext(Dispatchers.IO) { persist(_records.value) }
    }

    suspend fun clear() = mutex.withLock {
        _records.value = emptyList()
        withContext(Dispatchers.IO) { file?.delete() }
    }

    fun exportJson(): String = json.encodeToString(_records.value)

    private fun persist(list: List<OpRecord>) {
        runCatching { file?.writeText(json.encodeToString(list)) }
    }

    /** 只有确实能回到修改前状态的操作才提供恢复。 */
    suspend fun restore(context: Context, record: OpRecord): OpResult {
        if (!record.restorable) return OpResult.failure("该操作不可恢复")
        if (!Backend.canRun()) return OpResult.failure("需要 Shizuku 或 Root 授权")
        val result = when (record.action) {
            "FREEZE", "UNFREEZE" -> {
                val wantEnabled = record.action == "UNFREEZE"
                AppRepo.setAppEnabled(context, record.target, wantEnabled)
            }
            "APPOP" -> AppRepo.setAppOp(record.target, record.detail, record.before)
            "COMPONENT" -> AppRepo.setComponent(context, record.target, record.detail, record.before == "enabled")
            "PERMISSION" -> {
                if (record.before == "granted") AppRepo.grant(context, record.target, record.detail)
                else AppRepo.revoke(context, record.target, record.detail)
            }
            "SPECIAL" -> AppRepo.setSpecial(context, record.target, record.detail, record.before == "allow")
            else -> OpResult.failure("不支持恢复该类型操作")
        }
        if (result.ok) {
            add(
                OpRecord(
                    title = "恢复：${record.title}",
                    target = record.target,
                    action = record.action,
                    detail = record.detail,
                    before = record.after,
                    after = record.before,
                    backend = Backend.snapshot().active.label,
                    success = true,
                    restorable = false,
                ),
            )
        }
        return result
    }
}
