package com.scope.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.scope.app.core.Prefs
import com.scope.app.ui.ScopeRoot
import com.scope.app.ui.theme.ScopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 起强制 Edge-to-Edge：由系统栏 Insets 驱动布局，不写死颜色
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val mode by Prefs.themeMode.collectAsState()
            ScopeTheme(mode = mode) {
                ScopeRoot()
            }
        }
    }
}
