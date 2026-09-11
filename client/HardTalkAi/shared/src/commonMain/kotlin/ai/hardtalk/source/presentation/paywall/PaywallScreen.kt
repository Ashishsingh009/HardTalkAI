package ai.hardtalk.source.presentation.paywall

import ai.hardtalk.source.domain.billing.EntitlementState
import ai.hardtalk.source.domain.billing.ProEntitlement
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onBack: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isPro) {
        if (state.isPro) onUnlocked()
    }

    PaywallContent(
        state = state,
        onBack = onBack,
        onPurchase = viewModel::purchase,
        onRestore = viewModel::restore,
    )
}

@Composable
internal fun PaywallContent(
    state: EntitlementState,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HardTalkColors.ScreenGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("← Scenarios", color = HardTalkColors.AccentMuted)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "HardTalk Pro",
                color = HardTalkColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock the rest of the career catalog. The raise drill stays free.",
                color = HardTalkColors.TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HardTalkColors.Surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "Monthly",
                    color = HardTalkColors.AccentMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.monthlyPriceLabel ?: "Price unavailable",
                    color = HardTalkColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All manager drills · practice → score → retry · restore anytime",
                    color = HardTalkColors.TextMuted,
                    fontSize = 13.sp,
                )
            }
            if (state.monthlyPriceLabel == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (!state.configured) {
                        "RevenueCat isn’t configured (missing API key). You can still practice the free raise drill."
                    } else {
                        "Couldn’t load the monthly offering. Check network, or confirm entitlement `${ProEntitlement.ID}` and product `${ProEntitlement.MONTHLY_PRODUCT_ID}` in RevenueCat. The free raise drill still works."
                    },
                    color = HardTalkColors.TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPurchase,
                enabled = !state.busy && !state.isPro && state.monthlyPriceLabel != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.TextPrimary,
                    disabledContainerColor = HardTalkColors.SurfaceAlt,
                    disabledContentColor = HardTalkColors.TextMuted,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        color = HardTalkColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = if (state.monthlyPriceLabel != null) {
                            "Subscribe · ${state.monthlyPriceLabel}"
                        } else {
                            "Subscribe"
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRestore,
                enabled = !state.busy && state.configured,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HardTalkColors.AccentMuted,
                    disabledContentColor = HardTalkColors.TextMuted,
                ),
                border = BorderStroke(1.dp, HardTalkColors.AccentMuted),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore purchases")
            }
            state.message?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = HardTalkColors.Error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sandbox: debug builds use the RevenueCat Test Store key. " +
                    "Release must use a `goog_` Play key — never ship `test_`. " +
                    "Payments on a real device go through Google Play.",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
