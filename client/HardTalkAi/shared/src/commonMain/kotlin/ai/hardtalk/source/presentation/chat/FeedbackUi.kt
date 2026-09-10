package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.CoachingInsights
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.presentation.theme.HardTalkColors
import ai.hardtalk.source.presentation.theme.scoreColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeedbackCard(
    feedback: Feedback,
    turnNumber: Int,
    previous: Feedback?,
    overallHistory: List<Int>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HardTalkColors.SurfaceAlt)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Turn $turnNumber coaching",
                    color = HardTalkColors.Coach,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Overall ${feedback.overall}",
                    color = HardTalkColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            ScoreSparkline(
                scores = overallHistory,
                modifier = Modifier
                    .width(96.dp)
                    .height(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        ScoreRow("Clarity", feedback.clarity, previous?.clarity)
        ScoreRow("Empathy", feedback.empathy, previous?.empathy)
        ScoreRow("Assertiveness", feedback.assertiveness, previous?.assertiveness)
        if (feedback.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Try this",
                color = HardTalkColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            feedback.tips.forEach { tip ->
                Text(
                    text = "• $tip",
                    color = HardTalkColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun ScoreRow(label: String, value: Int, previous: Int? = null) {
    val change = previous?.let { value - it }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = HardTalkColors.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(110.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(HardTalkColors.Background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                    .height(8.dp)
                    .background(scoreColor(value)),
            )
        }
        Text(
            text = value.toString(),
            color = HardTalkColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (change != null) {
            Text(
                text = CoachingInsights.formatChange(change),
                color = when {
                    change > 0 -> HardTalkColors.WarmUp
                    change < 0 -> HardTalkColors.Hard
                    else -> HardTalkColors.TextMuted
                },
                fontSize = 11.sp,
                modifier = Modifier.width(32.dp).padding(start = 4.dp),
            )
        }
    }
}

@Composable
fun ScoreSparkline(
    scores: List<Int>,
    modifier: Modifier = Modifier,
    maxTurns: Int = PracticeLoop.MAX_USER_TURNS,
) {
    if (scores.isEmpty()) {
        Spacer(modifier = modifier)
        return
    }
    val lineColor = HardTalkColors.AccentMuted
    val dotColor = HardTalkColors.Coach
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pad = 4.dp.toPx()
        val usableWidth = (width - pad * 2).coerceAtLeast(1f)
        val usableHeight = (height - pad * 2).coerceAtLeast(1f)
        val slots = (maxTurns - 1).coerceAtLeast(1)
        fun point(index: Int, score: Int): Offset {
            val x = pad + usableWidth * (index / slots.toFloat())
            val y = pad + usableHeight * (1f - score.coerceIn(0, 100) / 100f)
            return Offset(x, y)
        }
        val points = scores.mapIndexed { index, score -> point(index, score) }
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        points.forEach { point ->
            drawCircle(color = dotColor, radius = 3.dp.toPx(), center = point)
        }
    }
}

@Composable
fun ScorePills(scores: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        scores.forEachIndexed { index, score ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HardTalkColors.Background)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(scoreColor(score)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "T${index + 1} · $score",
                    color = HardTalkColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
