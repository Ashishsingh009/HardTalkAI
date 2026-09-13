package ai.hardtalk.source.presentation.theme

import ai.hardtalk.source.domain.model.CounterpartTone
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object HardTalkColors {
    val Background = Color(0xFFFFFFFF)
    val BackgroundMid = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF8FAFC)
    val Accent = Color(0xFF2563EB)
    val AccentMuted = Color(0xFF3B82F6)
    val UserBubble = Color(0xFF2563EB)
    val OnAccent = Color(0xFFFFFFFF)
    val Border = Color(0xFFE2E8F0)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF1D4ED8)
    val TextMuted = Color(0xFF475569)
    val Error = Color(0xFFDC2626)
    val WarmUp = Color(0xFF047857)
    val Moderate = Color(0xFFB45309)
    val Hard = Color(0xFFB91C1C)
    val Coach = Color(0xFF2563EB)
    val TakeawaySurface = Color(0xFFEFF6FF)

    val ScreenGradient = Brush.verticalGradient(
        colors = listOf(Background, Background, Background),
    )
}

fun difficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "warm-up" -> HardTalkColors.WarmUp
    "moderate" -> HardTalkColors.Moderate
    "hard" -> HardTalkColors.Hard
    else -> HardTalkColors.Accent
}

fun difficultyBadgeBackground(difficulty: String): Color = when (difficulty.lowercase()) {
    "warm-up" -> Color(0xFFECFDF5)
    "moderate" -> Color(0xFFFFFBEB)
    "hard" -> Color(0xFFFEF2F2)
    else -> HardTalkColors.TakeawaySurface
}

fun scoreColor(value: Int): Color {
    val hue = (value.coerceIn(0, 100) / 100f) * 120f
    return Color.hsv(hue, 0.72f, 0.72f)
}

fun moodColor(tone: CounterpartTone): Color = when (tone) {
    CounterpartTone.WARMING -> HardTalkColors.WarmUp
    CounterpartTone.GUARDED -> HardTalkColors.Hard
    CounterpartTone.NEUTRAL -> HardTalkColors.Accent
}
