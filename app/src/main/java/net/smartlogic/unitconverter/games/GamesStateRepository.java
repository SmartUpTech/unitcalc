package net.smartlogic.unitconverter.games;

import android.content.Context;
import android.content.SharedPreferences;

import net.smartlogic.unitconverter.helper.Preferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Only completion records are persisted, never ad/session counters. */
public final class GamesStateRepository {
    public static final String COMPLETIONS_KEY = "games_completion_records_v1";
    private static GamesStateRepository instance;
    private final SharedPreferences preferences;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public enum WriteResult { NEW, DUPLICATE, FAILED }

    public static synchronized GamesStateRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GamesStateRepository(Preferences.getInstance(context).getPreferences());
        }
        return instance;
    }

    public GamesStateRepository(SharedPreferences preferences) { this.preferences = preferences; }

    public void execute(Runnable work) { worker.execute(work); }

    private Set<String> readRecords() {
        Set<String> records = new HashSet<>();
        try {
            Set<String> stored = preferences.getStringSet(COMPLETIONS_KEY, java.util.Collections.emptySet());
            if (stored != null) for (String record : stored) {
                if (record != null && record.length() > 11 && record.charAt(10) == ':'
                        && GamesConfig.isDate(record.substring(0, 10))
                        && GamesConfig.isGameId(record.substring(11))) records.add(record);
            }
        } catch (ClassCastException corrupt) {
            // A wrong-type value does not affect existing calculation/theme preferences.
        }
        return records;
    }

    /** Run on worker. SharedPreferences commit guarantees persistence before an ad attempt. */
    public WriteResult recordCompletion(String gameId, String date) {
        if (!GamesConfig.isGameId(gameId) || !GamesConfig.isDate(date)) return WriteResult.FAILED;
        synchronized (preferences) {
            Set<String> original = readRecords();
            Set<String> next = new HashSet<>(original);
            if (!next.add(date + ":" + gameId)) return WriteResult.DUPLICATE;
            if (preferences.edit().putStringSet(COMPLETIONS_KEY, next).commit()) return WriteResult.NEW;
            // commit() may update memory even when disk writing fails. Allow a real retry.
            preferences.edit().putStringSet(COMPLETIONS_KEY, original).apply();
            return WriteResult.FAILED;
        }
    }

    public Map<String, String> completionsFor(String date) {
        synchronized (preferences) {
            Map<String, String> result = new TreeMap<>();
            for (String record : readRecords()) {
                if (record.startsWith(date + ":")) result.put(record.substring(11), date);
            }
            return result;
        }
    }
}
