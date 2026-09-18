package com.aashu.natalks.call

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.aashu.natalks.NaTalksApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallSignalingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificationHelper: CallNotificationHelper

    override fun onCreate() {
        super.onCreate()
        val container = (application as NaTalksApp).container
        notificationHelper = container.callNotificationHelper
        startForeground(notificationHelper.connectionNotificationId(), notificationHelper.buildConnectionNotification())

        val session = container.sessionHolder.current()
        if (session != null) {
            container.signalingClient.connect(session.token)
        }

        scope.launch {
            container.callController.callState.collect { state ->
                when (state) {
                    is CallState.IncomingRinging -> notificationHelper.showIncomingCall(state.callerName, CallSignalingService::class.java)
                    else -> notificationHelper.clearIncomingCall()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as NaTalksApp).container
        when (intent?.action) {
            ACTION_ACCEPT_CALL -> {
                container.callController.acceptCall()
                startActivity(
                    Intent(this, com.aashu.natalks.MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
            ACTION_DECLINE_CALL -> container.callController.declineCall()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        (application as NaTalksApp).container.signalingClient.disconnect()
    }
}
