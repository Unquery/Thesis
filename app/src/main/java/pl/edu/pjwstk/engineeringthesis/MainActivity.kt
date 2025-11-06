package pl.edu.pjwstk.engineeringthesis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme
import pl.edu.pjwstk.engineeringthesis.util.Menu
import pl.edu.pjwstk.engineeringthesis.util.connectBandDestination
import pl.edu.pjwstk.engineeringthesis.util.menuDestination
import pl.edu.pjwstk.engineeringthesis.view.SplashOverlay


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
fun AppRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Navigation()

        if (showSplash) {
            SplashOverlay(onGone = { showSplash = false })
        }
    }
}


@Composable
fun Navigation(){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Menu){
        menuDestination(navController)

        connectBandDestination()

        //Todo Other screens
    }
}