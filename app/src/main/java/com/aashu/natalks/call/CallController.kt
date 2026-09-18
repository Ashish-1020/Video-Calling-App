package com.aashu.natalks.call

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.aashu.natalks.data.local.ContactNameCache
import com.aashu.natalks.data.local.SessionHolder
import com.aashu.natalks.data.repository.IceServerRepository
import com.aashu.natalks.data.signaling.CallAnswerSignal
import com.aashu.natalks.data.signaling.CallEndSignal
import com.aashu.natalks.data.signaling.CallOfferSignal
import com.aashu.natalks.data.signaling.CallRejectSignal
import com.aashu.natalks.data.signaling.IceCandidateSignal
import com.aashu.natalks.data.signaling.SignalMessage
import com.aashu.natalks.data.signaling.SignalingClient
import com.aashu.natalks.webrtc.WebRtcClient
import com.aashu.natalks.webrtc.createAnswerSuspend
import com.aashu.natalks.webrtc.createOfferSuspend
import com.aashu.natalks.webrtc.setLocalDescriptionSuspend
import com.aashu.natalks.webrtc.setRemoteDescriptionSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.UUID

private const val TAG = "CallController"
private const val STREAM_ID = "natalks_stream"
private const val ENDED_SCREEN_LINGER_MS = 1500L

