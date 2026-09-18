package com.aashu.natalks.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aashu.natalks.ui.components.GlassIconButton
import com.aashu.natalks.ui.theme.DangerRed
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

private val PIP_WIDTH = 110.dp
private val PIP_HEIGHT = 150.dp

private val NeutralGlass = Color.White.copy(alpha = 0.28f)

@Composable
fun InCallContent(
    peerName: String,
    eglBase: EglBase,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    isMicMuted: Boolean,
    isCameraOff: Boolean,
    isSpeakerOn: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangUp: () -> Unit
) {
    val callStartTimeMs = remember { System.currentTimeMillis() }
    var showInfoDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val pipWidthPx = with(density) { PIP_WIDTH.toPx() }
        val pipHeightPx = with(density) { PIP_HEIGHT.toPx() }
        val startPaddingPx = with(density) { 16.dp.toPx() }

        var offsetX by remember { mutableStateOf(startPaddingPx) }
        var offsetY by remember { mutableStateOf((containerHeightPx - pipHeightPx) / 2f) }

        VideoRendererView(
            eglBase = eglBase,
            videoTrack = remoteVideoTrack,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(width = PIP_WIDTH, height = PIP_HEIGHT)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.DarkGray)
                .pointerInput(containerWidthPx, containerHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, containerWidthPx - pipWidthPx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, containerHeightPx - pipHeightPx)
                    }
                }
        ) {
            VideoRendererView(
                eglBase = eglBase,
                videoTrack = localVideoTrack,
                mirror = true,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("You", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "End call",
                    containerColor = NeutralGlass,
                    onClick = onHangUp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(peerName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    CallDurationText(startTimeMs = callStartTimeMs, color = Color.White.copy(alpha = 0.85f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        icon = Icons.Filled.Notifications,
                        contentDescription = null,
                        containerColor = NeutralGlass,
                        onClick = { }
                    )
                    GlassIconButton(
                        icon = Icons.Filled.Menu,
                        contentDescription = "Call info",
                        containerColor = NeutralGlass,
                        onClick = { showInfoDialog = true }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                contentDescription = "Toggle camera",
                containerColor = NeutralGlass,
                onClick = onToggleCamera
            )
            GlassIconButton(
                icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                contentDescription = "Toggle speaker",
                containerColor = NeutralGlass,
                onClick = onToggleSpeaker
            )
            GlassIconButton(
                icon = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = "Toggle microphone",
                containerColor = NeutralGlass,
                onClick = onToggleMic
            )
            GlassIconButton(
                icon = Icons.Filled.CallEnd,
                contentDescription = "End call",
                containerColor = DangerRed,
                size = 56.dp,
                onClick = onHangUp
            )
        }
    }

    if (showInfoDialog) {
        val snapshotSeconds = ((System.currentTimeMillis() - callStartTimeMs) / 1000).toInt()
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
            title = { Text("Call with $peerName") },
            text = { Text("Duration: ${formatDuration(snapshotSeconds)}") }
        )
    }
}

@Composable
private fun CallDurationText(startTimeMs: Long, color: Color) {
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(startTimeMs) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            delay(1000)
        }
    }
    Text(formatDuration(elapsedSeconds), color = color, style = MaterialTheme.typography.bodyMedium)
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
