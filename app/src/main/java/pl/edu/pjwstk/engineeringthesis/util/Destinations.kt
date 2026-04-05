package pl.edu.pjwstk.engineeringthesis.util

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pl.edu.pjwstk.engineeringthesis.view.ChartsScreen
import pl.edu.pjwstk.engineeringthesis.view.ConnectBandScreen
import pl.edu.pjwstk.engineeringthesis.view.MenuScreen
import pl.edu.pjwstk.engineeringthesis.view.ProfileScreen
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel


fun NavGraphBuilder.menuDestination(navController: NavController){
    composable<Menu>{
        MenuScreen(
            onConnectBandClick = { navController.navigate(ConnectBand) },
            onProfileClick = { navController.navigate(Profile) },
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
            onHealthClick = { navController.navigate(Menu) },
            onDeviceClick = {},
            onProfileClick = { navController.navigate(Profile) },
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
            onHealthClick = { navController.navigate(Menu) },
            onDeviceClick = { navController.navigate(ConnectBand) },
            onProfileClick = {}
        )
    }
}
