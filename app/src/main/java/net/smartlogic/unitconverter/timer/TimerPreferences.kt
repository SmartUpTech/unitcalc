package net.smartlogic.unitconverter.timer

import android.content.Context
import android.content.SharedPreferences
import net.smartlogic.unitconverter.timer.model.RecentTimer
import net.smartlogic.unitconverter.timer.model.TimerDraft
import net.smartlogic.unitconverter.timer.model.TimerPhase
import org.json.JSONArray
import org.json.JSONObject

class TimerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadRecents(): List<RecentTimer> {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        RecentTimer(
                            durationMs = item.getLong("durationMs"),
                            label = item.optString("label", ""),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecents(recents: List<RecentTimer>) {
        val array = JSONArray()
        recents.take(TimerDurationFormatter.MAX_RECENTS).forEach { recent ->
            array.put(
                JSONObject()
                    .put("durationMs", recent.durationMs)
                    .put("label", recent.label),
            )
        }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    fun saveActiveSnapshot(phase: TimerPhase, completionToken: Long) {
        val editor = prefs.edit()
        when (phase) {
            is TimerPhase.Setting -> editor.remove(KEY_ACTIVE)
            is TimerPhase.Running -> {
                editor.putString(
                    KEY_ACTIVE,
                    JSONObject()
                        .put("state", "running")
                        .put("label", phase.label)
                        .put("originalDurationMs", phase.originalDurationMs)
                        .put("endElapsedRealtime", phase.endElapsedRealtime)
                        .put("completionToken", completionToken)
                        .toString(),
                )
            }
            is TimerPhase.Paused -> {
                editor.putString(
                    KEY_ACTIVE,
                    JSONObject()
                        .put("state", "paused")
                        .put("label", phase.label)
                        .put("originalDurationMs", phase.originalDurationMs)
                        .put("remainingMs", phase.remainingMs)
                        .put("completionToken", completionToken)
                        .toString(),
                )
            }
            is TimerPhase.Completed -> {
                editor.putString(
                    KEY_ACTIVE,
                    JSONObject()
                        .put("state", "completed")
                        .put("label", phase.label)
                        .put("originalDurationMs", phase.originalDurationMs)
                        .put("completionToken", phase.completionToken)
                        .toString(),
                )
            }
        }.apply()
    }

    fun loadActiveSnapshot(): ActiveSnapshot? {
        val raw = prefs.getString(KEY_ACTIVE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            ActiveSnapshot(
                state = json.getString("state"),
                label = json.optString("label", ""),
                originalDurationMs = json.getLong("originalDurationMs"),
                endElapsedRealtime = json.optLong("endElapsedRealtime", 0L),
                remainingMs = json.optLong("remainingMs", 0L),
                completionToken = json.optLong("completionToken", 0L),
            )
        }.getOrNull()
    }

    fun saveSettingDraft(draft: TimerDraft) {
        prefs.edit()
            .putInt(KEY_DRAFT_HOURS, draft.hours)
            .putInt(KEY_DRAFT_MINUTES, draft.minutes)
            .putInt(KEY_DRAFT_SECONDS, draft.seconds)
            .putString(KEY_DRAFT_LABEL, draft.label)
            .apply()
    }

    fun loadSettingDraft(): TimerDraft {
        return TimerDraft(
            hours = prefs.getInt(KEY_DRAFT_HOURS, 0),
            minutes = prefs.getInt(KEY_DRAFT_MINUTES, 0),
            seconds = prefs.getInt(KEY_DRAFT_SECONDS, 0),
            label = prefs.getString(KEY_DRAFT_LABEL, "") ?: "",
        )
    }

    fun clearActiveSnapshot() {
        prefs.edit().remove(KEY_ACTIVE).apply()
    }

    data class ActiveSnapshot(
        val state: String,
        val label: String,
        val originalDurationMs: Long,
        val endElapsedRealtime: Long,
        val remainingMs: Long,
        val completionToken: Long,
    )

    companion object {
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_RECENTS = "recents"
        private const val KEY_ACTIVE = "active_snapshot"
        private const val KEY_DRAFT_HOURS = "draft_hours"
        private const val KEY_DRAFT_MINUTES = "draft_minutes"
        private const val KEY_DRAFT_SECONDS = "draft_seconds"
        private const val KEY_DRAFT_LABEL = "draft_label"
    }
}
