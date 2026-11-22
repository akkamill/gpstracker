package com.example.gpstracker.ui.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SupportChatRoute(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: SupportChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SupportChatScreen(
        state = state,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::onSend,
        onCreateAccount = onCreateAccount,
        onSignIn = onSignIn,
    )
}
