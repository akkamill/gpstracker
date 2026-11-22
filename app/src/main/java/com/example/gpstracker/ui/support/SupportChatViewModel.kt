package com.example.gpstracker.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpstracker.data.support.ChatConnection
import com.example.gpstracker.data.support.SupportChatRepository
import com.example.gpstracker.data.support.SupportChatState
import com.example.gpstracker.model.api.ChatLimits
import com.example.gpstracker.model.api.ChatSender
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the header says about the chat. */
enum class SupportStatus { SignedOut, Connecting, TeamOnline, TeamAway, NoConnection }

/** One speech bubble. [sending] until the server confirms the message. */
data class ChatBubbleUi(val key: String, val text: String, val mine: Boolean, val timeLabel: String, val sending: Boolean = false)

data class SupportChatUiState(
    val status: SupportStatus = SupportStatus.Connecting,
    /** Oldest first. */
    val bubbles: List<ChatBubbleUi> = emptyList(),
    val draft: String = "",
) {
    val canSend: Boolean get() = status != SupportStatus.SignedOut && draft.isNotBlank() && draft.length <= ChatLimits.MAX_TEXT
}

/** Keeps the chat connected while the screen is open; the draft lives here, not in the repository. */
class SupportChatViewModel(
    private val chat: SupportChatRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val draft = MutableStateFlow("")

    val state: StateFlow<SupportChatUiState> = combine(chat.state, draft) { s, d -> map(s, d) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, map(chat.state.value, ""))

    init {
        chat.open()
    }

    fun onDraftChange(value: String) {
        draft.value = value.take(ChatLimits.MAX_TEXT)
    }

    fun onSend() {
        if (state.value.canSend && chat.send(draft.value)) draft.value = ""
    }

    override fun onCleared() = chat.close()

    private fun map(s: SupportChatState, draft: String): SupportChatUiState {
        val now = clock()
        val confirmed = s.messages.map {
            ChatBubbleUi("m${it.id}", it.text, mine = it.sender == ChatSender.Driver, timeLabel = timeLabel(it.sentAtMs, now))
        }
        val pending = s.pending.map {
            ChatBubbleUi("p${it.clientId}", it.text, mine = true, timeLabel = timeLabel(it.createdAtMs, now), sending = true)
        }
        return SupportChatUiState(status = status(s), bubbles = confirmed + pending, draft = draft)
    }

    private fun status(s: SupportChatState) = when (s.connection) {
        ChatConnection.SignedOut -> SupportStatus.SignedOut
        ChatConnection.Connecting -> SupportStatus.Connecting
        ChatConnection.Offline -> SupportStatus.NoConnection
        ChatConnection.Connected -> if (s.agentsOnline) SupportStatus.TeamOnline else SupportStatus.TeamAway
    }

    /** "14:05" today, "21 Nov, 14:05" on other days. */
    private fun timeLabel(ms: Long, now: Long): String {
        val sameDay = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_YEAR) ==
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_YEAR) && now - ms < DAY_MS
        val pattern = if (sameDay) "HH:mm" else "d MMM, HH:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
