package ai.hardtalk.source

import ai.hardtalk.source.data.api.defaultApiBaseUrl
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.presentation.chat.ChatScreen
import ai.hardtalk.source.presentation.chat.ChatViewModel
import ai.hardtalk.source.presentation.scenarios.ScenarioListScreen
import ai.hardtalk.source.presentation.scenarios.ScenarioListViewModel
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
import androidx.lifecycle.viewmodel.compose.viewModel

private sealed interface PracticeRoute {
    data object Splash : PracticeRoute
    data object Scenarios : PracticeRoute
    data class Chat(val scenario: Scenario) : PracticeRoute
}

@Composable
@Preview
fun App(apiBaseUrl: String = defaultApiBaseUrl()) {
    val container = remember(apiBaseUrl) { AppContainer(apiBaseUrl) }
    MaterialTheme {
        var route by remember { mutableStateOf<PracticeRoute>(PracticeRoute.Splash) }
        var chatSession by remember { mutableStateOf(0) }

        Crossfade(
            targetState = route,
            animationSpec = tween(durationMillis = 400),
            label = "ScreenTransition",
        ) { current ->
            when (current) {
                PracticeRoute.Splash -> {
                    SplashScreen(
                        onNavigateNext = { route = PracticeRoute.Scenarios },
                    )
                }
                PracticeRoute.Scenarios -> {
                    val listViewModel = viewModel(key = "scenarios-$apiBaseUrl") {
                        ScenarioListViewModel(container.practiceRepository)
                    }
                    ScenarioListScreen(
                        apiBaseUrl = container.apiBaseUrl,
                        viewModel = listViewModel,
                        onScenarioSelected = {
                            chatSession += 1
                            route = PracticeRoute.Chat(it)
                        },
                    )
                }
                is PracticeRoute.Chat -> {
                    val chatViewModel = viewModel(key = "chat-${current.scenario.id}-$chatSession") {
                        ChatViewModel(current.scenario, container.practiceRepository)
                    }
                    ChatScreen(
                        viewModel = chatViewModel,
                        onBack = { route = PracticeRoute.Scenarios },
                    )
                }
            }
        }
    }
}
