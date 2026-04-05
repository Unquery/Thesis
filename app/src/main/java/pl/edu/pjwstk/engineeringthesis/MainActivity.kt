package pl.edu.pjwstk.engineeringthesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.util.Menu
import pl.edu.pjwstk.engineeringthesis.util.chartsDestination
import pl.edu.pjwstk.engineeringthesis.util.connectBandDestination
import pl.edu.pjwstk.engineeringthesis.util.menuDestination
import pl.edu.pjwstk.engineeringthesis.util.profileDestination
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
    NavHost(navController = navController, startDestination = Menu){
        menuDestination(navController)

        connectBandDestination(navController, connectBandVm)
        chartsDestination(navController)
        profileDestination(navController)

    }
}
