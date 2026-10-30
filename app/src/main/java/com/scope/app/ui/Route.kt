package com.scope.app.ui

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 全应用唯一的路由定义。四个主入口各自持有一条独立的返回栈，
 * 其余页面在当前标签的栈内 push / pop，系统返回由 Miuix Nav 处理。
 */
@Serializable
sealed interface Route : NavKey {

    // 四个主入口
    @Serializable data object Home : Route
    @Serializable data object Apps : Route
    @Serializable data object Manage : Route
    @Serializable data object Settings : Route

    // 设置中心二级页面
    @Serializable data object SettingsSystem : Route
    @Serializable data object SettingsAppearance : Route
    @Serializable data object SettingsSafety : Route
    @Serializable data object SettingsData : Route
    @Serializable data object SettingsAboutDevice : Route

    // 设置中心三级页面
    @Serializable data object DeviceInfo : Route
    @Serializable data object About : Route
    @Serializable data object Licenses : Route

    // 功能页面
    @Serializable data object Auth : Route
    @Serializable data object Capabilities : Route
    @Serializable data object Scenarios : Route
    @Serializable data object History : Route
    @Serializable data object Diagnostics : Route
    @Serializable data object PermissionCenter : Route

    @Serializable data class ScenarioEdit(val id: String) : Route
    @Serializable data class AppDetail(val pkg: String, val label: String) : Route
    @Serializable data class Permissions(val pkg: String, val label: String) : Route
    @Serializable data class AppOps(val pkg: String, val label: String) : Route
    @Serializable data class Components(val pkg: String, val label: String) : Route
    @Serializable data class SpecialAccess(val pkg: String, val label: String) : Route
    @Serializable data class Picker(val title: String, val subtitle: String, val target: PickerTarget) : Route
}

@Serializable
enum class PickerTarget { APPOPS, COMPONENTS, SPECIAL, PERMISSIONS }
