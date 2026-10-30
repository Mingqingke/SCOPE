package com.scope.app

import android.app.Application
import com.scope.app.core.History
import com.scope.app.core.Prefs
import com.scope.app.core.ScenarioStore
import com.topjohnwu.superuser.Shell

class ScopeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        History.init(this)
        ScenarioStore.init(this)
        // libsu：统一超时，避免 Root 检测长时间挂起
        runCatching { Shell.setDefaultBuilder(Shell.Builder.create().setTimeout(20)) }
    }
}
