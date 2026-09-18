package com.aashu.natalks.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionHolder {
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    fun set(session: Session?) {
        _session.value = session
    }

    fun current(): Session? = _session.value
}
