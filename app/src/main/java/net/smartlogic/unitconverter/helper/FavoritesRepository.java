package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FavoritesRepository {

    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_IDS = "favorite_calculator_ids";

    private static FavoritesRepository instance;

    private final SharedPreferences preferences;

    private FavoritesRepository(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static synchronized FavoritesRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new FavoritesRepository(context);
        }
        return instance;
    }

    @NonNull
    public Set<String> getFavoriteIds() {
        return new HashSet<>(preferences.getStringSet(KEY_IDS, Collections.emptySet()));
    }

    public boolean isFavorite(@NonNull String calculatorId) {
        return getFavoriteIds().contains(calculatorId);
    }

    public void setFavorite(@NonNull String calculatorId, boolean favorite) {
        Set<String> ids = new HashSet<>(getFavoriteIds());
        if (favorite) {
            ids.add(calculatorId);
        } else {
            ids.remove(calculatorId);
        }
        preferences.edit().putStringSet(KEY_IDS, ids).apply();
    }

    public boolean toggleFavorite(@NonNull String calculatorId) {
        boolean next = !isFavorite(calculatorId);
        setFavorite(calculatorId, next);
        return next;
    }
}
