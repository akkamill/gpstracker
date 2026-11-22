package com.example.gpstracker.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.auth.components.PrimaryButton
import com.example.gpstracker.ui.theme.DriveColors
import com.example.gpstracker.ui.theme.DriveMetricsTheme

/** The support chat. Stateless: everything comes from [state]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    state: SupportChatUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.support_title), style = MaterialTheme.typography.titleMedium)
                        if (state.status != SupportStatus.SignedOut) StatusLine(state.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.status == SupportStatus.SignedOut) {
            SignedOutMessage(onCreateAccount, onSignIn, Modifier.padding(padding))
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                Messages(state.bubbles, Modifier.weight(1f))
                InputRow(state, onDraftChange, onSend)
            }
        }
    }
}

@Composable
private fun StatusLine(status: SupportStatus) {
    val (text, color) = when (status) {
        SupportStatus.TeamOnline -> R.string.support_status_online to DriveColors.success
        SupportStatus.TeamAway -> R.string.support_status_away to MaterialTheme.colorScheme.onSurfaceVariant
        SupportStatus.Connecting -> R.string.support_status_connecting to MaterialTheme.colorScheme.onSurfaceVariant
        SupportStatus.NoConnection, SupportStatus.SignedOut -> R.string.support_status_offline to DriveColors.warning
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Messages(bubbles: List<ChatBubbleUi>, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    LaunchedEffect(bubbles.size) { if (bubbles.isNotEmpty()) list.animateScrollToItem(bubbles.lastIndex) }
    if (bubbles.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.support_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        state = list,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(bubbles, key = { it.key }) { Bubble(it) }
    }
}

@Composable
private fun Bubble(bubble: ChatBubbleUi) {
    val mine = bubble.mine
    val who = stringResource(if (mine) R.string.support_from_you else R.string.support_from_team)
    val time = if (bubble.sending) stringResource(R.string.support_sending) else bubble.timeLabel
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, if (mine) 4.dp else 16.dp, if (mine) 16.dp else 4.dp))
                .background(if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$who, $time: ${bubble.text}" },
        ) {
            val onBubble = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Text(bubble.text, style = MaterialTheme.typography.bodyLarge, color = onBubble)
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = onBubble.copy(alpha = if (bubble.sending) SENDING_ALPHA else TIME_ALPHA),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun InputRow(state: SupportChatUiState, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.support_input_hint)) },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(onClick = onSend, enabled = state.canSend, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(R.string.support_send_cd),
                tint = if (state.canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = DISABLED_ALPHA,
                    )
                },
            )
        }
    }
}

@Composable
private fun SignedOutMessage(onCreateAccount: () -> Unit, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Rounded.SupportAgent,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.support_signed_out_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.support_signed_out_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = stringResource(R.string.welcome_create), onClick = onCreateAccount)
        TextButton(onClick = onSignIn) { Text(stringResource(R.string.support_sign_in)) }
    }
}

private const val SENDING_ALPHA = 0.6f
private const val TIME_ALPHA = 0.75f
private const val DISABLED_ALPHA = 0.38f

@Preview(showBackground = true)
@Composable
private fun SupportChatPreview() {
    DriveMetricsTheme {
        SupportChatScreen(
            state = SupportChatUiState(
                status = SupportStatus.TeamOnline,
                bubbles = listOf(
                    ChatBubbleUi("1", "My trip this morning was not saved.", mine = true, timeLabel = "08:41"),
                    ChatBubbleUi(
                        "2",
                        "Sorry about that! Did the car stand still for more than five minutes?",
                        mine = false,
                        timeLabel = "08:43",
                    ),
                    ChatBubbleUi("3", "No, only at the lights.", mine = true, timeLabel = "08:44", sending = true),
                ),
                draft = "Thanks",
            ),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onCreateAccount = {},
            onSignIn = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportChatSignedOutPreview() {
    DriveMetricsTheme {
        SupportChatScreen(SupportChatUiState(status = SupportStatus.SignedOut), {}, {}, {}, {}, {})
    }
}
