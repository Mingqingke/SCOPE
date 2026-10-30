package com.scope.app

import com.scope.app.core.AppRepo
import com.scope.app.core.Backup
import com.scope.app.core.Device
import com.scope.app.core.OpItem
import com.scope.app.core.OpRecord
import com.scope.app.core.Scenario
import com.scope.app.core.ScenarioStep
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test
    fun appOpsOutputIsParsedWithTimeSuffix() {
        val output = """
            android:coarse_location: allow
            android:fine_location: ignore; time=+1h2m3s
            android:system_alert_window: default
            No operations for package
        """.trimIndent()
        val ops = AppRepo.parseAppOpsOutput(output)
        assertEquals(3, ops.size)
        assertEquals("android:coarse_location", ops[0].op)
        assertEquals("allow", ops[0].mode)
        assertEquals("android:fine_location", ops[1].op)
        assertEquals("ignore", ops[1].mode)
        assertEquals("default", ops[2].mode)
    }

    @Test
    fun appOpsDisplayNameDropsAndroidPrefix() {
        assertEquals("system_alert_window", OpItem("android:system_alert_window", "allow").name)
        assertEquals("camera", OpItem("camera", "allow").name)
    }

    @Test
    fun scenarioStepDescribeIsReadable() {
        assertEquals("冻结 示例应用", ScenarioStep("FREEZE", "com.demo", "示例应用").describe())
        assertEquals(
            "AppOps 示例应用 · android:camera → ignore",
            ScenarioStep("APPOP", "com.demo", "示例应用", "android:camera|ignore").describe(),
        )
    }

    @Test
    fun scenarioAndHistorySurviveJsonRoundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val scenario = Scenario(name = "夜间", steps = listOf(ScenarioStep("FREEZE", "com.demo", "示例")))
        val restored = json.decodeFromString<Scenario>(json.encodeToString(scenario))
        assertEquals("夜间", restored.name)
        assertEquals(1, restored.steps.size)

        val record = OpRecord(
            title = "冻结 示例",
            target = "com.demo",
            action = "FREEZE",
            before = "enabled",
            after = "disabled",
            restorable = true,
        )
        val recordBack = json.decodeFromString<OpRecord>(json.encodeToString(record))
        assertEquals("FREEZE", recordBack.action)
        assertTrue(recordBack.restorable)
    }

    @Test
    fun osVersionIsParsedFromBuildStrings() {
        // HyperOS / MIUI / 通用三类真实字符串
        assertEquals("1.0.3.0.TKXCNXM", Device.parseOsVersion("OS1.0.3.0.TKXCNXM"))
        assertEquals("816.0.4.0.UMACNXM", Device.parseOsVersion("V816.0.4.0.UMACNXM"))
        assertEquals("1.2.3", Device.parseOsVersion("build 1.2.3 release"))
        assertNull(Device.parseOsVersion("S918BXXU4CXK7"))
    }

    @Test
    fun backupRoundTripKeepsScenariosAndRejectsForeignFile() {
        val scenario = Scenario(name = "通勤", steps = listOf(ScenarioStep("FREEZE", "com.demo", "示例")))
        val bundle = com.scope.app.core.BackupBundle(
            scenarios = listOf(scenario),
            history = listOf(OpRecord(title = "冻结 示例", target = "com.demo", action = "FREEZE", restorable = true)),
        )
        val text = Backup.encode(bundle)
        val parsed = Backup.parse(text)
        assertTrue(parsed.isSuccess)
        assertEquals(1, parsed.getOrNull()?.scenarios?.size)
        assertEquals("通勤", parsed.getOrNull()?.scenarios?.first()?.name)

        val foreign = Backup.parse("""{"app":"OTHER","schema":1}""")
        assertTrue(foreign.isFailure)
        assertNotNull(foreign.exceptionOrNull())
    }
}
