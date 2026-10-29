package net.smartlogic.unitconverter.timer.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

class TimerAlarmPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        if (mediaPlayer != null) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            player.setDataSource(appContext, uri)
            player.isLooping = true
            player.setOnPreparedListener { prepared ->
                if (mediaPlayer === prepared) prepared.start()
            }
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) stop()
                true
            }
            player.prepareAsync()
        } catch (_: java.io.IOException) {
            stop()
        } catch (_: RuntimeException) {
            stop()
        }
    }

    fun stop() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        // release also stops playback, and is valid while prepareAsync is pending.
        player.setOnPreparedListener(null)
        player.setOnErrorListener(null)
        player.release()
    }
}