class CallController(
    private val appContext: Context,
    private val webRtcClient: WebRtcClient,
    private val signalingClient: SignalingClient,
    private val iceServerRepository: IceServerRepository,
    private val sessionHolder: SessionHolder,
    private val contactNameCache: ContactNameCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted

    private val _isCameraOff = MutableStateFlow(false)
    val isCameraOff: StateFlow<Boolean> = _isCameraOff

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private val ringtonePlayer = RingtonePlayer(appContext)

    private val audioManager: AudioManager
        get() = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        scope.launch {
            signalingClient.incomingSignals.collect { signal -> onSignalReceived(signal) }
        }
        scope.launch {
            _callState.collect { state ->
                if (state is CallState.IncomingRinging) ringtonePlayer.start() else ringtonePlayer.stop()
            }
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
    }

    private val myUserId: String
        get() = sessionHolder.current()?.userId ?: error("Not logged in")

    fun placeCall(calleeId: String, calleeName: String) {
        if (_callState.value != CallState.Idle) return
        val callId = UUID.randomUUID().toString()
        _callState.value = CallState.OutgoingRinging(callId, calleeId, calleeName)

        scope.launch {
            try {
                val iceServers = iceServerRepository.fetchIceServers()
                val pc = webRtcClient.createPeerConnection(iceServers, buildObserver(callId, calleeId))
                peerConnection = pc
                attachLocalTracks(pc)

                val offer = pc.createOfferSuspend()
                pc.setLocalDescriptionSuspend(offer)

                signalingClient.sendSignal(
                    CallOfferSignal(callId = callId, fromUserId = myUserId, toUserId = calleeId, sdp = offer.description)
                )
            } catch (e: Exception) {
                Log.e(TAG, "placeCall failed", e)
                cleanupAndEnd("ERROR")
            }
        }
    }

    fun acceptCall() {
        val ringing = _callState.value as? CallState.IncomingRinging ?: return

        scope.launch {
            try {
                val iceServers = iceServerRepository.fetchIceServers()
                val pc = webRtcClient.createPeerConnection(iceServers, buildObserver(ringing.callId, ringing.callerId))
                peerConnection = pc
                attachLocalTracks(pc)

                pc.setRemoteDescriptionSuspend(SessionDescription(SessionDescription.Type.OFFER, ringing.offerSdp))
                remoteDescriptionSet = true
                flushPendingCandidates(pc)

                val answer = pc.createAnswerSuspend()
                pc.setLocalDescriptionSuspend(answer)

                signalingClient.sendSignal(
                    CallAnswerSignal(callId = ringing.callId, fromUserId = myUserId, toUserId = ringing.callerId, sdp = answer.description)
                )

                _callState.value = CallState.Connected(ringing.callId, ringing.callerId, ringing.callerName)
            } catch (e: Exception) {
                Log.e(TAG, "acceptCall failed", e)
                cleanupAndEnd("ERROR")
            }
        }
    }

    fun declineCall() {
        val ringing = _callState.value as? CallState.IncomingRinging ?: return
        signalingClient.sendSignal(
            CallRejectSignal(callId = ringing.callId, fromUserId = myUserId, toUserId = ringing.callerId, reason = "DECLINED")
        )
        _callState.value = CallState.Idle
    }

    fun hangUp() {
        val callId = currentCallId() ?: return
        val peerId = currentPeerId()
        if (peerId != null) {
            signalingClient.sendSignal(
                CallEndSignal(callId = callId, fromUserId = myUserId, toUserId = peerId, reason = "HANGUP")
            )
        }
        cleanupAndEnd("HANGUP")
    }

    fun toggleMic() {
        val muted = !_isMicMuted.value
        _isMicMuted.value = muted
        webRtcClient.setLocalAudioEnabled(!muted)
    }

    fun toggleCamera() {
        val off = !_isCameraOff.value
        _isCameraOff.value = off
        webRtcClient.setLocalVideoEnabled(!off)
    }

    fun switchCamera() {
        webRtcClient.switchCamera()
    }

    fun toggleSpeaker() {
        val on = !_isSpeakerOn.value
        _isSpeakerOn.value = on
        audioManager.isSpeakerphoneOn = on
    }

    private fun attachLocalTracks(pc: PeerConnection) {
        val (videoTrack, audioTrack) = webRtcClient.createLocalTracks(appContext)
        localAudioTrack = audioTrack
        _localVideoTrack.value = videoTrack
        pc.addTrack(videoTrack, listOf(STREAM_ID))
        pc.addTrack(audioTrack, listOf(STREAM_ID))
    }

    private fun currentCallId(): String? = when (val s = _callState.value) {
        is CallState.OutgoingRinging -> s.callId
        is CallState.IncomingRinging -> s.callId
        is CallState.Connected -> s.callId
        else -> null
    }

    private fun currentPeerId(): String? = when (val s = _callState.value) {
        is CallState.OutgoingRinging -> s.calleeId
        is CallState.IncomingRinging -> s.callerId
        is CallState.Connected -> s.peerId
        else -> null
    }

    private suspend fun flushPendingCandidates(pc: PeerConnection) {
        pendingRemoteCandidates.forEach { pc.addIceCandidate(it) }
        pendingRemoteCandidates.clear()
    }

    private fun onSignalReceived(signal: SignalMessage) {
        when (signal) {
            is CallOfferSignal -> {
                if (_callState.value != CallState.Idle) return
                val callerName = contactNameCache.nameFor(signal.fromUserId)
                _callState.value = CallState.IncomingRinging(signal.callId, signal.fromUserId, callerName, signal.sdp)
            }
            is CallAnswerSignal -> {
                val outgoing = _callState.value as? CallState.OutgoingRinging ?: return
                if (outgoing.callId != signal.callId) return
                val pc = peerConnection ?: return
                scope.launch {
                    pc.setRemoteDescriptionSuspend(SessionDescription(SessionDescription.Type.ANSWER, signal.sdp))
                    remoteDescriptionSet = true
                    flushPendingCandidates(pc)
                    _callState.value = CallState.Connected(outgoing.callId, outgoing.calleeId, outgoing.calleeName)
                }
            }
            is IceCandidateSignal -> {
                if (signal.callId != currentCallId()) return
                val candidate = IceCandidate(signal.sdpMid, signal.sdpMLineIndex, signal.candidate)
                val pc = peerConnection
                if (pc != null && remoteDescriptionSet) {
                    pc.addIceCandidate(candidate)
                } else {
                    pendingRemoteCandidates.add(candidate)
                }
            }
            is CallEndSignal -> {
                if (signal.callId != currentCallId()) return
                cleanupAndEnd(signal.reason)
            }
            is CallRejectSignal -> {
                if (signal.callId != currentCallId()) return
                cleanupAndEnd(signal.reason)
            }
        }
    }

    private fun buildObserver(callId: String, peerId: String) = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            signalingClient.sendSignal(
                IceCandidateSignal(
                    callId = callId,
                    fromUserId = myUserId,
                    toUserId = peerId,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    candidate = candidate.sdp
                )
            )
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                _remoteVideoTrack.value = track
            }
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            if (newState == PeerConnection.IceConnectionState.FAILED) {
                cleanupAndEnd("ERROR")
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) = Unit
        override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) = Unit
    }

    private fun cleanupAndEnd(reason: String) {
        peerConnection?.close()
        peerConnection = null
        remoteDescriptionSet = false
        pendingRemoteCandidates.clear()
        webRtcClient.releaseCallResources()
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        _isMicMuted.value = false
        _isCameraOff.value = false

        _callState.value = CallState.Ended(reason)
        scope.launch {
            delay(ENDED_SCREEN_LINGER_MS)
            if (_callState.value is CallState.Ended) {
                _callState.value = CallState.Idle
            }
        }
    }
}
