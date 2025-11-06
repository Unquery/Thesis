package pl.edu.pjwstk.engineeringthesis.util

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pl.edu.pjwstk.engineeringthesis.view.ConnectBandScreen
import pl.edu.pjwstk.engineeringthesis.view.MenuScreen


fun NavGraphBuilder.menuDestination(navController: NavController){
    composable<Menu>{
        MenuScreen(onConnectBandClick = {navController.navigate(ConnectBand)})
    }
}

fun NavGraphBuilder.connectBandDestination(){
    composable<ConnectBand>{
        ConnectBandScreen()
    }
}

