package com.aashu.natalks.call

sealed class CallState {
    data object Idle : CallState()
    data class OutgoingRinging(val callId: String, val calleeId: String, val calleeName: String) : CallState()
    data class IncomingRinging(
        val callId: String,
        val callerId: String,
        val callerName: String,
        val offerSdp: String
    ) : CallState()
    data class Connected(val callId: String, val peerId: String, val peerName: String) : CallState()
    data class Ended(val reason: String) : CallState()
}
