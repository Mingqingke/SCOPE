package com.scope.app.core

import android.content.Context
import android.os.Build

data class Capability(
    val group: String,
    val name: String,
    val desc: String,
    val requirement: String,
    val available: Boolean,
    val reason: String,
)

/**
 * 系统能力登记表：每项都带唯一 ID、所需后端、Android 版本要求与当前限制。
 * 能力是否可用由真实后端与版本共同决定，不由一个总开关决定。
 */
object Capabilities {

    fun evaluate(context: Context, status: BackendStatus, device: DeviceSnapshot): List<Capability> {
        val priv = status.privileged
        val privReason = if (priv) "可用（${status.active.label}）" else "需要 Shizuku 或 Root"
        val list = mutableListOf(
            Capability(
                group = "应用",
                name = "应用列表与详情",
                desc = "读取已安装应用、版本、签名、权限与组件清单",
                requirement = "Android ${Build.VERSION_CODES.O} 及以上",
                available = true,
                reason = "普通模式可用",
            ),
            Capability(
                group = "应用",
                name = "冻结 / 解冻应用",
                desc = "通过 pm disable-user 停用应用，保留数据",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "应用",
                name = "组件启停",
                desc = "启用或禁用 Activity / Service / Receiver / Provider",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "权限",
                name = "运行时权限授予 / 撤销",
                desc = "直接调用 pm grant / pm revoke 并复查结果",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "权限",
                name = "特殊访问读取",
                desc = "悬浮窗、使用情况、安装未知应用、电池优化等真实状态",
                requirement = "Android 6.0 及以上",
                available = true,
                reason = "普通模式可用",
            ),
            Capability(
                group = "权限",
                name = "特殊访问直接修改",
                desc = "通过 appops 直接切换可写的 AppOps 项",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "AppOps",
                name = "AppOps 读取",
                desc = "读取应用的 AppOps 模式（allow / ignore / default）",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "AppOps",
                name = "AppOps 修改",
                desc = "切换 AppOps 模式并在修改后重新校验",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "场景",
                name = "场景执行",
                desc = "按顺序执行冻结、AppOps 等真实系统操作",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "场景",
                name = "操作恢复",
                desc = "依据操作记录把支持的目标恢复为修改前状态",
                requirement = "Shizuku 或 Root",
                available = priv,
                reason = privReason,
            ),
            Capability(
                group = "后端",
                name = "Shizuku Shell",
                desc = "以 shell 身份执行系统命令",
                requirement = "Shizuku 服务运行且已授权",
                available = status.shizukuReady,
                reason = when {
                    !status.shizukuInstalled -> "未安装 Shizuku"
                    !status.shizukuRunning -> "Shizuku 服务未运行"
                    !status.shizukuGranted -> "尚未授权 SCOPE"
                    else -> "可用（shell）"
                },
            ),
            Capability(
                group = "后端",
                name = "Root Shell",
                desc = "以 root 身份执行系统命令",
                requirement = "设备已 Root 且已授权",
                available = status.rootGranted,
                reason = if (status.rootGranted) "可用（root）" else "未获得 Root 授权",
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            list += Capability(
                group = "权限",
                name = "所有文件访问",
                desc = "MANAGE_EXTERNAL_STORAGE 状态读取与切换",
                requirement = "Android 11 及以上",
                available = true,
                reason = if (priv) "可读取，直接修改需高级授权" else "普通模式可读取状态",
            )
        }
        return list
    }
}
