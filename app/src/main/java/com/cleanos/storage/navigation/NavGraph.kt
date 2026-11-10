package com.cleanos.storage.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cleanos.storage.CleanOSApp
import com.cleanos.storage.features.categories.CategorySelectionScreen
import com.cleanos.storage.features.dashboard.DashboardScreen
import com.cleanos.storage.features.deletequeue.DeleteQueueScreen
import com.cleanos.storage.features.onboarding.PermissionScreen
import com.cleanos.storage.features.scanner.ScannerViewModel
import com.cleanos.storage.features.scanner.ScanningScreen
import com.cleanos.storage.features.swipecleaner.KeepListScreen
import com.cleanos.storage.features.swipecleaner.SwipeCleanerScreen
import com.cleanos.storage.features.swipecleaner.SwipeViewModel
import androidx.compose.ui.platform.LocalContext

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val CATEGORIES = "categories"
    const val SCANNING = "scanning"
    const val SWIPE = "swipe"
    const val QUEUE = "queue"
    const val KEEP = "keep"
}

@Composable
fun CleanOSNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as CleanOSApp
    val start = if (app.repository.prefs.onboardingDone) Routes.DASHBOARD else Routes.ONBOARDING
    // Shared view models across the scan -> review -> queue flow.
    val scannerVm: ScannerViewModel = viewModel()
    val swipeVm: SwipeViewModel = viewModel()
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            PermissionScreen(onDone = {
                nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onStartScan = { nav.navigate(Routes.CATEGORIES) },
                onOpenSwipe = { nav.navigate(Routes.SWIPE) },
                onOpenQueue = { nav.navigate(Routes.QUEUE) },
                onOpenKeep = { nav.navigate(Routes.KEEP) }
            )
        }
        composable(Routes.CATEGORIES) {
            CategorySelectionScreen(onStartScan = { nav.navigate(Routes.SCANNING) }, vm = scannerVm)
        }
        composable(Routes.SCANNING) {
            ScanningScreen(
                onReview = { nav.navigate(Routes.SWIPE) { popUpTo(Routes.DASHBOARD) } },
                onBack = { nav.popBackStack(Routes.DASHBOARD, false) },
                vm = scannerVm
            )
        }
        composable(Routes.SWIPE) {
            SwipeCleanerScreen(onOpenQueue = { nav.navigate(Routes.QUEUE) },
                onDone = { nav.popBackStack(Routes.DASHBOARD, false) }, vm = swipeVm)
        }
        composable(Routes.QUEUE) {
            DeleteQueueScreen(onBackToSwipe = { nav.popBackStack(Routes.SWIPE, false) })
        }
        composable(Routes.KEEP) {
            KeepListScreen(vm = swipeVm)
        }
    }
}
