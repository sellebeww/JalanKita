package org.umn.jalankita.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import org.umn.jalankita.di.LocalAppContainer
import org.umn.jalankita.di.appViewModel
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.ui.components.BottomNavigationBar
import org.umn.jalankita.ui.screens.CameraScreen
import org.umn.jalankita.ui.screens.DamageGuideScreen
import org.umn.jalankita.ui.screens.ForgotPasswordDialog
import org.umn.jalankita.ui.screens.ConfirmReportScreen
import org.umn.jalankita.ui.screens.HomeScreen
import org.umn.jalankita.ui.screens.LocationPickerScreen
import org.umn.jalankita.ui.screens.NewReportScreen
import org.umn.jalankita.ui.screens.LoginScreen
import org.umn.jalankita.ui.screens.MapScreen
import org.umn.jalankita.ui.screens.NotificationScreen
import org.umn.jalankita.ui.screens.ProfileScreen
import org.umn.jalankita.ui.screens.RegisterScreen
import org.umn.jalankita.ui.screens.ReportDetailScreen
import org.umn.jalankita.ui.screens.ReporterProfileScreen
import org.umn.jalankita.ui.screens.ReportHistoryScreen
import org.umn.jalankita.ui.screens.ReportSuccessScreen
import org.umn.jalankita.ui.screens.SettingsScreen
import org.umn.jalankita.ui.screens.SplashScreen
import org.umn.jalankita.viewmodel.AuthViewModel
import org.umn.jalankita.viewmodel.HistoryViewModel
import org.umn.jalankita.viewmodel.HomeViewModel
import org.umn.jalankita.viewmodel.NotificationViewModel
import org.umn.jalankita.viewmodel.ProfileViewModel
import org.umn.jalankita.viewmodel.ReportDetailViewModel
import org.umn.jalankita.viewmodel.ReporterViewModel
import org.umn.jalankita.viewmodel.SettingsViewModel
import org.umn.jalankita.viewmodel.ReportViewModel

