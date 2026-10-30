package com.scope.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scope.app.core.AppRepo
import com.scope.app.core.Backend
import com.scope.app.core.Device
import com.scope.app.core.DeviceSnapshot
import com.scope.app.core.Prefs
import com.scope.app.ui.screens.AboutScopeScreen
import com.scope.app.ui.screens.AppDetailScreen
import com.scope.app.ui.screens.AppOpsScreen
import com.scope.app.ui.screens.AppPickerScreen
import com.scope.app.ui.screens.AppsScreen
import com.scope.app.ui.screens.AuthScreen
import com.scope.app.ui.screens.CapabilitiesScreen
import com.scope.app.ui.screens.ComponentsScreen
import com.scope.app.ui.screens.DeviceInfoScreen
import com.scope.app.ui.screens.DiagnosticsScreen
import com.scope.app.ui.screens.HistoryScreen
import com.scope.app.ui.screens.HomeScreen
import com.scope.app.ui.screens.LicensesScreen
import com.scope.app.ui.screens.ManageScreen
import com.scope.app.ui.screens.PermissionCenterScreen
import com.scope.app.ui.screens.PermissionsScreen
import com.scope.app.ui.screens.ScenarioEditScreen
import com.scope.app.ui.screens.ScenarioScreen
import com.scope.app.ui.screens.SettingsAboutDeviceScreen
import com.scope.app.ui.screens.SettingsAppearanceScreen
import com.scope.app.ui.screens.SettingsDataScreen
import com.scope.app.ui.screens.SettingsHomeScreen
import com.scope.app.ui.screens.SettingsSafetyScreen
import com.scope.app.ui.screens.SettingsSystemScreen
import com.scope.app.ui.screens.SpecialAccessScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavController
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Composable
fun ScopeRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val status by Backend.status.collectAsState()
    val reduceMotion by Prefs.reduceMotion.collectAsState()
    var device by remember { mutableStateOf(DeviceSnapshot()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var updateInfo by remember { mutableStateOf<com.scope.app.core.UpdateInfo?>(null) }

    // 每个主入口一条独立返回栈：切换标签不丢失各自页面
    val homeNav = rememberNavController<Route>(Route.Home)
    val appsNav = rememberNavController<Route>(Route.Apps)
    val manageNav = rememberNavController<Route>(Route.Manage)
    val settingsNav = rememberNavController<Route>(Route.Settings)
    val activeNav: NavController = when (tab) {
        1 -> appsNav
        2 -> manageNav
        3 -> settingsNav
        else -> homeNav
    }
    val atTabRoot = activeNav.backStack.size == 1

    suspend fun reloadDevice() {
        device = withContext(Dispatchers.IO) { Device.snapshot(context) }
    }

    LaunchedEffect(Unit) {
        reloadDevice()
        withContext(Dispatchers.IO) {
            Backend.refresh(context)
            AppRepo.load(context)
        }
        // 启动时静默检查更新，有可用版本才弹出
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            com.scope.app.core.Updater.check()
        }.let { result ->
            if (result is com.scope.app.core.UpdateCheck.Available) updateInfo = result.info
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    // 从系统授权页返回后立刻重新读取真实状态
                    Backend.refresh(context)
                    reloadDevice()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun notify(message: String) {
        if (message.isBlank()) return
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun refreshAll() {
        scope.launch {
            Backend.refresh(context)
            withContext(Dispatchers.IO) { AppRepo.load(context, force = true) }
            reloadDevice()
        }
    }

    // 非首页标签根页面：返回先回首页；首页根页面不拦截，交给系统退出
    BackHandler(enabled = atTabRoot && tab != 0) {
        tab = 0
    }

    val systemAnimator = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }
    val transition = if (reduceMotion || systemAnimator == 0f) NavTransitions.None else NavTransitions.MiuixDefault

    Scaffold(
        bottomBar = {
            if (atTabRoot) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = MiuixIcons.Home,
                        label = "首页",
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = MiuixIcons.GridView,
                        label = "应用",
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = MiuixIcons.Tune,
                        label = "管理",
                    )
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = MiuixIcons.Settings,
                        label = "设置",
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            updateInfo?.let { info ->
                com.scope.app.ui.screens.UpdateSheet(
                    info = info,
                    onDismiss = { updateInfo = null },
                    notify = ::notify,
                )
            }

            NavDisplay(
                navController = activeNav,
                modifier = Modifier.fillMaxSize(),
                transition = transition,
            ) {
                entry<Route.Home> {
                    HomeScreen(
                        status = status,
                        device = device,
                        onOpenAuth = { activeNav.push(Route.Auth) },
                        onOpenCapabilities = { activeNav.push(Route.Capabilities) },
                        onOpenScenarios = { activeNav.push(Route.Scenarios) },
                        onRefresh = { refreshAll() },
                    )
                }
                entry<Route.Apps> {
                    AppsScreen(onOpenApp = { pkg, label -> activeNav.push(Route.AppDetail(pkg, label)) })
                }
                entry<Route.Manage> {
                    ManageScreen(status = status, onOpen = { activeNav.push(it) })
                }
                entry<Route.Settings> {
                    SettingsHomeScreen(onOpen = { activeNav.push(it) })
                }

                entry<Route.SettingsSystem> {
                    SettingsSystemScreen(
                        status = status,
                        device = device,
                        onBack = { activeNav.pop() },
                        onOpen = { activeNav.push(it) },
                        onRefresh = { refreshAll() },
                        notify = ::notify,
                    )
                }
                entry<Route.SettingsAppearance> {
                    SettingsAppearanceScreen(onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.SettingsSafety> {
                    SettingsSafetyScreen(onBack = { activeNav.pop() })
                }
                entry<Route.SettingsData> {
                    SettingsDataScreen(
                        onBack = { activeNav.pop() },
                        onOpen = { activeNav.push(it) },
                        notify = ::notify,
                    )
                }
                entry<Route.SettingsAboutDevice> {
                    SettingsAboutDeviceScreen(onBack = { activeNav.pop() }, onOpen = { activeNav.push(it) })
                }
                entry<Route.DeviceInfo> {
                    DeviceInfoScreen(device = device, status = status, onBack = { activeNav.pop() })
                }
                entry<Route.About> {
                    AboutScopeScreen(device = device, onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.Licenses> {
                    LicensesScreen(onBack = { activeNav.pop() })
                }

                entry<Route.Auth> {
                    AuthScreen(
                        status = status,
                        onBack = { activeNav.pop() },
                        onRefresh = { refreshAll() },
                        notify = ::notify,
                    )
                }
                entry<Route.Capabilities> {
                    CapabilitiesScreen(status, device, onBack = { activeNav.pop() })
                }
                entry<Route.Scenarios> {
                    ScenarioScreen(
                        onBack = { activeNav.pop() },
                        onOpenEdit = { activeNav.push(Route.ScenarioEdit(it)) },
                        notify = ::notify,
                    )
                }
                entry<Route.ScenarioEdit> { route ->
                    ScenarioEditScreen(
                        scenarioId = route.id,
                        onBack = { activeNav.pop() },
                        notify = ::notify,
                    )
                }
                entry<Route.History> {
                    HistoryScreen(onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.Diagnostics> {
                    DiagnosticsScreen(status, device, onBack = { activeNav.pop() })
                }
                entry<Route.PermissionCenter> {
                    PermissionCenterScreen(
                        onBack = { activeNav.pop() },
                        onOpen = { activeNav.push(it) },
                        notify = ::notify,
                    )
                }

                entry<Route.AppDetail> { route ->
                    AppDetailScreen(
                        pkg = route.pkg,
                        fallbackLabel = route.label,
                        onBack = { activeNav.pop() },
                        onOpenPermissions = { activeNav.push(Route.Permissions(route.pkg, route.label)) },
                        onOpenAppOps = { activeNav.push(Route.AppOps(route.pkg, route.label)) },
                        onOpenComponents = { activeNav.push(Route.Components(route.pkg, route.label)) },
                        onOpenSpecial = { activeNav.push(Route.SpecialAccess(route.pkg, route.label)) },
                        notify = ::notify,
                    )
                }
                entry<Route.Permissions> { route ->
                    PermissionsScreen(route.pkg, route.label, onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.AppOps> { route ->
                    AppOpsScreen(route.pkg, route.label, onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.Components> { route ->
                    ComponentsScreen(route.pkg, route.label, onBack = { activeNav.pop() }, notify = ::notify)
                }
                entry<Route.SpecialAccess> { route ->
                    SpecialAccessScreen(route.pkg, route.label, onBack = { activeNav.pop() }, onOpenAuth = { activeNav.push(Route.Auth) }, notify = ::notify)
                }
                entry<Route.Picker> { route ->
                    AppPickerScreen(
                        title = route.title,
                        subtitle = route.subtitle,
                        onBack = { activeNav.pop() },
                        onPick = { pkg, label ->
                            when (route.target) {
                                PickerTarget.APPOPS -> activeNav.push(Route.AppOps(pkg, label))
                                PickerTarget.COMPONENTS -> activeNav.push(Route.Components(pkg, label))
                                PickerTarget.SPECIAL -> activeNav.push(Route.SpecialAccess(pkg, label))
                                PickerTarget.PERMISSIONS -> activeNav.push(Route.Permissions(pkg, label))
                            }
                        },
                    )
                }
            }
        }
    }
}
