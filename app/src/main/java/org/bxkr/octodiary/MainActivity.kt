package org.bxkr.octodiary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import java.time.LocalTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.graphics.scale
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.bxkr.octodiary.components.DebugMenu
import org.bxkr.octodiary.components.MigrationDialog
import org.bxkr.octodiary.components.ProfileChooser
import org.bxkr.octodiary.components.SettingsDialog
import org.bxkr.octodiary.components.TokenLogin
import org.bxkr.octodiary.components.settings.About
import org.bxkr.octodiary.screens.CallbackScreen
import org.bxkr.octodiary.screens.CallbackType
import org.bxkr.octodiary.screens.LoginScreen
import org.bxkr.octodiary.screens.NavScreen
import org.bxkr.octodiary.utils.PerformanceMonitor
import org.bxkr.octodiary.screens.navsections.daybook.DayChooser

import org.bxkr.octodiary.services.McpServerService
import org.bxkr.octodiary.ui.theme.CustomColorScheme
import org.bxkr.octodiary.ui.theme.OctoDiaryTheme
import java.io.ByteArrayOutputStream
import java.io.File

val modalBottomSheetStateLive = MutableStateFlow(false)
val modalBottomSheetContentLive = MutableStateFlow<(@Composable () -> Unit)?>(null)
val snackbarHostStateLive = MutableStateFlow(SnackbarHostState())
val navControllerLive = MutableStateFlow<NavHostController?>(null)
val showFilterLive = MutableStateFlow(false)
val contentDependentActionLive = MutableStateFlow<(@Composable () -> Unit)?>(null)
val contentDependentActionIconLive = MutableStateFlow(Icons.Rounded.FilterAlt)
val screenLive = MutableStateFlow<Screen>(Screen.Login)
val modalDialogStateLive = MutableStateFlow(false)
val modalDialogContentLive = MutableStateFlow<(@Composable () -> Unit)?>(null)
val modalDialogCloseListenerLive = MutableStateFlow<(() -> Unit)?>(null)
val reloadEverythingLive = MutableStateFlow<(() -> Unit)?>(null)
val darkThemeLive = MutableStateFlow<Boolean?>(null)
val colorSchemeLive = MutableStateFlow(-1)
val launchUrlLive = MutableStateFlow<Uri?>(null)
val launchPickerLive = MutableStateFlow<(() -> Unit)?>(null)
val LocalActivity = staticCompositionLocalOf<FragmentActivity> {
    error("No LocalActivity provided!")
}

