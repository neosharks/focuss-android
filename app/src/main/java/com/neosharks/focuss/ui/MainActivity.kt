package com.neosharks.focuss.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.neosharks.focuss.data.ScheduleLogic
import com.neosharks.focuss.ui.screens.AppSelectionScreen
import com.neosharks.focuss.ui.screens.HomeScreen
import com.neosharks.focuss.ui.screens.PermissionsScreen
import com.neosharks.focuss.ui.screens.ScheduleEditorScreen
import com.neosharks.focuss.ui.screens.SettingsScreen
import com.neosharks.focuss.ui.screens.SplashScreen
import com.neosharks.focuss.ui.theme.FocussTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.neosharks.focuss.data.LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationsIfNeeded()

        setContent {
            val vm: FocussViewModel = viewModel()
            val state by vm.state.collectAsState()

            FocussTheme(themeMode = state.themeMode) {
                // The Surface fills the whole window (incl. behind the status/nav
                // bars) so the themed background extends edge-to-edge; content is
                // inset separately.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                        FocussApp(vm)
                    }
                }
            }
        }
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun FocussApp(vm: FocussViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Refresh permissions / service state whenever we return to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
            },
        )
    }

    // Light periodic refresh so a schedule that becomes active is reflected live.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            vm.refresh()
        }
    }

    fun lockedToast() =
        Toast.makeText(context, context.getString(com.neosharks.focuss.R.string.session_locked_toast), Toast.LENGTH_SHORT).show()

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onReady = {
                    val dest = if (vm.state.value.allPermissionsGranted) "home" else "permissions"
                    nav.navigate(dest) { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable("permissions") {
            // Auto-advance once everything is granted.
            LaunchedEffect(state.allPermissionsGranted) {
                if (state.allPermissionsGranted) {
                    nav.navigate("home") { popUpTo("permissions") { inclusive = true } }
                }
            }
            PermissionsScreen(
                state = state,
                onRequestUsage = vm::requestUsage,
                onRequestOverlay = vm::requestOverlay,
                onRequestAccessibility = vm::requestAccessibility,
                onRequestBattery = vm::requestBatteryExemption,
                onContinue = {
                    nav.navigate("home") { popUpTo("permissions") { inclusive = true } }
                },
            )
        }

        composable("home") {
            HomeScreen(
                state = state,
                onOpenSettings = { nav.navigate("settings") },
                onFixService = vm::requestAccessibility,
                onStartProtection = vm::startProtection,
                onStopProtection = vm::stopProtection,
                onToggleSchedule = vm::toggleSchedule,
                onAddSchedule = {
                    // Allowed even during an active session; starts on a free, non-overlapping slot.
                    val fresh = ScheduleLogic.nonConflictingDefault(vm.state.value.schedules)
                    vm.upsertSchedule(fresh)
                    nav.navigate("editor/${fresh.id}")
                },
                onOpenSchedule = { id -> nav.navigate("editor/$id") },
                onLockedAttempt = { lockedToast() },
            )
        }

        composable("settings") {
            SettingsScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onCycleTheme = vm::cycleTheme,
                onSetLanguage = vm::setLanguage,
                onRequestBattery = vm::requestBatteryExemption,
                strictModeEnableIntent = vm::strictModeEnableIntent,
                onStrictModeResult = vm::refresh,
                onDisableStrict = vm::disableStrictMode,
                onReviewPermissions = { nav.navigate("permissions") },
            )
        }

        composable(
            "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val schedule = vm.scheduleById(id)
            if (schedule == null) {
                // Safety net for an unknown/already-removed id. Guard on the entry's
                // lifecycle so this doesn't fire a *second* pop while the editor is
                // already being popped after a delete — that would pop "home" too and
                // leave the NavHost empty (blank screen).
                LaunchedEffect(Unit) {
                    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        nav.popBackStack()
                    }
                }
            } else {
                ScheduleEditorScreen(
                    initial = schedule,
                    others = state.schedules,
                    onChange = vm::upsertSchedule,
                    onEditApps = { sid -> nav.navigate("apps/$sid") },
                    onDelete = { sid ->
                        vm.deleteSchedule(sid)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(
            "apps/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            LaunchedEffect(Unit) { vm.loadInstalledApps() }
            val schedule = vm.scheduleById(id)
            AppSelectionScreen(
                state = state,
                initiallySelected = schedule?.blockedApps ?: emptySet(),
                loadIcon = vm::loadIcon,
                onBack = { nav.popBackStack() },
                onSave = { picked ->
                    schedule?.let { vm.upsertSchedule(it.copy(blockedApps = picked)) }
                    nav.popBackStack()
                },
            )
        }
    }
}
