package com.aashu.natalks.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aashu.natalks.call.CallController
import com.aashu.natalks.call.CallState
import org.webrtc.EglBase

@Composable
fun CallScreen(
    callController: CallController,
    eglBase: EglBase,
    onCallFinished: () -> Unit
) {
    val state by callController.callState.collectAsStateWithLifecycle()
    val localVideoTrack by callController.localVideoTrack.collectAsStateWithLifecycle()
    val remoteVideoTrack by callController.remoteVideoTrack.collectAsStateWithLifecycle()
    val isMicMuted by callController.isMicMuted.collectAsStateWithLifecycle()
    val isCameraOff by callController.isCameraOff.collectAsStateWithLifecycle()
    val isSpeakerOn by callController.isSpeakerOn.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is CallState.Idle) {
            onCallFinished()
        }
    }

    when (val s = state) {
        is CallState.OutgoingRinging -> OutgoingCallContent(
            calleeName = s.calleeName,
            onCancel = { callController.hangUp() }
        )
        is CallState.IncomingRinging -> IncomingCallContent(
            callerName = s.callerName,
            onAccept = { callController.acceptCall() },
            onDecline = { callController.declineCall() }
        )
        is CallState.Connected -> InCallContent(
            peerName = s.peerName,
            eglBase = eglBase,
            localVideoTrack = localVideoTrack,
            remoteVideoTrack = remoteVideoTrack,
            isMicMuted = isMicMuted,
            isCameraOff = isCameraOff,
            isSpeakerOn = isSpeakerOn,
            onToggleMic = { callController.toggleMic() },
            onToggleCamera = { callController.toggleCamera() },
            onToggleSpeaker = { callController.toggleSpeaker() },
            onHangUp = { callController.hangUp() }
        )
        is CallState.Ended -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(endedMessage(s.reason), style = MaterialTheme.typography.titleLarge)
        }
        CallState.Idle -> Unit
    }
}

private fun endedMessage(reason: String): String = when (reason) {
    "HANGUP" -> "Call ended"
    "DECLINED" -> "Call declined"
    "BUSY" -> "They're on another call"
    "TARGET_OFFLINE" -> "They're offline"
    "PEER_DISCONNECTED" -> "Connection lost"
    else -> "Call ended"
}
