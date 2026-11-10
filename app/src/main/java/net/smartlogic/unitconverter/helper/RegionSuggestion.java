package net.smartlogic.unitconverter.helper;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Durable transition state, not an alternative app-language preference. */
public final class RegionSuggestion {
    public static final String PENDING = "regional_pending";
    private static final String OBSERVED = "regional_observed_translation";
    private static final String DECLINED = "regional_declined_pairs";
    private final SharedPreferences prefs;

    public RegionSuggestion(SharedPreferences prefs) { this.prefs = prefs; }

    public void observe(SupportedLanguage selected, RegionDefinition region) {
        Map<String, ?> values = prefs.getAll();
        Object previous = values.get(OBSERVED);
        if (selected.tag.equals(previous)) return;
        // First observation establishes a baseline; ordinary launches never invent prompts.
        if (previous == null) {
            prefs.edit().putString(OBSERVED, selected.tag).apply();
            return;
        }
        changed(selected, region);
    }

    /** Persist before requesting AppCompat recreation. */
    public void changed(SupportedLanguage selected, RegionDefinition region) {
        SharedPreferences.Editor edit = prefs.edit().putString(OBSERVED, selected.tag).remove(PENDING);
        if (!region.commonlyUses(selected.language) && !region.code.equals(selected.suggestedRegion)
                && !declined().contains(selected.language + "/" + region.code))
            edit.putString(PENDING, selected.tag + "/" + region.code);
        edit.apply();
    }

    public SupportedLanguage pending(RegionDefinition region) {
        Object raw = prefs.getAll().get(PENDING);
        if (!(raw instanceof String)) return null;
        String[] pair = ((String) raw).split("/");
        if (pair.length != 2 || !pair[1].equals(region.code)) return null;
        SupportedLanguage language = SupportedLanguage.resolve(pair[0]);
        return language.tag.equals(pair[0]) ? language : null;
    }

    public void keep(RegionDefinition region) {
        SupportedLanguage pending = pending(region);
        SharedPreferences.Editor edit = prefs.edit().remove(PENDING);
        if (pending != null) {
            Set<String> pairs = declined();
            pairs.add(pending.language + "/" + region.code);
            edit.putStringSet(DECLINED, pairs);
        }
        edit.apply();
    }

    private Set<String> declined() {
        try { return new HashSet<>(prefs.getStringSet(DECLINED, java.util.Collections.emptySet())); }
        catch (ClassCastException invalid) { return new HashSet<>(); }
    }
}
