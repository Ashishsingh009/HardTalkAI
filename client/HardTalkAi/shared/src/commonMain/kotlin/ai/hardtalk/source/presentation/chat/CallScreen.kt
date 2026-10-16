package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Live counterpart call. Coach Heather is not here — recap lands after hang-up.
 */
@Composable
fun CounterpartCallScreen(
    personaName: String,
    personaRole: String,
    status: String,
    userTurns: Int,
    muted: Boolean,
    onMuteToggle: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Live 1:1",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = personaName,
                color = HardTalkColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = personaRole,
                color = HardTalkColors.TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = status.ifBlank { "Listening…" },
                color = HardTalkColors.AccentMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Your turns $userTurns/${PracticeLoop.MAX_USER_TURNS} · hang up or the call ends at ${PracticeLoop.MAX_CALL_DURATION_SECONDS}s",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        CallWaveform()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Coach Heather scores after you hang up. This call is only $personaName.",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(onClick = onMuteToggle) {
                    Text(if (muted) "Unmute" else "Mute", color = HardTalkColors.TextPrimary)
                }
                Button(
                    onClick = onHangUp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HardTalkColors.Hard,
                        contentColor = HardTalkColors.OnAccent,
                    ),
                ) {
                    Text("Hang up", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CallWaveform() {
    val transition = rememberInfiniteTransition(label = "call-wave")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(7) { index ->
            val phase = transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700 + index * 90, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar-$index",
            )
            val height by phase
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(8.dp)
                    .height((18 + height * 42).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HardTalkColors.AccentMuted.copy(alpha = 0.85f)),
            )
        }
    }
}
