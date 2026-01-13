package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import pl.edu.pjwstk.engineeringthesis.R

@Composable
fun SplashOverlay(
    onGone: () -> Unit,
    holdMs: Long = 2100,
    exitMs: Int = 500
) {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_hearth_beat))
    val visibleState = remember { MutableTransitionState(true) }

    LaunchedEffect(Unit) {
        delay(holdMs)
        visibleState.targetState = false
        snapshotFlow { visibleState.isIdle && !visibleState.currentState && !visibleState.targetState }
            .first { it }
        onGone()
    }

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visibleState = visibleState,
        exit = fadeOut(
            animationSpec = tween(exitMs, easing = FastOutSlowInEasing)
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(exitMs, easing = FastOutSlowInEasing)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LottieAnimation(
                    composition = comp,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(108.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.splash_title_smart_band),
                    fontSize = 20.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
