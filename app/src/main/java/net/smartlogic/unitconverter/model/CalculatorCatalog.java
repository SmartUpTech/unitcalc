package net.smartlogic.unitconverter.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.graphy.integration.CalculationSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of implemented calculators and converters for navigation, explore, and favorites.
 */
public final class CalculatorCatalog {

    public static final String ID_BASIC = CalculationSnapshot.BASIC_CALCULATOR_ID;
    public static final String ID_CURRENCY = "currency";
    private static final String UNIT_PREFIX = "unit:";

    public enum Destination {
        BASIC_CALCULATOR,
        UNIT_CATEGORY,
        CURRENCY,
        EXPLORE,
        FAVORITES,
        SETTINGS,
        HISTORY,
        RATE_APP,
        SHARE_APP
    }

    public static final class Entry {
        public final String id;
        @StringRes public final int titleRes;
        @DrawableRes public final int iconRes;
        public final Destination destination;
        public final int unitCategoryId;
        public final int menuId;

        Entry(String id,
              @StringRes int titleRes,
              @DrawableRes int iconRes,
              Destination destination,
              int unitCategoryId,
              int menuId) {
            this.id = id;
            this.titleRes = titleRes;
            this.iconRes = iconRes;
            this.destination = destination;
            this.unitCategoryId = unitCategoryId;
            this.menuId = menuId;
        }

        public boolean isFavoriteEligible() {
            return destination == Destination.BASIC_CALCULATOR
                    || destination == Destination.UNIT_CATEGORY
                    || destination == Destination.CURRENCY;
        }
    }

    public static final class Section {
        @StringRes public final int titleRes;
        public final List<Entry> entries;

        Section(@StringRes int titleRes, List<Entry> entries) {
            this.titleRes = titleRes;
            this.entries = entries;
        }
    }

    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static final List<Section> SECTIONS = new ArrayList<>();
    private static final List<Entry> FAVORITE_ELIGIBLE = new ArrayList<>();
    private static int nextMenuId = 1;

    static {
        List<Entry> calculators = new ArrayList<>();
        calculators.add(register(entry(ID_BASIC, R.string.calculator, R.drawable.ic_calculator,
                Destination.BASIC_CALCULATOR, -1)));
        SECTIONS.add(new Section(R.string.drawer_section_calculators, calculators));

        List<Entry> units = new ArrayList<>();
        units.add(register(entry(unitId(Conversion.LENGTH), R.string.length, R.drawable.ic_length,
                Destination.UNIT_CATEGORY, Conversion.LENGTH)));
        units.add(register(entry(unitId(Conversion.AREA), R.string.area, R.drawable.ic_area,
                Destination.UNIT_CATEGORY, Conversion.AREA)));
        units.add(register(entry(unitId(Conversion.TIME), R.string.time, R.drawable.ic_time,
                Destination.UNIT_CATEGORY, Conversion.TIME)));
        units.add(register(entry(unitId(Conversion.TEMPERATURE), R.string.temperature, R.drawable.ic_temperature,
                Destination.UNIT_CATEGORY, Conversion.TEMPERATURE)));
        units.add(register(entry(unitId(Conversion.MASS), R.string.mass, R.drawable.ic_mass,
                Destination.UNIT_CATEGORY, Conversion.MASS)));
        units.add(register(entry(unitId(Conversion.STORAGE), R.string.storage, R.drawable.ic_storage,
                Destination.UNIT_CATEGORY, Conversion.STORAGE)));
        units.add(register(entry(unitId(Conversion.FUEL), R.string.fuel_consumption, R.drawable.ic_fuel,
                Destination.UNIT_CATEGORY, Conversion.FUEL)));
        units.add(register(entry(unitId(Conversion.COOKING), R.string.cooking, R.drawable.ic_cooking,
                Destination.UNIT_CATEGORY, Conversion.COOKING)));
        units.add(register(entry(unitId(Conversion.SPEED), R.string.speed, R.drawable.ic_speed,
                Destination.UNIT_CATEGORY, Conversion.SPEED)));
        units.add(register(entry(unitId(Conversion.VOLUME), R.string.volume, R.drawable.ic_volume,
                Destination.UNIT_CATEGORY, Conversion.VOLUME)));
        units.add(register(entry(unitId(Conversion.POWER), R.string.power, R.drawable.ic_power,
                Destination.UNIT_CATEGORY, Conversion.POWER)));
        units.add(register(entry(unitId(Conversion.PRESSURE), R.string.pressure, R.drawable.ic_pressure,
                Destination.UNIT_CATEGORY, Conversion.PRESSURE)));
        units.add(register(entry(unitId(Conversion.ENERGY), R.string.energy, R.drawable.ic_energy,
                Destination.UNIT_CATEGORY, Conversion.ENERGY)));
        units.add(register(entry(unitId(Conversion.TORQUE), R.string.torque, R.drawable.ic_torque,
                Destination.UNIT_CATEGORY, Conversion.TORQUE)));
        SECTIONS.add(new Section(R.string.drawer_section_units, units));

        List<Entry> currency = new ArrayList<>();
        currency.add(register(entry(ID_CURRENCY, R.string.currency_converter, R.drawable.ic_currency_converter,
                Destination.CURRENCY, -1)));
        SECTIONS.add(new Section(R.string.drawer_section_currency, currency));

        List<Entry> general = new ArrayList<>();
        general.add(register(entry("explore", R.string.nav_explore, R.drawable.ic_explore, Destination.EXPLORE, -1)));
        general.add(register(entry("favorites", R.string.nav_favorites, R.drawable.ic_favorite, Destination.FAVORITES, -1)));
        general.add(register(entry("settings", R.string.title_activity_settings, R.drawable.ic_unit_converter,
                Destination.SETTINGS, -1)));
        general.add(register(entry("history", R.string.nav_history, R.drawable.ic_history, Destination.HISTORY, -1)));
        general.add(register(entry("rate", R.string.pref_rate_title, R.drawable.ic_explore, Destination.RATE_APP, -1)));
        general.add(register(entry("share", R.string.pref_share_title, R.drawable.share, Destination.SHARE_APP, -1)));
        SECTIONS.add(new Section(R.string.drawer_section_general, general));
    }

    private CalculatorCatalog() {
    }

    private static Entry entry(String id,
                               @StringRes int titleRes,
                               @DrawableRes int iconRes,
                               Destination destination,
                               int unitCategoryId) {
        return new Entry(id, titleRes, iconRes, destination, unitCategoryId, nextMenuId++);
    }

    private static Entry register(@NonNull Entry entry) {
        BY_ID.put(entry.id, entry);
        if (entry.isFavoriteEligible()) {
            FAVORITE_ELIGIBLE.add(entry);
        }
        return entry;
    }

    @NonNull
    public static List<Section> getSections() {
        return Collections.unmodifiableList(SECTIONS);
    }

    @NonNull
    public static List<Entry> getFavoriteEligibleEntries() {
        return Collections.unmodifiableList(FAVORITE_ELIGIBLE);
    }

    @Nullable
    public static Entry getById(@NonNull String id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static Entry getByMenuId(int menuId) {
        for (Entry entry : BY_ID.values()) {
            if (entry.menuId == menuId) {
                return entry;
            }
        }
        return null;
    }

    @NonNull
    public static String titleForId(@NonNull android.content.Context context, @NonNull String id) {
        Entry entry = getById(id);
        if (entry != null) {
            return context.getString(entry.titleRes);
        }
        return context.getString(R.string.calculator);
    }

    @NonNull
    public static String unitId(int categoryId) {
        return UNIT_PREFIX + categoryId;
    }
}