class MainActivity : FragmentActivity() {
    private fun createNotificationChannel() {
        val name = getString(R.string.data_update_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("data_update", name, importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обработка изменения ориентации экрана
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Временный отладочный Toast для проверки запуска Main Activity
        android.widget.Toast.makeText(
            this,
            "MainActivity.onCreate() - Debug Check",
            android.widget.Toast.LENGTH_LONG
        ).show()
        createNotificationChannel()

        val currentLocale = resources.configuration.locales[0]
        Log.d("LocaleDebug", "Current application locale: $currentLocale")

        // Применяем сохранённый масштаб текста
        val textScale = mainPrefs.get<Float>("text_scale") ?: 1.0f
        val config = resources.configuration
        config.fontScale = textScale
        resources.updateConfiguration(config, resources.displayMetrics)

        // Инициализируем мониторинг производительности
        PerformanceMonitor.initialize(this)

        // Запускаем Battery Monitor
        org.bxkr.octodiary.utils.BatteryMonitor(this).startMonitoring { isActive ->
            // Battery saver активирован/деактивирован
            if (isActive) {
                // Очищаем кэш при активации battery saver
                DataService.clearCacheIfLowMemory(this)
            }
        }
        
        // Запускаем Bell Schedule Worker
        org.bxkr.octodiary.workers.BellScheduleWorker.scheduleNotifications(this)

        // Запускаем MCP Server Service
        startService(Intent(this, McpServerService::class.java))

        // Запускаем автоматическую запись уроков если включена в настройках
        val aiPrefs = getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        if (aiPrefs.getBoolean("auto_record_lectures", false)) {
            org.bxkr.octodiary.audio.AutomaticLectureRecordingService.startAutomaticRecording(this)
        }

        // Запускаем сервис автообновления данных, если включено в настройках
        if (mainPrefs.get<Boolean>("auto_update_enabled") != false) {
            startService(Intent(this, AutoUpdateService::class.java))
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }
// Picker removed for debugging


        setContent {
            val colorScheme by colorSchemeLive.collectAsState(-1)
            val darkTheme by darkThemeLive.collectAsState(isSystemInDarkTheme())
            /**
             * Когда `colorScheme == -1`, использует динамические цвета, **если доступны**.
             * Если нет, использует по умолчанию (жёлтый).
             **/
            val themeStartTime = System.currentTimeMillis()
            val animationStartTime = System.currentTimeMillis()
            // Оптимизация: используем remember для избежания лишних рекомпозиций
            val systemDark = isSystemInDarkTheme()
            val currentThemeState = remember(darkTheme, colorScheme, systemDark) {
                (darkTheme ?: systemDark) to colorScheme
            }

            AnimatedContent(
                targetState = currentThemeState,
                label = "theme_anim",
                transitionSpec = { androidx.compose.animation.ContentTransform(
                    targetContentEnter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(150)),
                    initialContentExit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))
                ) }
            ) {
                val currentScheme = remember(it.second) {
                    when {
                        it.second == -1 -> CustomColorScheme.Yellow
                        else -> CustomColorScheme.values()[it.second]
                    }
                }
                val amoledTheme = mainPrefs.get<Boolean>("amoled_theme") ?: false
                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "Theme selection completed in ${System.currentTimeMillis() - themeStartTime}ms")
                    Log.d("Performance", "Theme animation transition time: ${System.currentTimeMillis() - animationStartTime}ms")
                    LaunchedEffect(Unit) {
                        Log.d("Performance", "Theme AnimatedContent recomposed")
                    }
                }
                // Оптимизация: проверяем влияние тем на производительность
                val themeApplicationStart = System.currentTimeMillis()
                val themeKey = "${it.first}_${it.second}_$amoledTheme"
                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "Applying theme with key: $themeKey")
                }
                OctoDiaryTheme(
                    it.first,
                    colorScheme == -1,
                    amoledTheme,
                    currentScheme.lightColorScheme,
                    currentScheme.darkColorScheme
                ) {
                    if (BuildConfig.DEBUG) {
                        Log.d("Performance", "Theme application completed in ${System.currentTimeMillis() - themeApplicationStart}ms")
                    }
                    // Мониторим производительность MyApp
                    PerformanceMonitor.TrackComposablePerformance("MyApp")
                    MyApp(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun MyApp(
        modifier: Modifier = Modifier,
    ) {
        val startTime = System.currentTimeMillis()
        val recomposeScope = currentRecomposeScope
        if (BuildConfig.DEBUG) {
            Log.d("Performance", "MyApp composition started - recompose scope: ${recomposeScope.hashCode()}")
        }

        var title by rememberSaveable { mutableIntStateOf(R.string.app_name) }
        screenLive.value = if (authPrefs.get<Boolean>("auth") == true) {
            Screen.MainNav
        } else Screen.Login
        val pinFinished = remember { mutableStateOf(false) }
        val currentScreen by screenLive.collectAsState()
        val showBottomSheet by modalBottomSheetStateLive.collectAsState()
        val bottomSheetContent by modalBottomSheetContentLive.collectAsState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val snackbarHostState = snackbarHostStateLive.value!!
        if (navControllerLive.value == null) {
            navControllerLive.value = rememberNavController()
        }
        val navController by navControllerLive.collectAsState()
        val surfaceColor = MaterialTheme.colorScheme.surface
        val elevatedColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        // Оптимизация: используем derivedStateOf для topAppBarColor вместо прямого изменения
        val navBackStackEntry by navController?.currentBackStackEntryAsState() ?: mutableStateOf(null)
        val currentRoute by derivedStateOf { navBackStackEntry?.destination?.route }
        val topAppBarColor by derivedStateOf { if (currentRoute == NavSection.Daybook.route) elevatedColor else surfaceColor }
        val contentDependentAction by contentDependentActionLive.collectAsState()
        val showDialog by modalDialogStateLive.collectAsState()
        val dialogContent by modalDialogContentLive.collectAsState()
        val showFilter by showFilterLive.collectAsState(false)
        val launchUrl by launchUrlLive.collectAsState()

        if (BuildConfig.DEBUG) {
            Log.d("Performance", "MyApp basic setup completed in ${System.currentTimeMillis() - startTime}ms")
        }
        if (authPrefs.get<String>("access_token") != null) {
            if ((mainPrefs.get<Int>("version") ?: 25) <= 25) {
                modalDialogCloseListenerLive.value = {
                    mainPrefs.save("version" to BuildConfig.VERSION_CODE)
                    logOut("Migration from version 25")
                }
                modalDialogContentLive.value = {
                    MigrationDialog { modalDialogCloseListenerLive.value?.invoke() }
                }
                modalDialogStateLive.value = true
            } else if ((mainPrefs.get<Int>("version") ?: 31) <= 31) {
                logOut("Migration from version 31")
            } else if (mainPrefs.get<Int>("version") != BuildConfig.VERSION_CODE) {
                mainPrefs.save("version" to BuildConfig.VERSION_CODE)
            }
        } else if (mainPrefs.get<Int>("version") != BuildConfig.VERSION_CODE) {
            mainPrefs.save("version" to BuildConfig.VERSION_CODE)
        }

        if (launchUrl != null) {
            val tabIntent = CustomTabsIntent.Builder().build()
            tabIntent.launchUrl(LocalContext.current, launchUrl!!)
            launchUrlLive.value = null
        }

        // Оптимизация: убираем ненужные логи в продакшене
        if (BuildConfig.DEBUG) {
            Log.d("Performance", "Optimized navigation listener removed - using derivedStateOf instead")
        }

        // var localLoadedState от remember { mutableStateOf(false) }
        var settingsShown by remember { mutableStateOf(false) }

        val intentData = intent.dataString
        
        if (intentData != null) {
            val uri = Uri.parse(intentData)
            if (uri.scheme == "octodiary" && uri.host == "debug") {
                intent.setData(null)
                val path = uri.path
                LaunchedEffect(Unit) {
                    when (path) {
                        "/settings" -> settingsShown = true
                        "/daybook" -> navController?.navigate(NavSection.Daybook.route)
                        "/homeworks" -> navController?.navigate(NavSection.Homeworks.route)
                        "/dashboard" -> navController?.navigate(NavSection.Dashboard.route)
                        "/marks" -> navController?.navigate(NavSection.Marks.route)
                        "/profile" -> navController?.navigate(NavSection.Profile.route)
                    }
                }
            } else {
                intent.setData(null)
                if (authPrefs.get<Boolean>("auth") == true) {
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar(getString(R.string.already_auth))
                    }
                } else {
                    screenLive.value = Screen.Callback
                }
            }
        }

        CompositionLocalProvider(LocalActivity provides this) {
            Scaffold(modifier, topBar = {
                Column {
                    val topAppBarStartTime = System.currentTimeMillis()
                    TopAppBar(title = {
                        val titleAnimStart = System.currentTimeMillis()
                        val titleAnimationStart = System.currentTimeMillis()
                        AnimatedContent(targetState = title, label = "title_anim") {
                            if ((currentScreen == Screen.MainNav && DataService.loadedEverything.value) || currentScreen != Screen.MainNav) {
                                Text(stringResource(it))
                            } else {
                                Text(stringResource(R.string.app_name))
                            }
                            if (BuildConfig.DEBUG) {
                                Log.d("Performance", "Title AnimatedContent animation time: ${System.currentTimeMillis() - titleAnimationStart}ms")
                                LaunchedEffect(Unit) {
                                    Log.d("Performance", "Title AnimatedContent recomposed")
                                }
                            }
                        }
                        Log.d("Performance", "Title animation completed in ${System.currentTimeMillis() - titleAnimStart}ms")
                    }, actions = {
                        if (mainPrefs.get<Boolean>("force_debug") == true) {
                            DebugMenu(this@MainActivity)
                        }
                        if (DataService.loadedEverything.value && currentScreen == Screen.MainNav) {
                            val currentRoute =
                                navController!!.currentBackStackEntryAsState().value?.destination?.route
                            Row(Modifier) {
                                AnimatedVisibility(currentRoute == NavSection.Profile.route) {
                                    IconButton(onClick = {
                                        modalDialogStateLive.value = true
                                    }) {
                                        Icon(
                                            Icons.Rounded.Groups,
                                            stringResource(id = R.string.choose_context_profile)
                                        )
                                    }
                                }
                                IconButton(onClick = { settingsShown = true }) {
                                    Icon(
                                        Icons.Rounded.Settings,
                                        stringResource(id = R.string.settings)
                                    )
                                }
                            }
                            AnimatedVisibility(currentRoute == NavSection.Daybook.route) {
                                Row {
                                    IconButton(onClick = {
                                        modalDialogContentLive.value = { ProfileChooser() }
                                        modalDialogStateLive.value = true
                                    }) {
                                        Icon(
                                            Icons.Rounded.CalendarMonth,
                                            stringResource(id = R.string.by_date)
                                        )
                                    }
                                }
                            }
                            AnimatedVisibility(showFilter) {
                                var expanded by remember {
                                    mutableStateOf(false)
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    val icon by contentDependentActionIconLive.collectAsState(Icons.Rounded.FilterAlt)
                                    IconButton(onClick = { expanded = !expanded }) {
                                        val actionIconAnimationStart = System.currentTimeMillis()
                                        AnimatedContent(
                                            targetState = icon,
                                            label = "action_icon_anim"
                                        ) {
                                            Icon(it, "action")
                                            if (BuildConfig.DEBUG) {
                                                Log.d("Performance", "Action icon animation time: ${System.currentTimeMillis() - actionIconAnimationStart}ms")
                                            }
                                        }
                                    }
                                    DropdownMenu(expanded, { expanded = false }) {
                                        contentDependentAction?.invoke()
                                    }

                                }
                            }
                        } else if (currentScreen == Screen.Login) {
                            var expanded by remember { mutableStateOf(false) }
                            var showAboutDialog by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    stringResource(R.string.menu)
                                )
                            }
                            DropdownMenu(expanded, { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.log_in_by_token)) },
                                    onClick = {
                                        modalDialogContentLive.value = { TokenLogin() }
                                        modalDialogStateLive.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.about)) },
                                    onClick = { showAboutDialog = true })
                            }
                            AnimatedVisibility(showAboutDialog) {
                                Dialog(
                                    onDismissRequest = { showAboutDialog = false },
                                    properties = DialogProperties(
                                        usePlatformDefaultWidth = false
                                    )
                                ) {
                                    Surface(Modifier.fillMaxSize()) {
                                        Column(Modifier.verticalScroll(rememberScrollState())) {
                                            IconButton(
                                                onClick = { showAboutDialog = false },
                                                Modifier.padding(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                                    stringResource(R.string.back)
                                                )
                                            }
                                            About()
                                        }
                                    }
                                }
                            }
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topAppBarColor
                    )
                )
                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "TopAppBar rendering completed in ${System.currentTimeMillis() - topAppBarStartTime}ms")
                }
            }
        }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, bottomBar = {
                if ((currentScreen != Screen.MainNav) || !DataService.loadedEverything.value) return@Scaffold
                val navBarStartTime = System.currentTimeMillis()
                NavigationBar {
                    val navBackStackEntry by navController!!.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    Log.d("Performance", "NavigationBar rendering started")
                    NavSection.values().forEach {
                        val selected =
                            currentDestination?.hierarchy?.any { destination -> destination.route == it.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val clickStart = System.currentTimeMillis()
                                if (it == NavSection.Homeworks) {
                                    showFilterLive.value = true
                                } else {
                                    showFilterLive.value = false
                                }
                                val navigationStart = System.currentTimeMillis()
                                navController!!.navigate(it.route)
                                if (BuildConfig.DEBUG) {
                                    Log.d("Performance", "Navigation to ${it.route} completed in ${System.currentTimeMillis() - clickStart}ms - total nav time: ${System.currentTimeMillis() - navigationStart}ms")
                                }
                            },
                            icon = {
                                Icon(it.icon, stringResource(id = it.title))
                            },
                            label = {
                                Text(stringResource(id = it.title))
                            })
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "NavigationBar rendering completed in ${System.currentTimeMillis() - navBarStartTime}ms")
                }
            }) { padding ->
                Surface {
                    title = when (currentScreen!!) {
                        Screen.Login -> {
                            LoginScreen(Modifier.padding(padding))
                            R.string.log_in
                        }

                        Screen.Callback -> {
                            val uri = Uri.parse(intentData)
                            val callbackType =
                                CallbackType.values().firstOrNull { it.host == uri.host }
                            val code = uri.getQueryParameter("code")
                            val subsystem = uri.getQueryParameter("system")?.toIntOrNull()
                            if (code != null && callbackType != null) {
                                CallbackScreen(code, callbackType, subsystem)
                            } else {
                                screenLive.value = Screen.Login
                            }
                            R.string.log_in
                        }

                        Screen.MainNav -> {
                            val screenStartTime = System.currentTimeMillis()
                            val result = NavScreen(Modifier.padding(padding), pinFinished)
                            if (BuildConfig.DEBUG) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("Performance", "NavScreen rendered in ${System.currentTimeMillis() - screenStartTime}ms")
                                }
                            }
                            val navBackStackEntry = navController!!.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry.value?.destination?.route
                            NavSection.values().firstOrNull { it.route == currentRoute }?.title ?: R.string.app_name
                        }
                        else -> {
                            val screenStartTime = System.currentTimeMillis()
                            val result = NavScreen(Modifier.padding(padding), pinFinished)
                            Log.d("Performance", "NavScreen rendered in ${System.currentTimeMillis() - screenStartTime}ms")
                            val navBackStackEntry = navController!!.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry.value?.destination?.route
                            NavSection.values().firstOrNull { it.route == currentRoute }?.title ?: R.string.app_name
                        }
                    }
                }
                if (showBottomSheet == true) {
                    ModalBottomSheet(
                        onDismissRequest = { modalBottomSheetStateLive.value = false },
                        sheetState = sheetState
                    ) {
                        bottomSheetContent?.invoke()
                    }
                }
                if (showDialog == true) {
                    Dialog(onDismissRequest = {
                        modalDialogStateLive.value = false
                        modalDialogCloseListenerLive.value?.invoke()
                        modalDialogCloseListenerLive.value = {}
                    }) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            dialogContent?.invoke()
                        }
                    }
                }
                AnimatedVisibility(visible = settingsShown) {
                    SettingsDialog { settingsShown = false }
                }

                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "MyApp composition completed in ${System.currentTimeMillis() - startTime}ms")
                    LaunchedEffect(Unit) {
                        Log.d("Performance", "MyApp recomposed")
                    }
                }
            }
        }
    }

    @Preview(
        name = "Not Night", locale = "ru"
    )
    @Preview(
        name = "Night", uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "ru"
    )
    @Composable
    fun AppPreview() {
        OctoDiaryTheme {
            MyApp()
        }
    }
}
