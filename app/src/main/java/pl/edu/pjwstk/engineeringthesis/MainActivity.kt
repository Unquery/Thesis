package pl.edu.pjwstk.engineeringthesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.util.ConnectBand
import pl.edu.pjwstk.engineeringthesis.util.Menu
import pl.edu.pjwstk.engineeringthesis.util.Profile
import pl.edu.pjwstk.engineeringthesis.util.chartsDestination
import pl.edu.pjwstk.engineeringthesis.util.connectBandDestination
import pl.edu.pjwstk.engineeringthesis.util.menuDestination
import pl.edu.pjwstk.engineeringthesis.util.navigateToTopLevel
import pl.edu.pjwstk.engineeringthesis.util.profileDestination
import pl.edu.pjwstk.engineeringthesis.view.BottomNavBar
import pl.edu.pjwstk.engineeringthesis.view.ProfileOnboardingScreen
import pl.edu.pjwstk.engineeringthesis.view.SplashOverlay
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectBandViewModel
import pl.edu.pjwstk.engineeringthesis.viewmodel.ProfileGateViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        setContent {
            EngineeringThesisTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(
    gateVm: ProfileGateViewModel = hiltViewModel(),
    connectBandVm: ConnectBandViewModel = hiltViewModel()
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val show by gateVm.showOnboarding.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, connectBandVm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    connectBandVm.onAppVisible()
                    connectBandVm.startAppReconnectIfPossible(timeoutMs = 20_000L)
                }
                Lifecycle.Event.ON_STOP -> connectBandVm.onAppHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Navigation(connectBandVm)

        if (showSplash) {
            SplashOverlay(onGone = { showSplash = false })
        }

        if (show == true && !showSplash) {
            Surface(color = MaterialTheme.colorScheme.background) {
                ProfileOnboardingScreen(
                    onDone = { gateVm.markDone(); }
                )
            }
        }
    }
}


@Composable
fun Navigation(connectBandVm: ConnectBandViewModel){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isMenuDestination = currentDestination.isRouteInHierarchy<Menu>()
    val isConnectBandDestination = currentDestination.isRouteInHierarchy<ConnectBand>()
    val isProfileDestination = currentDestination.isRouteInHierarchy<Profile>()
    val showBottomBar = isMenuDestination || isConnectBandDestination || isProfileDestination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    onHealthClick = { navController.navigateToTopLevel(Menu) },
                    onDeviceClick = { navController.navigateToTopLevel(ConnectBand) },
                    onProfileClick = { navController.navigateToTopLevel(Profile) },
                    healthTint = if (isMenuDestination) Color(0xFFE53935) else Color.White,
                    deviceTint = if (isConnectBandDestination) Color(0xFF0284C7) else Color.White,
                    profileTint = if (isProfileDestination) Color(0xFFF59E0B) else Color.White
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Menu,
            modifier = Modifier.padding(innerPadding)
        ){
            menuDestination(navController)
            connectBandDestination(connectBandVm)
            chartsDestination(navController)
            profileDestination()
        }
    }
}

private inline fun <reified T : Any> NavDestination?.isRouteInHierarchy(): Boolean {
    return this?.hierarchy?.any { it.hasRoute<T>() } == true
}
