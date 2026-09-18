package com.aashu.natalks.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashu.natalks.ui.components.GlassIconButton
import com.aashu.natalks.ui.components.PulsingAvatar
import com.aashu.natalks.ui.theme.DangerRed
import com.aashu.natalks.ui.theme.OnlineGreen

@Composable
fun IncomingCallContent(callerName: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PulsingAvatar(name = callerName, ringColor = OnlineGreen, size = 120.dp)
        Spacer(Modifier.height(24.dp))
        Text(callerName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Incoming video call…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(64.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(
                icon = Icons.Filled.CallEnd,
                contentDescription = "Decline",
                containerColor = DangerRed,
                size = 64.dp,
                onClick = onDecline
            )
            GlassIconButton(
                icon = Icons.Filled.Call,
                contentDescription = "Accept",
                containerColor = OnlineGreen,
                size = 64.dp,
                onClick = onAccept
            )
        }
    }
}
