package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.presentation.theme.HardTalkColors
import ai.hardtalk.source.presentation.theme.difficultyColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScenarioListScreen(
    apiBaseUrl: String,
    viewModel: ScenarioListViewModel,
    onScenarioSelected: (Scenario) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HardTalkColors.ScreenGradient)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "HardTalk AI",
                color = HardTalkColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a scenario to practice",
                color = HardTalkColors.TextSecondary,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "API: $apiBaseUrl",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = uiState) {
                is ScenarioListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = HardTalkColors.AccentMuted)
                    }
                }
                is ScenarioListUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.message,
                            color = HardTalkColors.Error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Is FastAPI running on this host? Emulator default is http://10.0.2.2:3001",
                            color = HardTalkColors.TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadScenarios() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HardTalkColors.Accent,
                                contentColor = HardTalkColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Retry")
                        }
                    }
                }
                is ScenarioListUiState.Ready -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.scenarios, key = { it.id }) { scenario ->
                            ScenarioCard(
                                scenario = scenario,
                                onClick = { onScenarioSelected(scenario) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: Scenario,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HardTalkColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = scenario.difficulty,
            color = difficultyColor(scenario.difficulty),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = scenario.title,
            color = HardTalkColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = scenario.summary,
            color = HardTalkColors.TextMuted,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "You'll talk to ${scenario.persona.name} — ${scenario.persona.role}",
            color = HardTalkColors.TextSecondary,
            fontSize = 13.sp,
        )
    }
}
