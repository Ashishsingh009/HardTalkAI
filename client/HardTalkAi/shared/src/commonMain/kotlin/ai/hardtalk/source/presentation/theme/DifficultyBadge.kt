package ai.hardtalk.source.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DifficultyBadge(difficulty: String) {
    Text(
        text = difficulty,
        color = difficultyColor(difficulty),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(difficultyBadgeBackground(difficulty))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
