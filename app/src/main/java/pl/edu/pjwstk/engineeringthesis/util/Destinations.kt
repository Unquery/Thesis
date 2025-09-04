package pl.edu.pjwstk.engineeringthesis.util

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pl.edu.pjwstk.engineeringthesis.view.ConnectBandScreen
import pl.edu.pjwstk.engineeringthesis.view.MenuScreen


fun NavGraphBuilder.menuDestination(){
    composable<Menu>{
        MenuScreen(onConnectBandClick = {})
    }
}

fun NavGraphBuilder.connectBandDestination(){
    composable<ConnectBand>{
        ConnectBandScreen()
    }
}