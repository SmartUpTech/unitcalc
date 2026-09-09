package net.smartlogic.unitconverter.timer.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

class TimerAlarmPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        stop()
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(appContext, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.run {
            runCatching {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        }
        mediaPlayer = null
    }
}
