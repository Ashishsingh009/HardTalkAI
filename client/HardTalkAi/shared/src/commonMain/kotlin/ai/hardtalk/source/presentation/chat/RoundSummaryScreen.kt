package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.CoachingInsights
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.RoundProgress
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoundSummaryScreen(
    scenarioTitle: String,
    personaName: String,
    mood: String,
    tone: CounterpartTone,
    summary: RoundProgress,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Round complete",
                color = HardTalkColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = scenarioTitle,
                color = HardTalkColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HardTalkColors.Surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = "How scores moved",
                    color = HardTalkColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${summary.overallFirst} → ${summary.overallLast}",
                        color = HardTalkColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "  overall  ${CoachingInsights.formatChange(summary.overallChange)}",
                        color = when {
                            summary.overallChange > 0 -> HardTalkColors.WarmUp
                            summary.overallChange < 0 -> HardTalkColors.Hard
                            else -> HardTalkColors.TextMuted
                        },
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                ScoreSparkline(
                    scores = summary.overallScores,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ScorePills(scores = summary.overallScores)
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HardTalkColors.Surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = "Clarity · Empathy · Assertiveness",
                    color = HardTalkColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                summary.deltas.forEach { delta ->
                    ScoreRow(
                        label = delta.dimension.label,
                        value = delta.last,
                        previous = delta.first,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Biggest lift: ${summary.mostImproved.dimension.label} ${CoachingInsights.formatChange(summary.mostImproved.change)}",
                    color = HardTalkColors.WarmUp,
                    fontSize = 12.sp,
                )
                Text(
                    text = "Still to practice: ${summary.weakest.dimension.label}",
                    color = HardTalkColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HardTalkColors.TakeawaySurface)
                    .padding(16.dp),
            ) {
                Text(
                    text = "What to try next",
                    color = HardTalkColors.Coach,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summary.takeaway,
                    color = HardTalkColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                )
                val lastTips = summary.turns.last().feedback.tips
                if (lastTips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "From your last turn",
                        color = HardTalkColors.Coach,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    lastTips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            color = HardTalkColors.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                MoodChip(personaName = personaName, mood = mood, tone = tone)
            }
        }
        item {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.TextPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Try this scenario again")
            }
        }
        item {
            OutlinedButton(
                onClick = onPickAnother,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Choose another scenario", color = HardTalkColors.AccentMuted)
            }
        }
        item {
            Text(
                text = "This is a scored drill, not an open chat. Retry with one sharper sentence.",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}
