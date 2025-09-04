package pl.edu.pjwstk.engineeringthesis.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray700
import pl.edu.pjwstk.engineeringthesis.ui.theme.DarkGray850
import pl.edu.pjwstk.engineeringthesis.ui.theme.EngineeringThesisTheme

@Composable
fun MenuScreen(
    onConnectBandClick: () -> Unit
) {
    var connectBandMenuOpen by remember { mutableStateOf(false) }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (connectBandMenuOpen) 0.35f else 0f,
        label = stringResource(R.string.menu_alpha)
    )
    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopApplicationBar(
                    onConnectBandClick
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color.DarkGray)
                )
            }

        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(Color.Blue)
            )
        }

        if (scrimAlpha > 0f) {
            AlphaOverlay(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { connectBandMenuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = connectBandMenuOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 18.dp)
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    color = DarkGray850,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                    ) {
                        MenuActionItem(
                            text = stringResource(R.string.connect_band_menu_button),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            onClick = {
                                connectBandMenuOpen = false
                                onConnectBandClick()
                            }
                        )

                        MenuActionItem(
                            text = stringResource(R.string.connect_band_menu_button),
                            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                            onClick = { /* TODO: navigate to settings or any action */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionItem(
    text: String,
    shape: Shape,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = if (pressed) DarkGray700 else DarkGray850
    ) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .heightIn(min = 56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AlphaOverlay(modifier: Modifier){
    Box(
        modifier = modifier
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopApplicationBar(
    onPlusClicked : () -> Unit
){
    Box {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.main_screen_title),
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        fontSize = 40.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            navigationIcon = { /* no impl */ },
            actions = {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onPlusClicked
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.connect_band_menu_button),
                            tint = Color.White
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                scrolledContainerColor = Color.Black,
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    EngineeringThesisTheme {
        MenuScreen {}
    }
}

@Preview(showBackground = true)
@Composable
private fun TopApplicationBarPreview() {
    EngineeringThesisTheme {
        TopApplicationBar {}
    }
}