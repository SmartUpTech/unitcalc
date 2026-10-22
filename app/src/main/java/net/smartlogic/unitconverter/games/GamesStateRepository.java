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

/** Durable daily completions and earned bonus entitlements; no ad/session counters. */
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

    private Set<String> readRecords(String key) {
        Set<String> records = new HashSet<>();
        try {
            Set<String> stored = preferences.getStringSet(key, java.util.Collections.emptySet());
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
        return recordCompletion(gameId, date, 0);
    }

    public WriteResult recordCompletion(String gameId, String date, int round) {
        String key = round == 1 ? "games_bonus_completions_v1" : COMPLETIONS_KEY;
        if (!GamesConfig.isGameId(gameId) || !GamesConfig.isDate(date)) return WriteResult.FAILED;
        synchronized (preferences) {
            Set<String> original = readRecords(key);
            Set<String> next = new HashSet<>(original);
            if (!next.add(date + ":" + gameId)) return WriteResult.DUPLICATE;
            if (preferences.edit().putStringSet(key, next).commit()) return WriteResult.NEW;
            // commit() may update memory even when disk writing fails. Allow a real retry.
            preferences.edit().putStringSet(key, original).apply();
            return WriteResult.FAILED;
        }
    }

    public boolean bonusUnlocked(String date) {
        synchronized (preferences) {
            try { return preferences.getStringSet("games_bonus_unlocks_v1", java.util.Collections.emptySet()).contains(date); }
            catch (ClassCastException error) { return false; }
        }
    }

    public boolean unlockBonus(String date) {
        if (!GamesConfig.isDate(date)) return false;
        synchronized (preferences) {
            Set<String> original;
            try { original = new HashSet<>(preferences.getStringSet("games_bonus_unlocks_v1", java.util.Collections.emptySet())); }
            catch (ClassCastException error) { original = new HashSet<>(); }
            Set<String> next = new HashSet<>(original);
            next.add(date);
            if (preferences.edit().putStringSet("games_bonus_unlocks_v1", next).commit()) return true;
            preferences.edit().putStringSet("games_bonus_unlocks_v1", original).apply();
            return false;
        }
    }

    public Map<String, String> completionsFor(String date) { return completionsFor(date, 0); }

    public Map<String, String> completionsFor(String date, int round) {
        synchronized (preferences) {
            Map<String, String> result = new TreeMap<>();
            for (String record : readRecords(round == 1 ? "games_bonus_completions_v1" : COMPLETIONS_KEY)) {
                if (record.startsWith(date + ":")) result.put(record.substring(11), date);
            }
            return result;
        }
    }
}
