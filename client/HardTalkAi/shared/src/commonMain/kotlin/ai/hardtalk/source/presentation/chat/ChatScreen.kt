package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.presentation.theme.HardTalkColors
import ai.hardtalk.source.presentation.theme.difficultyColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val summary = uiState.roundSummary

    LaunchedEffect(uiState.messages.size, uiState.sending, uiState.roundComplete) {
        if (summary != null) return@LaunchedEffect
        val lastIndex = uiState.messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HardTalkColors.ScreenGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ChatHeader(
            title = uiState.scenario.title,
            difficulty = uiState.scenario.difficulty,
            personaName = uiState.scenario.persona.name,
            mood = uiState.mood,
            tone = uiState.counterpartTone,
            scoredTurns = uiState.scoredUserTurns,
            canRetry = uiState.canRetry,
            roundComplete = uiState.roundComplete,
            onBack = onBack,
            onRetry = viewModel::retryScenario,
        )

        if (summary != null) {
            RoundSummaryScreen(
                scenarioTitle = uiState.scenario.title,
                personaName = uiState.scenario.persona.name,
                mood = uiState.mood,
                tone = uiState.counterpartTone,
                summary = summary,
                onRetry = viewModel::retryScenario,
                onPickAnother = onBack,
            )
        } else {
            CoachPanel(
                personaName = uiState.scenario.persona.name,
                personaRole = uiState.scenario.persona.role,
                goals = uiState.scenario.goals,
                mood = uiState.mood,
                tone = uiState.counterpartTone,
                progress = uiState.roundProgress,
                scoredTurns = uiState.scoredUserTurns,
            )

            uiState.error?.let { message ->
                Text(
                    text = message,
                    color = HardTalkColors.Error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(uiState.messages) { index, message ->
                    val historyThroughHere = uiState.messages
                        .take(index + 1)
                        .mapNotNull { turn ->
                            turn.feedback.takeIf { turn.role == ChatRole.USER }
                        }
                    MessageBubble(
                        message = message,
                        counterpartName = uiState.scenario.persona.name,
                        turnNumber = historyThroughHere.size,
                        previous = historyThroughHere.dropLast(1).lastOrNull(),
                        overallHistory = historyThroughHere.map { it.overall },
                    )
                }
                if (uiState.sending) {
                    item {
                        Text(
                            text = "${uiState.scenario.persona.name} is scoring your reply…",
                            color = HardTalkColors.TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Composer(
                value = uiState.input,
                enabled = uiState.canSend,
                placeholder = "Practice your reply to ${uiState.scenario.persona.name}…",
                onValueChange = viewModel::onInputChange,
                onSend = viewModel::send,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    title: String,
    difficulty: String,
    personaName: String,
    mood: String,
    tone: CounterpartTone,
    scoredTurns: Int,
    canRetry: Boolean,
    roundComplete: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Scenarios", color = HardTalkColors.AccentMuted)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (canRetry) {
                TextButton(onClick = onRetry) {
                    Text("Try again", color = HardTalkColors.AccentMuted)
                }
            }
            Text(
                text = difficulty,
                color = difficultyColor(difficulty),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Text(
            text = title,
            color = HardTalkColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = if (roundComplete) {
                "Recap · retry with intent"
            } else {
                "$personaName · ${tone.shortLabel} · Turn ${scoredTurns.coerceAtMost(PracticeLoop.MAX_USER_TURNS)} of ${PracticeLoop.MAX_USER_TURNS}"
            },
            color = HardTalkColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        if (!roundComplete) {
            Text(
                text = mood,
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    counterpartName: String,
    turnNumber: Int,
    previous: Feedback?,
    overallHistory: List<Int>,
) {
    val isUser = message.role == ChatRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = if (isUser) "You" else counterpartName,
            color = HardTalkColors.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) HardTalkColors.Accent else HardTalkColors.Surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                color = HardTalkColors.TextPrimary,
                fontSize = 15.sp,
            )
        }
        message.feedback?.let { feedback ->
            FeedbackCard(
                feedback = feedback,
                turnNumber = turnNumber,
                previous = previous,
                overallHistory = overallHistory,
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    enabled: Boolean,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = {
                Text(placeholder, color = HardTalkColors.TextMuted, fontSize = 14.sp)
            },
            minLines = 2,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HardTalkColors.TextPrimary,
                unfocusedTextColor = HardTalkColors.TextPrimary,
                focusedBorderColor = HardTalkColors.Accent,
                unfocusedBorderColor = HardTalkColors.SurfaceAlt,
                cursorColor = HardTalkColors.AccentMuted,
                focusedContainerColor = HardTalkColors.Surface,
                unfocusedContainerColor = HardTalkColors.Surface,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = HardTalkColors.Accent,
                contentColor = HardTalkColors.TextPrimary,
                disabledContainerColor = HardTalkColors.SurfaceAlt,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (enabled) "Send" else "…")
        }
    }
}
