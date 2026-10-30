package com.scope.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 所有系统操作的唯一入口：读取修改前状态 → 执行 → 复查 → 写操作记录。
 * UI 不直接拼命令、不直接判断成功。
 */
object Ops {

    private suspend fun enabledState(context: Context, pkg: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(pkg, 0).enabled
        }.getOrDefault(true)
    }

    suspend fun setAppEnabled(context: Context, pkg: String, label: String, enable: Boolean): OpResult {
        val before = enabledState(context, pkg)
        val result = AppRepo.setAppEnabled(context, pkg, enable)
        if (result.ok) AppRepo.markStale()
        History.add(
            OpRecord(
                title = if (enable) "启用 ${label}" else "冻结 ${label}",
                target = pkg,
                action = if (enable) "UNFREEZE" else "FREEZE",
                before = if (before) "enabled" else "disabled",
                after = if (result.ok) (if (enable) "enabled" else "disabled") else (if (before) "enabled" else "disabled"),
                backend = Backend.snapshot().active.label,
                success = result.ok,
                error = if (result.ok) "" else result.message,
                restorable = result.ok,
            ),
        )
        return result
    }

    suspend fun setAppOp(context: Context, pkg: String, label: String, op: String, mode: String): OpResult {
        val before = AppRepo.readAppOps(pkg).firstOrNull { it.op == op }?.mode.orEmpty()
        val result = AppRepo.setAppOp(pkg, op, mode)
        History.add(
            OpRecord(
                title = "AppOps · ${label}",
                target = pkg,
                action = "APPOP",
                detail = op,
                before = before,
                after = if (result.ok) mode else before,
                backend = Backend.snapshot().active.label,
                success = result.ok,
                error = if (result.ok) "" else result.message,
                restorable = result.ok && before.isNotBlank(),
            ),
        )
        return result
    }

    suspend fun setComponent(
        context: Context,
        pkg: String,
        label: String,
        component: String,
        enable: Boolean,
        beforeEnabled: Boolean,
    ): OpResult {
        val result = AppRepo.setComponent(context, pkg, component, enable, beforeEnabled)
        if (result.ok) AppRepo.markStale()
        History.add(
            OpRecord(
                title = (if (enable) "启用组件 · " else "禁用组件 · ") + component.substringAfterLast('.'),
                target = pkg,
                action = "COMPONENT",
                detail = component,
                before = if (beforeEnabled) "enabled" else "disabled",
                after = if (result.ok) (if (enable) "enabled" else "disabled") else (if (beforeEnabled) "enabled" else "disabled"),
                backend = Backend.snapshot().active.label,
                success = result.ok,
                error = if (result.ok) "" else result.message,
                restorable = result.ok,
            ),
        )
        return result
    }

    suspend fun setPermission(
        context: Context,
        pkg: String,
        label: String,
        permission: String,
        grant: Boolean,
    ): OpResult {
        val result = if (grant) AppRepo.grant(context, pkg, permission) else AppRepo.revoke(context, pkg, permission)
        if (result.ok) AppRepo.markStale()
        History.add(
            OpRecord(
                title = (if (grant) "授予 " else "撤销 ") + permission.substringAfterLast('.'),
                target = pkg,
                action = "PERMISSION",
                detail = permission,
                before = if (grant) "denied" else "granted",
                after = if (result.ok) (if (grant) "granted" else "denied") else (if (grant) "denied" else "granted"),
                backend = Backend.snapshot().active.label,
                success = result.ok,
                error = if (result.ok) "" else result.message,
                restorable = result.ok,
            ),
        )
        return result
    }

    suspend fun setSpecial(context: Context, pkg: String, label: String, id: String, name: String, grant: Boolean): OpResult {
        val before = AppRepo.detail(context, pkg)?.special?.firstOrNull { it.id == id }?.granted ?: false
        val result = AppRepo.setSpecial(context, pkg, id, grant)
        if (result.ok) AppRepo.markStale()
        History.add(
            OpRecord(
                title = "特殊访问 · $name",
                target = pkg,
                action = "SPECIAL",
                detail = id,
                before = if (before) "allow" else "default",
                after = if (result.ok) (if (grant) "allow" else "default") else (if (before) "allow" else "default"),
                backend = Backend.snapshot().active.label,
                success = result.ok,
                error = if (result.ok) "" else result.message,
                restorable = result.ok,
            ),
        )
        return result
    }
}
