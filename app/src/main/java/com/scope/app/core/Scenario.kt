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
import java.util.UUID

@Serializable
data class ScenarioStep(
    val kind: String,
    val pkg: String,
    val label: String = "",
    val extra: String = "",
) {
    fun describe(): String = when (kind) {
        "FREEZE" -> "冻结 $label"
        "UNFREEZE" -> "启用 $label"
        "APPOP" -> "AppOps $label · ${extra.replace("|", " → ")}"
        else -> "$kind $label"
    }
}

@Serializable
data class Scenario(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val note: String = "",
    val steps: List<ScenarioStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class StepOutcome(val step: ScenarioStep, val ok: Boolean, val message: String)

object ScenarioStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private var file: File? = null

    private val _list = MutableStateFlow<List<Scenario>>(emptyList())
    val list: StateFlow<List<Scenario>> = _list

    fun init(context: Context) {
        if (file != null) return
        file = File(context.filesDir, "scenarios.json")
        runCatching {
            val text = file?.takeIf { it.exists() }?.readText().orEmpty()
            if (text.isNotBlank()) _list.value = json.decodeFromString<List<Scenario>>(text)
        }
    }

    fun get(id: String): Scenario? = _list.value.firstOrNull { it.id == id }

    suspend fun save(scenario: Scenario) = mutex.withLock {
        val next = _list.value.filterNot { it.id == scenario.id } + scenario
        _list.value = next.sortedByDescending { it.createdAt }
        withContext(Dispatchers.IO) { persist() }
    }

    suspend fun replaceAll(list: List<Scenario>) = mutex.withLock {
        _list.value = list
        withContext(Dispatchers.IO) { persist() }
    }

    suspend fun delete(id: String) = mutex.withLock {
        _list.value = _list.value.filterNot { it.id == id }
        withContext(Dispatchers.IO) { persist() }
    }

    fun exportJson(): String = json.encodeToString(_list.value)

    private fun persist() {
        runCatching { file?.writeText(json.encodeToString(_list.value)) }
    }

    /** 顺序执行真实操作，每一步都写入操作记录。 */
    suspend fun run(
        context: Context,
        scenario: Scenario,
        onStep: (index: Int, step: ScenarioStep) -> Unit,
    ): List<StepOutcome> {
        if (!Backend.canRun()) {
            return listOf(StepOutcome(ScenarioStep("NONE", "", "系统操作"), false, "需要 Shizuku 或 Root 授权"))
        }
        val outcomes = mutableListOf<StepOutcome>()
        scenario.steps.forEachIndexed { index, step ->
            onStep(index, step)
            val result = when (step.kind) {
                "FREEZE" -> Ops.setAppEnabled(context, step.pkg, step.label.ifBlank { step.pkg }, false)
                "UNFREEZE" -> Ops.setAppEnabled(context, step.pkg, step.label.ifBlank { step.pkg }, true)
                "APPOP" -> {
                    val op = step.extra.substringBefore("|")
                    val mode = step.extra.substringAfter("|", "default")
                    Ops.setAppOp(context, step.pkg, step.label.ifBlank { step.pkg }, op, mode)
                }
                else -> OpResult.failure("未知操作类型 ${step.kind}")
            }
            outcomes += StepOutcome(step, result.ok, result.message)
        }
        return outcomes
    }
}
