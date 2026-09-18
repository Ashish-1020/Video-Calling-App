package com.aashu.natalks.data.signaling

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

sealed class SignalMessage {
    abstract val callId: String
    abstract val fromUserId: String
    abstract val toUserId: String
}

data class CallOfferSignal(
    override val callId: String,
    override val fromUserId: String,
    override val toUserId: String,
    val sdp: String
) : SignalMessage()

data class CallAnswerSignal(
    override val callId: String,
    override val fromUserId: String,
    override val toUserId: String,
    val sdp: String
) : SignalMessage()

data class IceCandidateSignal(
    override val callId: String,
    override val fromUserId: String,
    override val toUserId: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val candidate: String
) : SignalMessage()

data class CallEndSignal(
    override val callId: String,
    override val fromUserId: String,
    override val toUserId: String,
    val reason: String = "HANGUP"
) : SignalMessage()

data class CallRejectSignal(
    override val callId: String,
    override val fromUserId: String,
    override val toUserId: String,
    val reason: String = "DECLINED"
) : SignalMessage()

fun Moshi.Builder.addSignalMessageAdapter(): Moshi.Builder = add(
    PolymorphicJsonAdapterFactory.of(SignalMessage::class.java, "type")
        .withSubtype(CallOfferSignal::class.java, "CALL_OFFER")
        .withSubtype(CallAnswerSignal::class.java, "CALL_ANSWER")
        .withSubtype(IceCandidateSignal::class.java, "ICE_CANDIDATE")
        .withSubtype(CallEndSignal::class.java, "CALL_END")
        .withSubtype(CallRejectSignal::class.java, "CALL_REJECT")
)
