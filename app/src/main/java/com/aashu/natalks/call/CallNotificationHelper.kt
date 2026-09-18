package com.aashu.natalks.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aashu.natalks.MainActivity

private const val CONNECTION_CHANNEL_ID = "natalks_connection"
private const val INCOMING_CALL_CHANNEL_ID = "natalks_incoming_call"
const val INCOMING_CALL_NOTIFICATION_ID = 1001
private const val CONNECTION_NOTIFICATION_ID = 1000

const val ACTION_ACCEPT_CALL = "com.aashu.natalks.ACTION_ACCEPT_CALL"
const val ACTION_DECLINE_CALL = "com.aashu.natalks.ACTION_DECLINE_CALL"

class CallNotificationHelper(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CONNECTION_CHANNEL_ID,
                    "Connection",
                    NotificationManager.IMPORTANCE_MIN
                ).apply { description = "Keeps you reachable for incoming calls" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    INCOMING_CALL_CHANNEL_ID,
                    "Incoming calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Incoming video call alerts" }
            )
        }
    }

    fun buildConnectionNotification(): Notification =
        NotificationCompat.Builder(context, CONNECTION_CHANNEL_ID)
            .setContentTitle("NaTalks")
            .setContentText("Ready to receive calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    fun connectionNotificationId(): Int = CONNECTION_NOTIFICATION_ID

    fun showIncomingCall(callerName: String, serviceClass: Class<*>) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acceptIntent = PendingIntent.getService(
            context, 1,
            Intent(context, serviceClass).setAction(ACTION_ACCEPT_CALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declineIntent = PendingIntent.getService(
            context, 2,
            Intent(context, serviceClass).setAction(ACTION_DECLINE_CALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
            .setContentTitle("Incoming video call")
            .setContentText(callerName)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(0, "Accept", acceptIntent)
            .addAction(0, "Decline", declineIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    fun clearIncomingCall() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(INCOMING_CALL_NOTIFICATION_ID)
    }
}
