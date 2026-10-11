package com.example.gpstracker.ui.auth

import androidx.lifecycle.ViewModel
import com.example.gpstracker.AppContainer
import com.example.gpstracker.data.auth.AuthRepository
import com.example.gpstracker.ui.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/** Exposes who is using the app and the two session actions that need no form. */
class SessionViewModel(
    private val auth: AuthRepository = AppContainer.auth,
) : ViewModel() {
    val session: StateFlow<SessionState> = auth.session

    fun continueAsGuest() = auth.continueAsGuest()

    fun signOut() = auth.signOut()
}
