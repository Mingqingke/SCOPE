package com.scope.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** SCOPE 语义色：首页授权卡片、状态色与层级全部集中在这里。 */
@Immutable
data class ScopeColors(
    val dark: Boolean,
    val page: Color,
    val card: Color,
    val mint: Color,
    val mintEdge: Color,
    val green: Color,
    val greenSoft: Color,
    val blue: Color,
    val amber: Color,
    val textSecondary: Color,
    val hairline: Color,
)

val ScopeLight = ScopeColors(
    dark = false,
    page = Color(0xFFF4F4F6),
    card = Color(0xFFFFFFFF),
    mint = Color(0xFFD8F1DE),
    mintEdge = Color(0xFFC4E9CE),
    green = Color(0xFF2F9F68),
    greenSoft = Color(0x3333B57A),
    blue = Color(0xFF3482FF),
    amber = Color(0xFFC98A1B),
    textSecondary = Color(0xFF8A8F98),
    hairline = Color(0xFFECEDF0),
)

val ScopeDark = ScopeColors(
    dark = true,
    page = Color(0xFF0E0F11),
    card = Color(0xFF1A1C1F),
    mint = Color(0xFF162E20),
    mintEdge = Color(0xFF1E3D2B),
    green = Color(0xFF4CC38A),
    greenSoft = Color(0x334CC38A),
    blue = Color(0xFF5B9BFF),
    amber = Color(0xFFE0B25C),
    textSecondary = Color(0xFF8E939B),
    hairline = Color(0xFF26292D),
)

val LocalScope = staticCompositionLocalOf { ScopeLight }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3482FF),
    background = Color(0xFFF4F4F6),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFF4F4F6),
    onSurface = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFFFFFFFF),
    onSurfaceContainer = Color(0xFF0A0A0A),
    onSurfaceContainerVariant = Color(0xFF8A8F98),
    onSurfaceVariantSummary = Color(0x99000000),
    outline = Color(0xFFE3E4E8),
    dividerLine = Color(0xFFEDEEF1),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    background = Color(0xFF0E0F11),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0E0F11),
    onSurface = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFF1A1C1F),
    onSurfaceContainer = Color(0xFFEDEDED),
    onSurfaceContainerVariant = Color(0xFF8E939B),
    onSurfaceVariantSummary = Color(0x99FFFFFF),
    outline = Color(0xFF2A2D31),
    dividerLine = Color(0xFF26292D),
)

@Composable
fun ScopeTheme(mode: Int, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // 状态栏 / 系统导航栏图标明暗跟随真实主题，不写死厂商颜色
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalScope provides if (dark) ScopeDark else ScopeLight) {
        MiuixTheme(colors = if (dark) DarkScheme else LightScheme) {
            content()
        }
    }
}
