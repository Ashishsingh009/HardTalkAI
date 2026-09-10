package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.domain.model.RoundProgress
import ai.hardtalk.source.presentation.theme.HardTalkColors
import ai.hardtalk.source.presentation.theme.moodColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CoachPanel(
    personaName: String,
    personaRole: String,
    goals: List<String>,
    mood: String,
    tone: CounterpartTone,
    progress: RoundProgress?,
    scoredTurns: Int,
) {
    var goalsExpanded by remember { mutableStateOf(true) }
    LaunchedEffect(scoredTurns) {
        if (scoredTurns > 0) goalsExpanded = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HardTalkColors.Surface)
            .padding(12.dp),
    ) {
        Text(
            text = "Coach Heather",
            color = HardTalkColors.Coach,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Career drill with $personaName · $personaRole",
            color = HardTalkColors.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )

        if (goals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { goalsExpanded = !goalsExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "What to practice",
                    color = HardTalkColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (goalsExpanded) "Hide" else "${goals.size} goals",
                    color = HardTalkColors.AccentMuted,
                    fontSize = 11.sp,
                )
            }
            if (goalsExpanded) {
                Column {
                    goals.forEachIndexed { index, goal ->
                        GoalRow(number = index + 1, text = goal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        MoodChip(personaName = personaName, mood = mood, tone = tone)

        if (progress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Scores this round",
                color = HardTalkColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            ScoreSparkline(
                scores = progress.overallScores,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            ScorePills(scores = progress.overallScores)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = progress.takeaway,
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "After each reply: clarity, empathy, and assertiveness — plus one thing to try next. ${PracticeLoop.MAX_USER_TURNS} turns, then a recap.",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun GoalRow(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = number.toString(),
            color = HardTalkColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .background(HardTalkColors.Accent)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = HardTalkColors.TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
fun MoodChip(
    personaName: String,
    mood: String,
    tone: CounterpartTone,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(moodColor(tone)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$personaName is ${tone.shortLabel.lowercase()}",
                color = moodColor(tone),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mood,
                color = HardTalkColors.TextMuted,
                fontSize = 11.sp,
            )
        }
    }
}
