package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Objects;

/** Saves an immutable valid result after a foreground pause in editing. */
public final class IdleHistoryRecorder implements DefaultLifecycleObserver {
    public static final long IDLE_DELAY_MS = 3_000L;
    private static final String STATE = "idle_history_last_saved";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Fragment fragment;
    private final Context context;
    private Entry candidate;
    private Entry lastSaved;
    private final Runnable save = this::saveNow;

    private record Entry(String expression, String result, String tool) { }

    public IdleHistoryRecorder(Fragment fragment, Bundle state) {
        this.fragment = fragment;
        context = fragment.requireContext().getApplicationContext();
        if (state != null) {
            String[] values = state.getStringArray(STATE);
            if (values != null && values.length == 3) lastSaved = new Entry(values[0], values[1], values[2]);
        }
        fragment.getLifecycle().addObserver(this);
    }

    public void update(String expression, String result, String tool) {
        Entry next = new Entry(expression, result, tool);
        if (next.equals(candidate)) return;
        candidate = next;
        visibilityChanged();
    }

    /** Invalid/empty input must cancel the previous valid result's countdown. */
    public void invalidate() {
        handler.removeCallbacks(save);
        candidate = null;
    }

    /** Opening an existing history row is a preview, not a new calculation. */
    public void markCurrentAsSaved() {
        handler.removeCallbacks(save);
        lastSaved = candidate;
    }

    public void visibilityChanged() {
        handler.removeCallbacks(save);
        if (isActive() && candidate != null && !candidate.equals(lastSaved)) {
            handler.postDelayed(save, IDLE_DELAY_MS);
        }
    }

    private boolean isActive() {
        for (Fragment current = fragment; current != null; current = current.getParentFragment()) {
            if (!current.isResumed() || current.isHidden() || current.getView() == null) return false;
        }
        return true;
    }

    public void saveNow() {
        handler.removeCallbacks(save);
        Entry entry = candidate;
        if (!isActive() || entry == null || Objects.equals(entry, lastSaved)) return;
        lastSaved = entry;
        DatabaseHelper.executeHistoryWork(() -> {
            try (DatabaseHelper database = new DatabaseHelper(context)) {
                database.addHistory(entry.expression(), entry.result(), entry.tool());
                UsageAnalytics.event("calculation_saved", "tool_id", entry.tool());
                handler.post(() -> InAppReview.recordUse(context));
            } catch (RuntimeException error) {
                Log.e("IdleHistoryRecorder", "Unable to save calculation history", error);
                handler.post(() -> { if (entry.equals(lastSaved)) lastSaved = null; });
            }
        });
    }

    public void saveState(Bundle state) {
        if (lastSaved != null) state.putStringArray(STATE,
                new String[]{lastSaved.expression(), lastSaved.result(), lastSaved.tool()});
    }

    @Override public void onResume(@NonNull LifecycleOwner owner) { visibilityChanged(); }
    @Override public void onPause(@NonNull LifecycleOwner owner) { handler.removeCallbacks(save); }
    @Override public void onDestroy(@NonNull LifecycleOwner owner) { invalidate(); }
}
