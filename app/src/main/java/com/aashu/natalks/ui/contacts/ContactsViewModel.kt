package com.aashu.natalks.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aashu.natalks.data.local.ContactNameCache
import com.aashu.natalks.data.repository.AuthRepository
import com.aashu.natalks.data.repository.Contact
import com.aashu.natalks.data.repository.ContactsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 5000L

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val contactNameCache: ContactNameCache,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState

    private var polling = false

    fun startPolling() {
        if (polling) return
        polling = true
        viewModelScope.launch {
            while (polling) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        polling = false
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val contacts = contactsRepository.listContacts()
                contactNameCache.putAll(contacts.associate { it.id to it.username })
                _uiState.value = ContactsUiState(contacts = contacts, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Couldn't load contacts")
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
