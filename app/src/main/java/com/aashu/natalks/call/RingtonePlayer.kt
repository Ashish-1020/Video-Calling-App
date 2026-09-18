package com.aashu.natalks.call

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

private val VIBRATION_PATTERN = longArrayOf(0, 500, 500)

class RingtonePlayer(private val context: Context) {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    fun start() {
        stop()

        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        ringtone = uri?.let { RingtoneManager.getRingtone(context, it) }?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }

        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator = vib
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
        } else {
            @Suppress("DEPRECATION")
            vib?.vibrate(VIBRATION_PATTERN, 0)
        }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }
}
