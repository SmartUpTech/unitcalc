package net.smartlogic.unitconverter.games;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Set;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class GamesStateRepositoryTest {
    private SharedPreferences preferences;
    private GamesStateRepository repository;
    private static final String DAY = "2026-10-06";

    @Before public void setup() {
        preferences = RuntimeEnvironment.getApplication().getSharedPreferences("games_test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        repository = new GamesStateRepository(preferences);
    }

    @Test public void duplicatesSurviveRepositoryRecreationAndClockRollback() {
        assertEquals(GamesStateRepository.WriteResult.NEW, repository.recordCompletion("future_game", DAY));
        assertEquals(GamesStateRepository.WriteResult.NEW, repository.recordCompletion("future_game", "2026-10-07"));
        GamesStateRepository recreated = new GamesStateRepository(preferences);
        assertEquals(GamesStateRepository.WriteResult.DUPLICATE, recreated.recordCompletion("future_game", DAY));
        assertEquals(Map.of("future_game", DAY), recreated.completionsFor(DAY));
        assertTrue(recreated.completionsFor("2026-10-08").isEmpty());
        assertEquals(1, preferences.getAll().size()); // No persisted session/ad counters.
    }

    @Test public void corruptStorageDoesNotChangeOtherPreferences() {
        preferences.edit().putString(GamesStateRepository.COMPLETIONS_KEY, "corrupt")
                .putString("selected_theme", "existing_theme").commit();
        assertTrue(repository.completionsFor(DAY).isEmpty());
        assertEquals(GamesStateRepository.WriteResult.NEW, repository.recordCompletion("new_game", DAY));
        assertEquals("existing_theme", preferences.getString("selected_theme", ""));
        preferences.edit().putStringSet(GamesStateRepository.COMPLETIONS_KEY,
                Set.of("bad", DAY + ":../game", DAY + ":valid_game")).commit();
        assertEquals(Map.of("valid_game", DAY), repository.completionsFor(DAY));
    }

    @Test public void concurrentCallbacksHaveExactlyOneNewCompletion() throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Future<GamesStateRepository.WriteResult>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) results.add(executor.submit(() -> repository.recordCompletion("same_game", DAY)));
            int count = 0;
            for (var result : results) if (result.get() == GamesStateRepository.WriteResult.NEW) count++;
            assertEquals(1, count);
        } finally { executor.shutdownNow(); }
    }

    @Test public void diskFailureDoesNotGrantACompletionAndCanBeRetried() {
        AtomicBoolean fail = new AtomicBoolean(true);
        SharedPreferences failingStore = (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("edit")) return method.invoke(preferences, args);
                    SharedPreferences.Editor real = preferences.edit();
                    return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                            new Class<?>[]{SharedPreferences.Editor.class}, (editorProxy, editorMethod, editorArgs) -> {
                                if (editorMethod.getName().equals("commit") && fail.get()) {
                                    real.apply(); // Simulate commit's in-memory update before disk failure.
                                    return false;
                                }
                                Object value = editorMethod.invoke(real, editorArgs);
                                return value instanceof SharedPreferences.Editor ? editorProxy : value;
                            });
                });
        GamesStateRepository failingRepository = new GamesStateRepository(failingStore);
        assertEquals(GamesStateRepository.WriteResult.FAILED, failingRepository.recordCompletion("valid_game", DAY));
        assertTrue(failingRepository.completionsFor(DAY).isEmpty());
        fail.set(false);
        assertEquals(GamesStateRepository.WriteResult.NEW, failingRepository.recordCompletion("valid_game", DAY));
    }
}
