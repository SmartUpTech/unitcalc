package net.smartlogic.unitconverter.helper;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.HashSet;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class RegionalPreferencesTest {
    private Preferences preferences;
    private SharedPreferences store;
    @Before public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = Preferences.getInstance(context);
        store = preferences.getPreferences();
        store.edit().clear().commit();
    }
    @Test public void freshInstallAndResetHaveExplicitDefaults() {
        preferences.migrateRegionalPreferences(false);
        RegionalProfile profile = preferences.getRegionalProfile();
        assertEquals("US", profile.region.code);
        assertEquals("USD", profile.currency);
        assertEquals("1,234,567.89", profile.numbers.format(new java.math.BigDecimal("1234567.89")));
        preferences.setNumberFormat(new NumberFormatSettings(',', " ", "indian", 8));
        preferences.applyRegion("US");
        assertEquals(8, preferences.getRegionalProfile().numbers.decimalPlaces);
        preferences.applyRegion("DE");
        assertEquals("EUR", preferences.getRegionalProfile().currency);
        assertEquals(4, preferences.getRegionalProfile().numbers.decimalPlaces);
        assertEquals("1.234.567,89", preferences.getRegionalProfile().numbers.format(new java.math.BigDecimal("1234567.89")));
    }
    @Test public void migrationPreservesAbsentKeysAndDoesNotRepeat() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CANADA);
            preferences.migrateRegionalPreferences(true);
            assertEquals("CAD", preferences.getRegionalProfile().currency);
            assertEquals("", preferences.getRegionalProfile().numbers.groupSeparator);
            preferences.applyRegion("IN");
            preferences.migrateRegionalPreferences(true);
            assertEquals("IN", preferences.getRegionalProfile().region.code);
            assertEquals("12,34,567.89", preferences.getRegionalProfile().numbers.format(new java.math.BigDecimal("1234567.89")));
        } finally { Locale.setDefault(previous); }
    }
    @Test public void resetPreservesSavedAmountsAndConverterChoices() {
        preferences.migrateRegionalPreferences(false);
        store.edit().putString("emi_scenario", "CAD|1000.01").putInt(Preferences.PREFS_CURR_FROM_INDEX, 7).apply();
        preferences.setNumberFormat(new NumberFormatSettings('.', "", "indian", 10));
        preferences.setPreferredCurrency("INR");
        preferences.resetRegionDefaults();
        assertEquals("CAD|1000.01", store.getString("emi_scenario", ""));
        assertEquals(7, preferences.getFromCurrencyIndex());
        assertEquals("USD", preferences.getRegionalProfile().currency);
        assertFalse(preferences.getRegionalProfile().customNumbers);
    }
    @Test public void corruptSettingsDoNotClearUnrelatedSettings() {
        store.edit().putInt(Preferences.PREFS_DECIMAL_SEPARATOR, 12)
                .putString(Preferences.PREFS_NUMBER_OF_DECIMALS, "200")
                .putString(Preferences.PREFS_PREFERRED_CURRENCY, "invalid")
                .putString("saved", "42.5").apply();
        preferences.migrateRegionalPreferences(true);
        assertEquals(4, preferences.getRegionalProfile().numbers.decimalPlaces);
        assertEquals("USD", preferences.getRegionalProfile().currency);
        assertEquals("42.5", store.getString("saved", ""));
    }
    @Test public void registriesHaveStableUniqueSupportedIdentities() {
        HashSet<String> codes = new HashSet<>();
        Context context = ApplicationProvider.getApplicationContext();
        for (RegionDefinition r : RegionRegistry.ALL) {
            assertTrue(r.code, codes.add(r.code));
            assertFalse(context.getString(r.nameResource).isEmpty());
            assertNotNull(PreferredCurrency.find(context, r.currency));
            assertNotNull(r.defaults());
        }
        assertEquals(35, SupportedLanguage.ALL.size());
        for (SupportedLanguage language : SupportedLanguage.ALL)
            assertNotNull(RegionRegistry.find(language.suggestedRegion));
    }
    @Test public void suggestionSurvivesRecreationAndSharesSpanishSuppression() {
        RegionDefinition us = RegionRegistry.find("US");
        RegionSuggestion state = new RegionSuggestion(store);
        state.observe(SupportedLanguage.resolve("en-US"), us);
        assertNull(state.pending(us));
        state.changed(SupportedLanguage.resolve("es-MX"), us);
        state = new RegionSuggestion(store);
        assertEquals("MX", state.pending(us).suggestedRegion);
        state.keep(us);
        state.changed(SupportedLanguage.resolve("es-ES"), us);
        assertNull(state.pending(us));
        state.changed(SupportedLanguage.resolve("de-DE"), us);
        assertEquals("DE", state.pending(us).suggestedRegion);
        state.changed(SupportedLanguage.resolve("hi-IN"), us);
        assertEquals("IN", state.pending(us).suggestedRegion);
    }
    @Test public void commonLanguageMatchesDoNotSuggest() {
        RegionSuggestion state = new RegionSuggestion(store);
        for (String code : new String[]{"US", "IN", "CA", "AU"}) {
            RegionDefinition region = RegionRegistry.find(code);
            state.changed(SupportedLanguage.resolve("en-US"), region);
            assertNull(code, state.pending(region));
        }
        for (String[] pair : new String[][]{{"fr-FR","CA"},{"pt-BR","PT"},{"es-MX","ES"},{"es-ES","MX"}}) {
            RegionDefinition region = RegionRegistry.find(pair[1]);
            state.changed(SupportedLanguage.resolve(pair[0]), region);
            assertNull(state.pending(region));
        }
    }
}
