package ai.hardtalk.source.presentation.about

import ai.hardtalk.source.legal.LegalLinks
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

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
        ) {
            Text(
                text = "Privacy policy",
                color = HardTalkColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prototype stub · updated ${LegalLinks.LAST_UPDATED}",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = LegalLinks.SUMMARY,
                color = HardTalkColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Not legal advice. Not a GDPR/CCPA certification. " +
                    "Read the full policy before Play listing.",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { uriHandler.openUri(LegalLinks.PRIVACY_POLICY_URL) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.TextPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Read the full policy")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contact ${LegalLinks.CONTACT_EMAIL}",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}
