package ai.hardtalk.source.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun HardTalkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HardTalkColors.Accent,
            onPrimary = HardTalkColors.OnAccent,
            secondary = HardTalkColors.AccentMuted,
            onSecondary = HardTalkColors.OnAccent,
            background = HardTalkColors.Background,
            onBackground = HardTalkColors.TextPrimary,
            surface = HardTalkColors.Surface,
            onSurface = HardTalkColors.TextPrimary,
            error = HardTalkColors.Error,
            outline = HardTalkColors.Border,
        ),
        content = content,
    )
}
