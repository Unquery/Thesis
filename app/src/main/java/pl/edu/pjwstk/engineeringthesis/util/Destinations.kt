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

private inline fun <reified T : Any> NavController.navigateToTopLevel(route: T) {
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
            onConnectBandClick = { navController.navigateToTopLevel(ConnectBand) },
            onProfileClick = { navController.navigateToTopLevel(Profile) },
            onMetricClick = { metric -> navController.navigate(Charts(metric)) }
        )
    }
}

fun NavGraphBuilder.connectBandDestination(
    navController: NavController,
    vmConnectBand: ConnectBandViewModel
){
    composable<ConnectBand>{
        ConnectBandScreen(
            onHealthClick = { navController.navigateToTopLevel(Menu) },
            onDeviceClick = {},
            onProfileClick = { navController.navigateToTopLevel(Profile) },
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

fun NavGraphBuilder.profileDestination(navController: NavController){
    composable<Profile>{
        ProfileScreen(
            onHealthClick = { navController.navigateToTopLevel(Menu) },
            onDeviceClick = { navController.navigateToTopLevel(ConnectBand) },
            onProfileClick = {}
        )
    }
}
