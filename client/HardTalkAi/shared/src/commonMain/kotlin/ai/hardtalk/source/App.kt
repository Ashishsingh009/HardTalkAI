package ai.hardtalk.source

import ai.hardtalk.source.domain.model.SplashDestination
import ai.hardtalk.source.presentation.main.MainScreen
import ai.hardtalk.source.presentation.splash.SplashScreen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var destination by remember { mutableStateOf<SplashDestination?>(null) }

        Crossfade(
            targetState = destination,
            animationSpec = tween(durationMillis = 600),
            label = "ScreenTransition"
        ) { currentDestination ->
            if (currentDestination == null) {
                SplashScreen(
                    onNavigateNext = { target ->
                        destination = target
                    }
                )
            } else {
                MainScreen(destination = currentDestination)
            }
        }
    }
}
