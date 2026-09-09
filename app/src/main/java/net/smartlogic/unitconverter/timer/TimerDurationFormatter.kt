package net.smartlogic.unitconverter.timer

import net.smartlogic.unitconverter.timer.model.RecentTimer
import net.smartlogic.unitconverter.timer.model.TimerDraft

object TimerDurationFormatter {
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun formatCountdown(remainingMs: Long): String = formatDuration(remainingMs)

    fun normalizeLabel(label: String): String = label.trim().take(MAX_LABEL_LENGTH)

    fun displayLabel(label: String, defaultLabel: String): String {
        val normalized = normalizeLabel(label)
        return normalized.ifEmpty { defaultLabel }
    }

    fun draftFromDuration(durationMs: Long, label: String): TimerDraft {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = (totalSeconds / 3600L).toInt().coerceAtMost(MAX_HOURS)
        val minutes = ((totalSeconds % 3600L) / 60L).toInt().coerceIn(0, 59)
        val seconds = (totalSeconds % 60L).toInt().coerceIn(0, 59)
        return TimerDraft(hours, minutes, seconds, normalizeLabel(label))
    }

    fun recentKey(recent: RecentTimer): String = "${recent.durationMs}|${normalizeLabel(recent.label)}"

    const val MAX_LABEL_LENGTH = 25
    const val MAX_HOURS = 99
    const val MAX_RECENTS = 20
}
