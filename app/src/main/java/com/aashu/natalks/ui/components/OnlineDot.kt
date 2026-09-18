package com.aashu.natalks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aashu.natalks.ui.theme.OfflineGray
import com.aashu.natalks.ui.theme.OnlineGreen

@Composable
fun OnlineDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .background(if (online) OnlineGreen else OfflineGray, CircleShape)
    )
}
