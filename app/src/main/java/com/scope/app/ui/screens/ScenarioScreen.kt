package com.scope.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scope.app.core.AppEntry
import com.scope.app.core.Backend
import com.scope.app.core.Prefs
import com.scope.app.core.Scenario
import com.scope.app.core.ScenarioStep
import com.scope.app.core.ScenarioStore
import com.scope.app.core.StepOutcome
import com.scope.app.ui.components.EmptyHint
import com.scope.app.ui.components.ScreenTitle
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScenarioScreen(
    onBack: () -> Unit,
    onOpenEdit: (String) -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    val scenarios by ScenarioStore.list.collectAsState()
    val privileged = Backend.snapshot().privileged

    var picking by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var running by remember { mutableStateOf<Scenario?>(null) }
    var progressText by remember { mutableStateOf("") }
    var outcomes by remember { mutableStateOf<List<StepOutcome>>(emptyList()) }
    var armed by remember { mutableStateOf(false) }

    if (picking) {
        AppMultiPickerScreen(
            title = "选择要冻结的应用",
            onBack = { picking = false },
            onConfirm = {
                picked = it
                picking = false
                if (it.isEmpty()) notify("没有选择应用") else {
                    name = "自定义方案 ${scenarios.size + 1}"
                    naming = true
                }
            },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "场景引擎",
            subtitle = if (privileged) "按顺序执行真实系统操作" else "需要 Shizuku 或 Root 才能执行",
            onBack = onBack,
            actions = { TextButton(text = "新建", onClick = { picking = true }) },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (scenarios.isEmpty()) {
                item { EmptyHint("还没有场景。新建后选择应用与操作，即可一键执行。") }
            }
            items(scenarios, key = { it.id }) { scenario ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    colors = CardDefaults.defaultColors(color = scope.card),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(scenario.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${scenario.steps.size} 个步骤",
                            fontSize = 12.sp,
                            color = scope.textSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        scenario.steps.take(3).forEach { step ->
                            Text(
                                text = "· ${step.describe()}",
                                fontSize = 12.sp,
                                color = scope.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (scenario.steps.size > 3) {
                            Text("· 其余 ${scenario.steps.size - 3} 步", fontSize = 12.sp, color = scope.textSecondary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                text = "执行",
                                onClick = {
                                    outcomes = emptyList()
                                    progressText = ""
                                    armed = !Prefs.confirmBatch.value
                                    running = scenario
                                },
                                enabled = scenario.steps.isNotEmpty() && privileged,
                                minHeight = 34.dp,
                                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            )
                            TextButton(
                                text = "编辑",
                                onClick = { onOpenEdit(scenario.id) },
                                minHeight = 34.dp,
                                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            )
                            TextButton(
                                text = "删除",
                                onClick = { coroutine.launch { ScenarioStore.delete(scenario.id) } },
                                minHeight = 34.dp,
                                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (naming) {
        OverlayDialog(
            show = true,
            title = "命名场景",
            summary = "${picked.size} 个应用将被冻结",
            onDismissRequest = { naming = false },
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "场景名称",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    cornerRadius = 12.dp,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton("取消", onClick = { naming = false }, modifier = Modifier.weight(1f))
                    TextButton(
                        text = "保存",
                        onClick = {
                            val scenario = Scenario(
                                name = name.ifBlank { "未命名方案" },
                                steps = picked.map { ScenarioStep("FREEZE", it.pkg, it.label) },
                            )
                            naming = false
                            coroutine.launch {
                                ScenarioStore.save(scenario)
                                notify("已保存 ${scenario.name}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    val current = running
    OverlayBottomSheet(
        show = current != null,
        title = current?.let { "执行 ${it.name}" } ?: "",
        onDismissRequest = { if (progressText.isEmpty()) running = null },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            current?.steps?.forEach { step ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(scope.green),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(step.describe(), fontSize = 13.sp)
                }
            }
            if (progressText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(progressText, fontSize = 12.sp, color = scope.textSecondary)
            }
            if (outcomes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                outcomes.forEach { outcome ->
                    Text(
                        text = "${if (outcome.ok) "✓" else "·"} ${outcome.step.describe()}：${if (outcome.ok) "成功" else outcome.message}",
                        fontSize = 12.sp,
                        color = if (outcome.ok) scope.green else Color(0xFFE0655B),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                text = when {
                    outcomes.isNotEmpty() -> "关闭"
                    armed -> "确认执行 ${current?.steps?.size ?: 0} 步"
                    else -> "开始执行"
                },
                onClick = {
                    val scenario = current ?: return@TextButton
                    if (outcomes.isNotEmpty()) {
                        running = null
                        return@TextButton
                    }
                    if (!armed) {
                        armed = true
                        return@TextButton
                    }
                    coroutine.launch {
                        val result = ScenarioStore.run(context, scenario) { index, step ->
                            progressText = "正在执行 ${index + 1}/${scenario.steps.size} · ${step.describe()}"
                        }
                        outcomes = result
                        progressText = ""
                        notify("完成 ${result.count { it.ok }}/${result.size} 步")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = outcomes.isNotEmpty() || privileged,
            )
            if (armed && outcomes.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "执行会立即修改系统状态，可在操作记录中恢复支持回滚的步骤。",
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 场景编辑：改名、调整顺序、删除步骤、继续添加冻结步骤。 */
@Composable
fun ScenarioEditScreen(scenarioId: String, onBack: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    val scope = LocalScope.current
    val coroutine = rememberCoroutineScope()
    val stored = ScenarioStore.get(scenarioId)
    var name by remember(scenarioId) { mutableStateOf(stored?.name.orEmpty()) }
    val steps = remember(scenarioId) { mutableStateListOf<ScenarioStep>().apply { addAll(stored?.steps.orEmpty()) } }
    var picking by remember { mutableStateOf(false) }

    if (picking) {
        AppMultiPickerScreen(
            title = "添加冻结步骤",
            onBack = { picking = false },
            onConfirm = { apps ->
                picking = false
                if (apps.isEmpty()) {
                    notify("没有选择应用")
                } else {
                    steps.addAll(apps.map { ScenarioStep("FREEZE", it.pkg, it.label) })
                }
            },
        )
        return
    }

    if (stored == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(title = "编辑场景", onBack = onBack)
            EmptyHint("场景不存在或已被删除")
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "编辑场景",
            subtitle = "${steps.size} 个步骤",
            onBack = onBack,
            actions = {
                TextButton(
                    text = "保存",
                    onClick = {
                        coroutine.launch {
                            ScenarioStore.save(stored.copy(name = name.ifBlank { stored.name }, steps = steps.toList()))
                            notify("已保存")
                            onBack()
                        }
                    },
                )
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PrefCard {
                    Column(Modifier.padding(14.dp)) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "场景名称",
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            cornerRadius = 12.dp,
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                            ),
                        )
                    }
                }
            }
            if (steps.isEmpty()) {
                item { EmptyHint("还没有步骤，点击下方按钮添加") }
            }
            items(steps.size, key = { index -> "$index-${steps[index].pkg}" }) { index ->
                val step = steps[index]
                PrefCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(step.describe(), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(step.pkg, fontSize = 11.sp, color = scope.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(
                            text = "上移",
                            onClick = {
                                if (index > 0) {
                                    val item = steps.removeAt(index)
                                    steps.add(index - 1, item)
                                }
                            },
                            enabled = index > 0,
                            minHeight = 30.dp,
                            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        )
                        TextButton(
                            text = "下移",
                            onClick = {
                                if (index < steps.size - 1) {
                                    val item = steps.removeAt(index)
                                    steps.add(index + 1, item)
                                }
                            },
                            enabled = index < steps.size - 1,
                            minHeight = 30.dp,
                            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        )
                        TextButton(
                            text = "删除",
                            onClick = { steps.removeAt(index) },
                            minHeight = 30.dp,
                            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            item {
                TextButton(
                    text = "添加冻结步骤",
                    onClick = { picking = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = "保存后场景按当前顺序执行；执行前仍会读取真实状态并逐条验证。",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    color = scope.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
