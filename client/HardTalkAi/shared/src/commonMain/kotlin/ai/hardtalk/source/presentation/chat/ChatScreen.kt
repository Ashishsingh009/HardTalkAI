package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.presentation.theme.DifficultyBadge
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
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
        val extra = if (uiState.sending) 1 else 0
        val target = uiState.messages.size + extra
        if (target >= 0) {
            listState.animateScrollToItem(target)
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
            uiState.error?.let { message ->
                Text(
                    text = message,
                    color = HardTalkColors.Error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CoachPanel(
                        personaName = uiState.scenario.persona.name,
                        personaRole = uiState.scenario.persona.role,
                        goals = uiState.scenario.goals,
                        mood = uiState.mood,
                        tone = uiState.counterpartTone,
                        progress = uiState.roundProgress,
                        scoredTurns = uiState.scoredUserTurns,
                    )
                }
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
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                }
            }

            Composer(
                value = uiState.input,
                enabled = uiState.canSend,
                placeholder = "Reply to ${uiState.scenario.persona.name}…",
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
    val turnLabel = scoredTurns.coerceAtMost(PracticeLoop.MAX_USER_TURNS)
    val meta = if (roundComplete) {
        "Recap · retry with intent"
    } else {
        "$personaName · ${tone.shortLabel} · $turnLabel/${PracticeLoop.MAX_USER_TURNS}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            Text("← Scenarios", color = HardTalkColors.AccentMuted, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp, end = 8.dp),
        ) {
            Text(
                text = title,
                color = HardTalkColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
            Text(
                text = meta,
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!roundComplete && mood.isNotBlank()) {
                Text(
                    text = mood,
                    color = HardTalkColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            DifficultyBadge(difficulty)
            if (canRetry) {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 32.dp),
                ) {
                    Text("Try again", color = HardTalkColors.AccentMuted, fontSize = 12.sp)
                }
            }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f).align(
                if (isUser) Alignment.End else Alignment.Start,
            ),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = if (isUser) "You" else counterpartName,
                color = HardTalkColors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp,
                        ),
                    )
                    .background(if (isUser) HardTalkColors.UserBubble else HardTalkColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.content,
                    color = HardTalkColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HardTalkColors.Background.copy(alpha = 0.92f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HardTalkColors.Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                enabled = enabled,
                placeholder = {
                    Text(placeholder, color = HardTalkColors.TextMuted, fontSize = 14.sp, maxLines = 1)
                },
                minLines = 1,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HardTalkColors.TextPrimary,
                    unfocusedTextColor = HardTalkColors.TextPrimary,
                    disabledTextColor = HardTalkColors.TextMuted,
                    focusedBorderColor = HardTalkColors.AccentMuted,
                    unfocusedBorderColor = HardTalkColors.Border,
                    cursorColor = HardTalkColors.AccentMuted,
                    focusedContainerColor = HardTalkColors.Surface,
                    unfocusedContainerColor = HardTalkColors.Surface,
                    disabledContainerColor = HardTalkColors.Surface,
                ),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.TextPrimary,
                    disabledContainerColor = HardTalkColors.SurfaceAlt,
                    disabledContentColor = HardTalkColors.TextMuted,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (enabled) "Send" else "…", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
