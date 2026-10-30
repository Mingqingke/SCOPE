package com.scope.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 设置存储：同步写入，读取即用，迁移旧键，保证升级后不重置用户选择。
 */
object Prefs {

    private const val FILE = "scope_prefs"

    private lateinit var sp: SharedPreferences

    /** 0 跟随系统 / 1 浅色 / 2 深色 */
    val themeMode = MutableStateFlow(0)

    /** 卡片按压反馈：0 Sink / 1 Tilt / 2 关闭 */
    val cardFeedback = MutableStateFlow(0)

    /** 减少动效（同时遵循系统动画缩放） */
    val reduceMotion = MutableStateFlow(false)

    /** 冻结 / 停用前确认 */
    val confirmRisky = MutableStateFlow(true)

    /** 权限与 AppOps 修改前确认 */
    val confirmPermission = MutableStateFlow(true)

    /** 批量执行（场景）前确认 */
    val confirmBatch = MutableStateFlow(true)

    /** 后端优先级：0 自动 / 1 优先 Root / 2 优先 Shizuku */
    val backendPreference = MutableStateFlow(0)

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        themeMode.value = sp.getInt("theme_mode", 0)
        // 旧版本只有 tilt_feedback: Boolean，迁移一次后保留新键
        val legacyTilt = sp.getBoolean("tilt_feedback", false)
        cardFeedback.value = sp.getInt("card_feedback", if (legacyTilt) 1 else 0)
        reduceMotion.value = sp.getBoolean("reduce_motion", false)
        confirmRisky.value = sp.getBoolean("confirm_risky", true)
        confirmPermission.value = sp.getBoolean("confirm_permission", true)
        confirmBatch.value = sp.getBoolean("confirm_batch", true)
        backendPreference.value = sp.getInt("backend_preference", 0)
    }

    fun setThemeMode(value: Int) {
        themeMode.value = value
        sp.edit().putInt("theme_mode", value).apply()
    }

    fun setCardFeedback(value: Int) {
        cardFeedback.value = value
        sp.edit().putInt("card_feedback", value).apply()
    }

    fun setReduceMotion(value: Boolean) {
        reduceMotion.value = value
        sp.edit().putBoolean("reduce_motion", value).apply()
    }

    fun setConfirmRisky(value: Boolean) {
        confirmRisky.value = value
        sp.edit().putBoolean("confirm_risky", value).apply()
    }

    fun setConfirmPermission(value: Boolean) {
        confirmPermission.value = value
        sp.edit().putBoolean("confirm_permission", value).apply()
    }

    fun setConfirmBatch(value: Boolean) {
        confirmBatch.value = value
        sp.edit().putBoolean("confirm_batch", value).apply()
    }

    fun setBackendPreference(value: Int) {
        backendPreference.value = value
        sp.edit().putInt("backend_preference", value).apply()
    }

    /** 供备份使用：导出全部设置键值。 */
    fun exportMap(): Map<String, Any?> = mapOf(
        "theme_mode" to themeMode.value,
        "card_feedback" to cardFeedback.value,
        "reduce_motion" to reduceMotion.value,
        "confirm_risky" to confirmRisky.value,
        "confirm_permission" to confirmPermission.value,
        "confirm_batch" to confirmBatch.value,
        "backend_preference" to backendPreference.value,
    )

    fun applyMap(map: Map<String, Any?>) {
        (map["theme_mode"] as? Number)?.let { setThemeMode(it.toInt()) }
        (map["card_feedback"] as? Number)?.let { setCardFeedback(it.toInt()) }
        (map["reduce_motion"] as? Boolean)?.let { setReduceMotion(it) }
        (map["confirm_risky"] as? Boolean)?.let { setConfirmRisky(it) }
        (map["confirm_permission"] as? Boolean)?.let { setConfirmPermission(it) }
        (map["confirm_batch"] as? Boolean)?.let { setConfirmBatch(it) }
        (map["backend_preference"] as? Number)?.let { setBackendPreference(it.toInt()) }
    }
}
