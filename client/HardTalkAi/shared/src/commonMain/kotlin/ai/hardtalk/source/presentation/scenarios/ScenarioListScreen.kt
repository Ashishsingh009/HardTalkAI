package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.presentation.theme.DifficultyBadge
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScenarioListScreen(
    apiBaseUrl: String,
    viewModel: ScenarioListViewModel,
    onScenarioSelected: (Scenario) -> Unit,
    onPrivacyClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        val ready = uiState as? ScenarioListUiState.Ready ?: return@LaunchedEffect
        val open = ready.openScenario ?: return@LaunchedEffect
        viewModel.consumeOpenScenario()
        onScenarioSelected(open)
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HardTalk AI",
                        color = HardTalkColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Career coaching · Coach Heather",
                        color = HardTalkColors.TextSecondary,
                        fontSize = 16.sp,
                    )
                }
                TextButton(
                    onClick = onPrivacyClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Privacy", color = HardTalkColors.AccentMuted, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a drill, get scored, retry — not an open-ended chat.",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
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
                            text = "Is FastAPI running on this host? This build talks to $apiBaseUrl",
                            color = HardTalkColors.TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadScenarios() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HardTalkColors.Accent,
                                contentColor = HardTalkColors.OnAccent,
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item {
                            val lede = if (state.hasPro || state.ungated) {
                                "${state.scenarios.size} manager conversations. Raise is the free practice."
                            } else {
                                "${state.scenarios.size} manager conversations. Raise is free; unlock the rest with HardTalk Pro."
                            }
                            Text(
                                text = lede,
                                color = HardTalkColors.TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        items(state.scenarios, key = { it.id }) { scenario ->
                            ScenarioCard(
                                scenario = scenario,
                                locked = state.isLocked(scenario),
                                onClick = { viewModel.onScenarioTapped(scenario) },
                            )
                        }
                    }
                }
            }
        }

        val ready = uiState as? ScenarioListUiState.Ready
        val paywallScenario = ready?.paywallScenario
        if (ready != null && paywallScenario != null) {
            PaywallScrim(
                scenario = paywallScenario,
                priceLabel = ready.priceLabel,
                purchasing = ready.paywallPurchasing,
                error = ready.paywallError,
                onDismiss = viewModel::dismissPaywall,
                onPurchase = viewModel::purchaseSelected,
                onRestore = viewModel::restorePurchases,
            )
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: Scenario,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HardTalkColors.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DifficultyBadge(scenario.difficulty)
            if (scenario.free) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Free practice",
                    color = HardTalkColors.Coach,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(HardTalkColors.TakeawaySurface)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            } else if (locked) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pro",
                    color = HardTalkColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(HardTalkColors.SurfaceAlt)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = scenario.title,
            color = HardTalkColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = scenario.summary,
            color = HardTalkColors.TextMuted,
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "You'll talk to ${scenario.persona.name} — ${scenario.persona.role}",
            color = HardTalkColors.TextSecondary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PaywallScrim(
    scenario: Scenario,
    priceLabel: String?,
    purchasing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HardTalkColors.TextPrimary.copy(alpha = 0.32f))
            .clickable(enabled = !purchasing, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(HardTalkColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(20.dp),
        ) {
            Text(
                text = "Unlock HardTalk Pro",
                color = HardTalkColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${scenario.title}\" is a Pro drill. The raise stays free. Unlock the rest of the catalog with a one-time Play purchase.",
                color = HardTalkColors.TextMuted,
                fontSize = 14.sp,
            )
            if (!error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error,
                    color = HardTalkColors.Error,
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPurchase,
                enabled = !purchasing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.OnAccent,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (purchasing) {
                        "Working…"
                    } else {
                        val price = priceLabel?.takeIf { it.isNotBlank() }
                        if (price != null) "Unlock for $price" else "Unlock HardTalk Pro"
                    },
                )
            }
            TextButton(
                onClick = onRestore,
                enabled = !purchasing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore purchase", color = HardTalkColors.TextSecondary)
            }
            TextButton(
                onClick = onDismiss,
                enabled = !purchasing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Not now", color = HardTalkColors.TextMuted)
            }
        }
    }
}
