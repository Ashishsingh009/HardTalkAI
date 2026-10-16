package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.InvoiceSendResult
import ai.hardtalk.source.domain.model.InvoiceTerms
import ai.hardtalk.source.presentation.theme.HardTalkColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InvoiceReviewScreen(
    terms: InvoiceTerms?,
    missing: List<String>,
    source: String?,
    extracting: Boolean,
    sending: Boolean,
    result: InvoiceSendResult?,
    error: String?,
    onTermsChange: (InvoiceTerms) -> Unit,
    onSend: () -> Unit,
    onBackToRecap: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val draft = terms ?: InvoiceTerms()
    val canSend = !extracting && !sending && result == null && draft.readyToSend

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Review invoice terms",
                color = HardTalkColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Nothing is sent to PayPal until you tap Send PayPal invoice.",
                color = HardTalkColors.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (extracting) {
            item {
                Text(
                    text = "Reading the round for amount, date, and client email…",
                    color = HardTalkColors.TextSecondary,
                    fontSize = 14.sp,
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
                    text = if (source == "ai") {
                        "Extracted with AI from the transcript. Edit anything that’s wrong."
                    } else {
                        "Extracted from the transcript. Fill anything that’s missing before you send."
                    },
                    color = HardTalkColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                if (missing.isNotEmpty() && result == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Still needed: ${missing.joinToString(", ")}",
                        color = HardTalkColors.Moderate,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        item {
            InvoiceField(
                label = "Amount",
                value = draft.amount,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(amount = it)) },
            )
        }
        item {
            InvoiceField(
                label = "Currency",
                value = draft.currency,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(currency = it.uppercase())) },
            )
        }
        item {
            InvoiceField(
                label = "Due date (YYYY-MM-DD)",
                value = draft.dueDate,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(dueDate = it)) },
            )
        }
        item {
            InvoiceField(
                label = "Client name",
                value = draft.recipientName,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(recipientName = it)) },
            )
        }
        item {
            InvoiceField(
                label = "Client email",
                value = draft.recipientEmail,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(recipientEmail = it)) },
            )
        }
        item {
            InvoiceField(
                label = "Line item",
                value = draft.description,
                enabled = result == null && !extracting,
                onValueChange = { onTermsChange(draft.copy(description = it)) },
            )
        }
        if (!error.isNullOrBlank()) {
            item {
                Text(
                    text = error,
                    color = HardTalkColors.Error,
                    fontSize = 13.sp,
                )
            }
        }
        if (result != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HardTalkColors.Surface)
                        .padding(16.dp),
                ) {
                    Text(
                        text = if (result.isMock) "Mock invoice (no PayPal credentials)" else "PayPal sandbox invoice",
                        color = if (result.isMock) HardTalkColors.Moderate else HardTalkColors.WarmUp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: ${result.status}",
                        color = HardTalkColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "ID: ${result.invoiceId}",
                        color = HardTalkColors.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (result.payerLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { uriHandler.openUri(result.payerLink) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Open payer link", color = HardTalkColors.AccentMuted)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardTalkColors.Accent,
                    contentColor = HardTalkColors.OnAccent,
                    disabledContainerColor = HardTalkColors.SurfaceAlt,
                    disabledContentColor = HardTalkColors.TextMuted,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    when {
                        sending -> "Sending…"
                        result != null -> "Invoice sent"
                        else -> "Send PayPal invoice"
                    },
                )
            }
        }
        item {
            OutlinedButton(
                onClick = onBackToRecap,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Back to recap", color = HardTalkColors.AccentMuted)
            }
        }
        item {
            Text(
                text = "Sandbox only. Live PayPal is disabled in this build.",
                color = HardTalkColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun InvoiceField(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label, color = HardTalkColors.TextMuted, fontSize = 12.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HardTalkColors.TextPrimary,
            unfocusedTextColor = HardTalkColors.TextPrimary,
            disabledTextColor = HardTalkColors.TextMuted,
            focusedBorderColor = HardTalkColors.AccentMuted,
            unfocusedBorderColor = HardTalkColors.Border,
            cursorColor = HardTalkColors.AccentMuted,
            focusedContainerColor = HardTalkColors.Surface,
            unfocusedContainerColor = HardTalkColors.Surface,
            disabledContainerColor = HardTalkColors.SurfaceAlt,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}