@Composable
fun AppNavigation(
    pendingReportId: String? = null,
    onPendingReportHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val container = LocalAppContainer.current

    // Auth and Home live above the NavHost: the bottom bar badge and the greeting
    // must stay consistent across tab switches.
    val authViewModel: AuthViewModel = appViewModel {
        AuthViewModel(it.authRepository, it.sessionStore)
    }
    val homeViewModel: HomeViewModel = appViewModel {
        HomeViewModel(
            authRepository = it.authRepository,
            reportRepository = it.reportRepository,
            networkMonitor = it.networkMonitor,
            nearbyAlertMonitor = it.nearbyAlertMonitor,
        )
    }
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

    // A notification tap lands here; wait for the session before deep-linking.
    LaunchedEffect(pendingReportId, authState) {
        val reportId = pendingReportId ?: return@LaunchedEffect
        if (authState !is AuthState.SignedIn) return@LaunchedEffect
        navController.navigate(Routes.reportDetail(reportId))
        onPendingReportHandled()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomBarRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onSelect = navController::switchTab,
                    unreadNotifications = homeState.unreadNotifications,
                )
            }
        },
    ) { innerPadding ->
        val bottomBarHeight = innerPadding.calculateBottomPadding()
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = bottomBarHeight)
                // Tells the per-screen Scaffolds the bottom inset is already
                // handled, so scrollable content is not padded twice.
                .consumeWindowInsets(PaddingValues(bottom = bottomBarHeight)),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize(),
            ) {

                composable(Routes.SPLASH) {
                    SplashScreen(
                        authState = authState,
                        onAuthenticated = { navController.navigateAsRoot(Routes.HOME) },
                        onUnauthenticated = { navController.navigateAsRoot(Routes.LOGIN) },
                    )
                }

                composable(Routes.LOGIN) {
                    val state by authViewModel.login.collectAsStateWithLifecycle()
                    val reset by authViewModel.forgotPassword.collectAsStateWithLifecycle()
                    RedirectWhenSignedIn(authState) { navController.navigateAsRoot(Routes.HOME) }
                    LoginScreen(
                        state = state,
                        isLocalOnly = authViewModel.isLocalOnly,
                        onEmailChange = authViewModel::onLoginEmailChange,
                        onPasswordChange = authViewModel::onLoginPasswordChange,
                        onTogglePasswordVisibility = authViewModel::toggleLoginPasswordVisibility,
                        onRememberMeChange = authViewModel::setRememberMe,
                        onForgotPassword = authViewModel::openForgotPassword,
                        onSubmit = authViewModel::submitLogin,
                        onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    )
                    ForgotPasswordDialog(
                        state = reset,
                        mode = authViewModel.passwordResetMode,
                        onEmailChange = authViewModel::onForgotEmailChange,
                        onPasswordChange = authViewModel::onForgotPasswordChange,
                        onConfirmChange = authViewModel::onForgotConfirmChange,
                        onSubmit = authViewModel::submitForgotPassword,
                        onDismiss = authViewModel::dismissForgotPassword,
                    )
                }

                composable(Routes.REGISTER) {
                    val state by authViewModel.register.collectAsStateWithLifecycle()
                    RedirectWhenSignedIn(authState) { navController.navigateAsRoot(Routes.HOME) }
                    RegisterScreen(
                        state = state,
                        onNameChange = authViewModel::onRegisterNameChange,
                        onEmailChange = authViewModel::onRegisterEmailChange,
                        onPasswordChange = authViewModel::onRegisterPasswordChange,
                        onConfirmPasswordChange = authViewModel::onRegisterConfirmPasswordChange,
                        onTogglePasswordVisibility = authViewModel::toggleRegisterPasswordVisibility,
                        onSubmit = authViewModel::submitRegister,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        state = homeState,
                        isMapsConfigured = container.isMapsConfigured,
                        onNewReport = { navController.navigate(Routes.REPORT_FLOW) },
                        onOpenNotifications = { navController.switchTab(Routes.NOTIFICATIONS) },
                        onOpenMap = { navController.switchTab(Routes.MAP) },
                        onOpenHistory = { navController.switchTab(Routes.HISTORY) },
                        onOpenGuide = { navController.navigate(Routes.DAMAGE_GUIDE) },
                        onSyncNow = container::requestManualSync,
                        onLocationPermissionGranted = homeViewModel::onLocationPermissionGranted,
                        onReportClick = { navController.navigate(Routes.reportDetail(it)) },
                    )
                }

                composable(Routes.MAP) {
                    MapScreen(
                        reports = homeState.communityReports,
                        nearby = homeState.nearby,
                        isMapsConfigured = container.isMapsConfigured,
                        locationProvider = container.locationProvider,
                        onNewReport = { navController.navigate(Routes.REPORT_FLOW) },
                        onReportClick = { navController.navigate(Routes.reportDetail(it)) },
                    )
                }

                composable(Routes.HISTORY) {
                    val historyViewModel: HistoryViewModel = appViewModel {
                        HistoryViewModel(it.authRepository, it.reportRepository, it.networkMonitor)
                    }
                    val state by historyViewModel.uiState.collectAsStateWithLifecycle()
                    ReportHistoryScreen(
                        state = state,
                        onReportClick = { navController.navigate(Routes.reportDetail(it)) },
                        onNewReport = { navController.navigate(Routes.REPORT_FLOW) },
                        onFilterChange = historyViewModel::setFilter,
                        onSyncNow = container::requestManualSync,
                    )
                }

                composable(Routes.NOTIFICATIONS) {
                    val notificationViewModel: NotificationViewModel = appViewModel {
                        NotificationViewModel(it.authRepository, it.reportRepository)
                    }
                    val state by notificationViewModel.uiState.collectAsStateWithLifecycle()
                    NotificationScreen(
                        state = state,
                        onNotificationClick = { notification ->
                            notificationViewModel.markAsRead(notification.id)
                            notification.reportId?.let {
                                navController.navigate(Routes.reportDetail(it))
                            }
                        },
                        onMarkAllRead = notificationViewModel::markAllAsRead,
                    )
                }

                composable(Routes.PROFILE) {
                    val profileViewModel: ProfileViewModel = appViewModel {
                        ProfileViewModel(it.authRepository, it.reportRepository)
                    }
                    val state by profileViewModel.uiState.collectAsStateWithLifecycle()
                    ProfileScreen(
                        state = state,
                        onUpdateName = authViewModel::updateDisplayName,
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenGuide = { navController.navigate(Routes.DAMAGE_GUIDE) },
                        onOpenPublicProfile = {
                            state.user?.id?.let {
                                navController.navigate(Routes.reporterProfile(it))
                            }
                        },
                        onNewReport = { navController.navigate(Routes.REPORT_FLOW) },
                        onSignOut = {
                            authViewModel.signOut()
                            navController.navigateAsRoot(Routes.LOGIN)
                        },
                    )
                }

                composable(Routes.SETTINGS) {
                    val settingsViewModel: SettingsViewModel = appViewModel {
                        SettingsViewModel(
                            it.authRepository,
                            it.reportRepository,
                            it.sessionStore,
                            it.networkMonitor,
                        )
                    }
                    val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
                    SettingsScreen(
                        state = state,
                        onThemeModeChange = settingsViewModel::setThemeMode,
                        onSyncNow = container::requestManualSync,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.DAMAGE_GUIDE) {
                    DamageGuideScreen(
                        onBack = { navController.popBackStack() },
                        onStartReport = { navController.navigate(Routes.REPORT_FLOW) },
                    )
                }

                // Nested graph: one ReportViewModel survives the Camera and
                // Location detours, so the draft is never rebuilt mid-flow.
                navigation(startDestination = Routes.NEW_REPORT, route = Routes.REPORT_FLOW) {

                    composable(Routes.NEW_REPORT) { entry ->
                        val viewModel = reportFlowViewModel(navController, entry)
                        val draft by viewModel.draft.collectAsStateWithLifecycle()
                        NewReportScreen(
                            draft = draft,
                            isMapsConfigured = container.isMapsConfigured,
                            onPermissionResult = viewModel::onPermissionResult,
                            onRetryLocation = viewModel::requestLocation,
                            onPickLocationOnMap = { navController.navigate(Routes.LOCATION_PICKER) },
                            onTakePhoto = { navController.navigate(Routes.CAMERA) },
                            onCategoryChange = viewModel::setCategory,
                            onDescriptionChange = viewModel::setDescription,
                            onOpenGuide = { navController.navigate(Routes.DAMAGE_GUIDE) },
                            onContinue = {
                                if (viewModel.validateForConfirmation()) {
                                    navController.navigate(Routes.CONFIRM_REPORT)
                                }
                            },
                            onBack = {
                                viewModel.discardDraft()
                                navController.popBackStack()
                            },
                        )
                    }

                    composable(Routes.CAMERA) { entry ->
                        val viewModel = reportFlowViewModel(navController, entry)
                        val draft by viewModel.draft.collectAsStateWithLifecycle()
                        CameraScreen(
                            reportId = draft.reportId,
                            photoStorage = viewModel.photoStorage,
                            photoTip = draft.category?.photoTip,
                            onPhotoConfirmed = { path ->
                                viewModel.setPhoto(path)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.LOCATION_PICKER) { entry ->
                        val viewModel = reportFlowViewModel(navController, entry)
                        val draft by viewModel.draft.collectAsStateWithLifecycle()
                        LocationPickerScreen(
                            initialLatitude = draft.location?.latitude,
                            initialLongitude = draft.location?.longitude,
                            isMapsConfigured = container.isMapsConfigured,
                            onConfirm = { latitude, longitude ->
                                viewModel.setManualLocation(latitude, longitude)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.CONFIRM_REPORT) { entry ->
                        val viewModel = reportFlowViewModel(navController, entry)
                        val draft by viewModel.draft.collectAsStateWithLifecycle()
                        val synced by viewModel.submittedSynced.collectAsStateWithLifecycle()

                        LaunchedEffect(synced) {
                            val result = synced ?: return@LaunchedEffect
                            navController.navigate(Routes.reportSuccess(result)) {
                                popUpTo(Routes.REPORT_FLOW) { inclusive = true }
                            }
                        }

                        ConfirmReportScreen(
                            draft = draft,
                            onEdit = { navController.popBackStack() },
                            onSubmit = viewModel::submit,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                composable(
                    route = Routes.REPORT_SUCCESS,
                    arguments = listOf(
                        navArgument(Routes.ARG_SUCCESS_SYNCED) { type = NavType.BoolType },
                    ),
                ) { entry ->
                    ReportSuccessScreen(
                        synced = entry.arguments?.getBoolean(Routes.ARG_SUCCESS_SYNCED) ?: false,
                        onViewReports = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        onBackToHome = {
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                    )
                }

                composable(Routes.REPORT_DETAIL) { entry ->
                    val reportId = entry.arguments?.getString(Routes.ARG_REPORT_ID).orEmpty()
                    val detailViewModel: ReportDetailViewModel = appViewModel(key = reportId) {
                        ReportDetailViewModel(it.reportRepository, reportId, it.networkMonitor)
                    }
                    val state by detailViewModel.uiState.collectAsStateWithLifecycle()
                    ReportDetailScreen(
                        state = state,
                        isMapsConfigured = container.isMapsConfigured,
                        onRetrySync = detailViewModel::retrySync,
                        onReporterClick = { navController.navigate(Routes.reporterProfile(it)) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.REPORTER_PROFILE) { entry ->
                    val reporterId = entry.arguments?.getString(Routes.ARG_REPORTER_ID).orEmpty()
                    val reporterViewModel: ReporterViewModel = appViewModel(key = reporterId) {
                        ReporterViewModel(
                            reportRepository = it.reportRepository,
                            reporterId = reporterId,
                            currentUserId = it.authRepository.currentUser?.id,
                        )
                    }
                    val state by reporterViewModel.uiState.collectAsStateWithLifecycle()
                    ReporterProfileScreen(
                        state = state,
                        onReportClick = { navController.navigate(Routes.reportDetail(it)) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/**
 * Resolves the ReportViewModel owned by the report-flow graph entry, so every
 * step of the flow shares one draft.
 */
@Composable
private fun reportFlowViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): ReportViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.REPORT_FLOW) }
    return appViewModel(owner = parentEntry) { container ->
        ReportViewModel(
            authRepository = container.authRepository,
            reportRepository = container.reportRepository,
            locationProvider = container.locationProvider,
            photoStorage = container.photoStorage,
            networkMonitor = container.networkMonitor,
        )
    }
}

/** Sends the user on as soon as a session exists, without leaving auth screens behind. */
@Composable
private fun RedirectWhenSignedIn(authState: AuthState, onAuthenticated: () -> Unit) {
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) onAuthenticated()
    }
}

/** Replaces the whole back stack — used when crossing the auth boundary. */
fun NavHostController.navigateAsRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Bottom-bar switching.
 *
 * `popUpTo` must stay non-inclusive: popping Home inclusively and then navigating
 * back to it leaves the host without a destination to show.
 */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
