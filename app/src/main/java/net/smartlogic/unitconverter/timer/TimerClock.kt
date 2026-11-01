package net.smartlogic.unitconverter.timer

import android.os.SystemClock

interface TimerClock {
    fun elapsedRealtime(): Long
    fun currentTimeMillis(): Long
}

object SystemTimerClock : TimerClock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class FakeTimerClock(
    var elapsedRealtimeMs: Long = 0L,
    var currentTimeMillisMs: Long = 0L,
) : TimerClock {
    override fun elapsedRealtime(): Long = elapsedRealtimeMs
    override fun currentTimeMillis(): Long = currentTimeMillisMs

    fun advance(ms: Long) {
        elapsedRealtimeMs += ms
        currentTimeMillisMs += ms
    }
}
