package pl.edu.pjwstk.engineeringthesis.util

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.composable
import pl.edu.pjwstk.engineeringthesis.view.ChartsScreen
import pl.edu.pjwstk.engineeringthesis.view.ConnectBandScreen
import pl.edu.pjwstk.engineeringthesis.view.MenuScreen
import pl.edu.pjwstk.engineeringthesis.view.ProfileScreen
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel

internal inline fun <reified T : Any> NavController.navigateToTopLevel(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavGraphBuilder.menuDestination(navController: NavController){
    composable<Menu>{
        MenuScreen(
            onMetricClick = { metric -> navController.navigate(Charts(metric)) }
        )
    }
}

fun NavGraphBuilder.connectBandDestination(
    vmConnectBand: ConnectBandViewModel
){
    composable<ConnectBand>{
        ConnectBandScreen(
            vmConnectBand = vmConnectBand
        )
    }
}

fun NavGraphBuilder.chartsDestination(navController: NavController){
    composable<Charts>{
        ChartsScreen(
            onBack = { navController.navigateUp() }
        )
    }
}

fun NavGraphBuilder.profileDestination(
    autoOpenCalibration: Boolean = false,
    onAutoOpenCalibrationConsumed: () -> Unit = {}
){
    composable<Profile>{
        ProfileScreen(
            autoOpenCalibration = autoOpenCalibration,
            onAutoOpenCalibrationConsumed = onAutoOpenCalibrationConsumed
        )
    }
}
