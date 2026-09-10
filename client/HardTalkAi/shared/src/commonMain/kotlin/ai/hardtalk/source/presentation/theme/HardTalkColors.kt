package ai.hardtalk.source.presentation.theme

import ai.hardtalk.source.domain.model.CounterpartTone
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object HardTalkColors {
    val Background = Color(0xFF0F172A)
    val BackgroundMid = Color(0xFF1E1B4B)
    val Surface = Color(0xFF1E293B)
    val SurfaceAlt = Color(0xFF273449)
    val Accent = Color(0xFF6366F1)
    val AccentMuted = Color(0xFF818CF8)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA5B4FC)
    val TextMuted = Color(0xFF94A3B8)
    val Error = Color(0xFFFCA5A5)
    val WarmUp = Color(0xFF34D399)
    val Moderate = Color(0xFFFBBF24)
    val Hard = Color(0xFFF87171)
    val Coach = Color(0xFFC4B5FD)
    val TakeawaySurface = Color(0xFF312E81)

    val ScreenGradient = Brush.verticalGradient(
        colors = listOf(Background, BackgroundMid, Background),
    )
}

fun difficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "warm-up" -> HardTalkColors.WarmUp
    "moderate" -> HardTalkColors.Moderate
    "hard" -> HardTalkColors.Hard
    else -> HardTalkColors.AccentMuted
}

fun scoreColor(value: Int): Color {
    val hue = (value.coerceIn(0, 100) / 100f) * 120f
    return Color.hsv(hue, 0.70f, 0.55f)
}

fun moodColor(tone: CounterpartTone): Color = when (tone) {
    CounterpartTone.WARMING -> HardTalkColors.WarmUp
    CounterpartTone.GUARDED -> HardTalkColors.Hard
    CounterpartTone.NEUTRAL -> HardTalkColors.Moderate
}
