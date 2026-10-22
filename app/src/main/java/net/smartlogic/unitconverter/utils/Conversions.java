package net.smartlogic.unitconverter.utils;

import static net.smartlogic.unitconverter.model.Unit.*;

import net.smartlogic.unitconverter.R;
import net.smartlogic.unitconverter.model.Conversion;
import net.smartlogic.unitconverter.model.TemperatureUnit;
import net.smartlogic.unitconverter.model.Unit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all conversion info
 */
public final class Conversions {

    private static Conversions mInstance = null;
    private final Map<Integer, Conversion> mConversions = new HashMap<>();

    /**
     * Get instance of Conversions objects, which contains mapping of type and Conversion object
     *
     * @return Conversions mInstance
     */
    public static Conversions getInstance() {
        //Create singleton to contain all conversions
        if (mInstance == null) {
            mInstance = new Conversions();
        }
        return mInstance;
    }

    private Conversions() {
        //Fill conversions HashMap
        getAreaConversions();
        getCookingConversions();
        getStorageConversions();
        getEnergyConversions();
        getFuelConversions();
        getLengthConversions();
        getMassConversions();
        getPowerConversions();
        getPressureConversions();
        getSpeedConversions();
        getTemperatureConversions();
        getTimeConversions();
        getTorqueConversions();
        getVolumeConversions();
        getAngleConversions();
    }

    /**
     * Get Conversion object by its id
     *
     * @param id id of conversion
     * @return Conversion object
     */
    public Conversion getById(int id) {
        return mConversions.get(id);
    }

    
    /** Category IDs come from the registry; currency has its own converter. */
    public List<Integer> getCategoryIds() {
        List<Integer> ids = new ArrayList<>(mConversions.keySet());
        java.util.Collections.sort(ids);
        return java.util.Collections.unmodifiableList(ids);
    }

    private void addConversion(int id, Conversion conversion) {
        mConversions.put(id, conversion);
    }

    private void getAreaConversions() {
        //Base unit: square metre

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(SQ_KILOMETRES, R.string.sq_kilometre, "1000000.0", "km²"));
        units.add(new Unit(SQ_METRES, R.string.sq_metre, "1.0", "m²"));
        units.add(new Unit(SQ_CENTIMETRES, R.string.sq_centimetre, "0.0001", "cm²"));
        units.add(new Unit(HECTARE, R.string.hectare, "10000.0", "ha"));
        units.add(new Unit(SQ_MILE, R.string.sq_mile, "2589988.110336", "mi²"));
        units.add(new Unit(SQ_YARD, R.string.sq_yard, "0.83612736", "yd²"));
        units.add(new Unit(SQ_FOOT, R.string.sq_foot, "0.09290304", "ft²"));
        units.add(new Unit(SQ_INCH, R.string.sq_inch, "0.00064516", "in²"));
        units.add(new Unit(ACRE, R.string.acre, "4046.8564224", "ac"));
        units.add(new Unit(SQ_DECIMETRE, R.string.sq_decimetre, "0.01", "dm²"));
        units.add(new Unit(SQ_MILLIMETRE, R.string.sq_millimetre, "0.000001", "mm²"));
        units.add(new Unit(GUNTHA, R.string.guntha, "4046.8564224", "40", "guntha"));
        units.add(new Unit(CENT, R.string.cent, "4046.8564224", "100", "cent"));
        units.add(new Unit(MARLA, R.string.marla, "25.29285264", "marla"));
        addConversion(Conversion.AREA, new Conversion(Conversion.AREA, R.string.area, R.drawable.ic_area, units));
    }

    private void getCookingConversions() {
        // Base unit - cubic metre
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(TEASPOON, R.string.teaspoon, "0.00000492892159375", "tsp"));
        units.add(new Unit(TABLESPOON, R.string.tablespoon, "0.00001478676478125", "tbsp"));
        units.add(new Unit(CUP, R.string.cup, "0.0002365882365", "cup"));
        units.add(new Unit(FLUID_OUNCE, R.string.fluid_ounce, "0.0000295735295625", "fl oz"));
        units.add(new Unit(FLUID_OUNCE_UK, R.string.fluid_ounce_uk, "0.0000284130625", "fl oz"));
        units.add(new Unit(PINT, R.string.pint, "0.000473176473", "pt"));
        units.add(new Unit(PINT_UK, R.string.pint_uk, "0.00056826125", "pt"));
        units.add(new Unit(QUART, R.string.quart, "0.000946352946", "qt"));
        units.add(new Unit(QUART_UK, R.string.quart_uk, "0.0011365225", "qt"));
        units.add(new Unit(GALLON, R.string.gallon, "0.003785411784", "gal"));
        units.add(new Unit(GALLON_UK, R.string.gallon_uk, "0.00454609", "gal"));
        units.add(new Unit(MILLILITRE, R.string.millilitre, "0.000001", "ml"));
        units.add(new Unit(LITRE, R.string.litre, "0.001", "l"));
        units.add(new Unit(METRIC_CUP, R.string.metric_cup, "0.00025", "cup (metric)"));
        units.add(new Unit(TABLESPOON_AU, R.string.tablespoon_au, "0.00002", "tbsp (AU)"));
        units.add(new Unit(TEASPOON_IMPERIAL, R.string.teaspoon_imperial, "0.00454609", "768", "tsp (Imp)"));
        units.add(new Unit(TABLESPOON_IMPERIAL, R.string.tablespoon_imperial, "0.00454609", "256", "tbsp (Imp)"));
        units.add(new Unit(TEASPOON_METRIC, R.string.teaspoon_metric, "0.000005", "tsp (metric)"));
        units.add(new Unit(TABLESPOON_METRIC, R.string.tablespoon_metric, "0.000015", "tbsp (metric)"));
        addConversion(Conversion.COOKING, new Conversion(Conversion.COOKING, R.string.cooking, R.drawable.ic_cooking, units));
    }
    private void getStorageConversions() {
        // Base unit = mebibyte. Legacy binary IDs keep their factors and positions.
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(BIT, R.string.bit, "1", "8388608", "bit"));
        units.add(new Unit(BYTE, R.string.Byte, "0.00000095367431640625", "B"));
        units.add(new Unit(KILOBIT, R.string.kibibit, "0.0001220703125", "Kibit"));
        units.add(new Unit(KILOBYTE, R.string.kibibyte, "0.0009765625", "KiB"));
        units.add(new Unit(MEGABIT, R.string.mebibit, "0.125", "Mibit"));
        units.add(new Unit(MEGABYTE, R.string.mebibyte, "1.0", "MiB"));
        units.add(new Unit(GIGABIT, R.string.gibibit, "128.0", "Gibit"));
        units.add(new Unit(GIGABYTE, R.string.gibibyte, "1024.0", "GiB"));
        units.add(new Unit(TERABIT, R.string.tebibit, "131072.0", "Tibit"));
        units.add(new Unit(TERABYTE, R.string.tebibyte, "1048576.0", "TiB"));
        units.add(new Unit(PETABIT, R.string.pebibit, "134217728.0", "Pibit"));
        units.add(new Unit(PETABYTE, R.string.pebibyte, "1073741824.0", "PiB"));
        units.add(new Unit(EXABIT, R.string.exbibit, "137438953472.0", "Eibit"));
        units.add(new Unit(EXABYTE, R.string.exbibyte, "1099511627776.0", "EiB"));
        units.add(new Unit(DECIMAL_KILOBIT, R.string.kilobit, "1000", "8388608", "kbit"));
        units.add(new Unit(DECIMAL_KILOBYTE, R.string.kilobyte, "1000", "1048576", "kB"));
        units.add(new Unit(DECIMAL_MEGABIT, R.string.megabit, "1000000", "8388608", "Mbit"));
        units.add(new Unit(DECIMAL_MEGABYTE, R.string.megabyte, "1000000", "1048576", "MB"));
        units.add(new Unit(DECIMAL_GIGABIT, R.string.gigabit, "1000000000", "8388608", "Gbit"));
        units.add(new Unit(DECIMAL_GIGABYTE, R.string.gigabyte, "1000000000", "1048576", "GB"));
        units.add(new Unit(DECIMAL_TERABIT, R.string.terabit, "1000000000000", "8388608", "Tbit"));
        units.add(new Unit(DECIMAL_TERABYTE, R.string.terabyte, "1000000000000", "1048576", "TB"));
        units.add(new Unit(DECIMAL_PETABIT, R.string.petabit, "1000000000000000", "8388608", "Pbit"));
        units.add(new Unit(DECIMAL_PETABYTE, R.string.petabyte, "1000000000000000", "1048576", "PB"));
        units.add(new Unit(DECIMAL_EXABIT, R.string.exabit, "1000000000000000000", "8388608", "Ebit"));
        units.add(new Unit(DECIMAL_EXABYTE, R.string.exabyte, "1000000000000000000", "1048576", "EB"));
        addConversion(Conversion.STORAGE, new Conversion(Conversion.STORAGE, R.string.storage, R.drawable.ic_storage, units));
    }

    private void getEnergyConversions() {
        //Base unit Joules

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(JOULE, R.string.joule, "1.0", "J"));
        units.add(new Unit(KILOJOULE, R.string.kilojoule, "1000.0", "kJ"));
        units.add(new Unit(CALORIE, R.string.calorie, "4.184", "cal"));
        units.add(new Unit(KILOCALORIE, R.string.kilocalorie, "4184.0", "kcal"));
        units.add(new Unit(BTU, R.string.btu, "1055.05585262", "BTU"));
        units.add(new Unit(FT_LBF, R.string.ft_lbF, "1.35581794833140040", "ft⋅lbf"));
        units.add(new Unit(IN_LBF, R.string.in_lbF, "0.11298482902761670", "in⋅lbf"));
        units.add(new Unit(ELECTRON_VOLT, R.string.electron_volt, "0.0000000000000000001602176634", "eV"));
        units.add(new Unit(WATT_HOUR, R.string.watt_hour, "3600.0", "W⋅h"));
        units.add(new Unit(KILOWATT_HOUR, R.string.kilowatt_hour, "3600000.0", "kW⋅h"));
        units.add(new Unit(MEGAJOULE, R.string.megajoule, "1000000.0", "MJ"));
        units.add(new Unit(MEGAWATT_HOUR, R.string.megawatt_hour, "3600000000.0", "MW⋅h"));
        addConversion(Conversion.ENERGY, new Conversion(Conversion.ENERGY, R.string.energy, R.drawable.ic_energy, units));
    }

    private void getFuelConversions() {
        //Base Unit - Miles per Gallon US

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(MPG_US, R.string.mpg_us, 1.0, 1.0, "mpg"));
        units.add(new Unit(MPG_UK, R.string.mpg_uk, 0.83267418460479, 1.2009499255398, "mpg"));
        units.add(new Unit(L_100K, R.string.l_100k, 235.214582, 235.214582, "l/100km"));
        units.add(new Unit(KM_L, R.string.km_l, 2.352145833, 0.42514370749052, "km/l"));
        units.add(new Unit(MILES_L, R.string.miles_l, 3.7854118, 0.264172052, "mi/l"));
        addConversion(Conversion.FUEL, new Conversion(Conversion.FUEL, R.string.fuel_consumption, R.drawable.ic_fuel, units));
    }

    private void getLengthConversions() {
        //Base unit - Metres

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(KILOMETRE, R.string.kilometre, "1000.0", "km"));
        units.add(new Unit(MILE, R.string.mile, "1609.344", "mi"));
        units.add(new Unit(METRE, R.string.metre, "1.0", "m"));
        units.add(new Unit(CENTIMETRE, R.string.centimetre, "0.01", "cm"));
        units.add(new Unit(MILLIMETRE, R.string.millimetre, "0.001", "mm"));
        units.add(new Unit(MICROMETRE, R.string.micrometre, "0.000001", "µm"));
        units.add(new Unit(NANOMETRE, R.string.nanometre, "0.000000001", "nm"));
        units.add(new Unit(YARD, R.string.yard, "0.9144", "yd"));
        units.add(new Unit(FEET, R.string.feet, "0.3048", "ft"));
        units.add(new Unit(INCH, R.string.inch, "0.0254", "in"));
        units.add(new Unit(NAUTICAL_MILE, R.string.nautical_mile, "1852.0", "nmi"));
        units.add(new Unit(FURLONG, R.string.furlong, "201.168", "fur"));
        units.add(new Unit(LIGHT_YEAR, R.string.light_year, "9460730472580800.0", "ly"));
        units.add(new Unit(DECIMETRE, R.string.decimetre, "0.1", "dm"));
        units.add(new Unit(ANGSTROM, R.string.angstrom, "0.0000000001", "Å"));
        units.add(new Unit(PICOMETRE, R.string.picometre, "0.000000000001", "pm"));
        addConversion(Conversion.LENGTH, new Conversion(Conversion.LENGTH, R.string.length, R.drawable.ic_length, units));
    }

    private void getMassConversions() {
        //Base unit - Kilograms

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(KILOGRAM, R.string.kilogram, "1.0", "kg"));
        units.add(new Unit(POUND, R.string.pound, "0.45359237", "lb"));
        units.add(new Unit(GRAM, R.string.gram, "0.001", "g"));
        units.add(new Unit(MILLIGRAM, R.string.milligram, "0.000001", "mg"));
        units.add(new Unit(OUNCE, R.string.ounce, "0.028349523125", "oz"));
        units.add(new Unit(GRAIN, R.string.grain, "0.00006479891", "gr"));
        units.add(new Unit(STONE, R.string.stone, "6.35029318", "st"));
        units.add(new Unit(METRIC_TON, R.string.metric_ton, "1000.0", "t"));
        units.add(new Unit(SHORT_TON, R.string.short_ton, "907.18474", "ton"));
        units.add(new Unit(LONG_TON, R.string.long_ton, "1016.0469088", "ton"));
        units.add(new Unit(MICROGRAM, R.string.microgram, "0.000000001", "µg"));
        units.add(new Unit(CARAT, R.string.carat, "0.0002", "ct"));
        units.add(new Unit(QUINTAL, R.string.quintal, "100", "q"));
        units.add(new Unit(TOLA, R.string.tola, "0.0116638038", "tola"));
        units.add(new Unit(TROY_OUNCE, R.string.troy_ounce, "0.0311034768", "oz t"));
        addConversion(Conversion.MASS, new Conversion(Conversion.MASS, R.string.mass, R.drawable.ic_mass, units));
    }

    private void getPowerConversions() {
        //Base unit - Watt

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(WATT, R.string.watt, "1.0", "W"));
        units.add(new Unit(KILOWATT, R.string.kilowatt, "1000.0", "kW"));
        units.add(new Unit(MEGAWATT, R.string.megawatt, "1000000.0", "MW"));
        units.add(new Unit(HP, R.string.hp, "735.49875", "hp"));
        units.add(new Unit(HP_UK, R.string.hp_uk, "745.69987158227022", "hp"));
        units.add(new Unit(FT_LBF_S, R.string.ft_lbf_s, "1.35581794833140040", "ft⋅lbf/s"));
        units.add(new Unit(CALORIE_S, R.string.calorie_s, "4.1868", "cal/s"));
        units.add(new Unit(BTU_S, R.string.btu_s, "1055.05585262", "BTU/s"));
        units.add(new Unit(KVA, R.string.kva, "1000.0", "kVA"));
        addConversion(Conversion.POWER, new Conversion(Conversion.POWER, R.string.power, R.drawable.ic_power, units));
    }

    private void getPressureConversions() {
        //Base unit - Pa

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(MEGAPASCAL, R.string.megapascal, "1000000.0", "MPa"));
        units.add(new Unit(KILOPASCAL, R.string.kilopascal, "1000.0", "kPa"));
        units.add(new Unit(PASCAL, R.string.pascal, "1.0", "Pa"));
        units.add(new Unit(BAR, R.string.bar, "100000.0", "bar"));
        units.add(new Unit(PSI, R.string.psi, "6894.757293168361", "psi"));
        units.add(new Unit(PSF, R.string.psf, "47.880258980335840277777777778", "psf"));
        units.add(new Unit(ATMOSPHERE, R.string.atmosphere, "101325.0", "atm"));
        units.add(new Unit(TECHNICAL_ATMOSPHERE, R.string.technical_atmosphere, "98066.5", "at"));
        units.add(new Unit(MMHG, R.string.mmhg, "133.322387415", "mmHg"));
        units.add(new Unit(TORR, R.string.torr, "101325", "760", "Torr"));
        units.add(new Unit(HECTOPASCAL, R.string.hectopascal, "100.0", "hPa"));
        units.add(new Unit(MILLIBAR, R.string.millibar, "100", "mbar"));
        units.add(new Unit(INCH_MERCURY, R.string.inch_mercury, "3386.389", "inHg"));
        units.add(new Unit(CM_WATER, R.string.cm_water, "98.0665", "cmH₂O"));
        units.add(new Unit(INCH_WATER, R.string.inch_water, "249.08891", "inH₂O"));
        addConversion(Conversion.PRESSURE, new Conversion(Conversion.PRESSURE, R.string.pressure, R.drawable.ic_pressure, units));
    }

    private void getSpeedConversions() {
        //base unit - m/s

        List<Unit> units = new ArrayList<>();
        units.add(new Unit(KM_HR, R.string.km_h, "5", "18", "km/h"));
        units.add(new Unit(MPH, R.string.mph, "0.44704", "mph"));
        units.add(new Unit(M_S, R.string.m_s, "1.0", "m/s"));
        units.add(new Unit(FPS, R.string.fps, "0.3048", "fps"));
        units.add(new Unit(KNOT, R.string.knot, "1852", "3600", "kn"));
        units.add(new Unit(M_MIN, R.string.m_min, "1", "60", "m/min"));
        units.add(new Unit(KM_S, R.string.km_s, "1000.0", "km/s"));
        addConversion(Conversion.SPEED, new Conversion(Conversion.SPEED, R.string.speed, R.drawable.ic_speed, units));
    }

    private void getTemperatureConversions() {
        List<Unit> units = new ArrayList<>();
        units.add(new TemperatureUnit(CELSIUS, R.string.celsius, "°C"));
        units.add(new TemperatureUnit(FAHRENHEIT, R.string.fahrenheit, "°F"));
        units.add(new TemperatureUnit(KELVIN, R.string.kelvin, "K"));
        units.add(new TemperatureUnit(RANKINE, R.string.rankine, "°R"));
        units.add(new TemperatureUnit(DELISLE, R.string.delisle, "°De"));
        units.add(new TemperatureUnit(NEWTON, R.string.newton, "°N"));
        units.add(new TemperatureUnit(REAUMUR, R.string.reaumur, "°Ré"));
        units.add(new TemperatureUnit(ROMER, R.string.romer, "°Rø"));
        units.add(new TemperatureUnit(GAS_MARK, R.string.gas_mark, "GM"));
        addConversion(Conversion.TEMPERATURE, new Conversion(Conversion.TEMPERATURE, R.string.temperature, R.drawable.ic_temperature, units));
    }

    private void getTimeConversions() {
        //Base unit - seconds
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(YEAR, R.string.year, "31536000.0", "yr"));
        units.add(new Unit(MONTH, R.string.month, "2628000.0", "mo"));
        units.add(new Unit(WEEK, R.string.week, "604800.0", "wk"));
        units.add(new Unit(DAY, R.string.day, "86400.0", "d"));
        units.add(new Unit(HOUR, R.string.hour, "3600.0", "h"));
        units.add(new Unit(MINUTE, R.string.minute, "60.0", "min"));
        units.add(new Unit(SECOND, R.string.second, "1.0", "s"));
        units.add(new Unit(MILLISECOND, R.string.millisecond, "0.001", "ms"));
        units.add(new Unit(NANOSECOND, R.string.nanosecond, "0.000000001", "ns"));
        units.add(new Unit(MICROSECOND, R.string.microsecond, "0.000001", "µs"));
        units.add(new Unit(PICOSECOND, R.string.picosecond, "0.000000000001", "ps"));
        addConversion(Conversion.TIME, new Conversion(Conversion.TIME, R.string.time, R.drawable.ic_time, units));
    }

    private void getTorqueConversions() {
        // Base unit - Newton-metres
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(N_M, R.string.n_m, "1.0", "N⋅m"));
        units.add(new Unit(FT_LBF, R.string.ft_lbF, "1.35581794833140040", "ft⋅lbf"));
        units.add(new Unit(IN_LBF, R.string.in_lbF, "0.11298482902761670", "in⋅lbf"));
        units.add(new Unit(KGF_M, R.string.kgf_m, "9.80665", "kgf·m"));
        units.add(new Unit(N_CM, R.string.n_cm, "0.01", "N·cm"));
        addConversion(Conversion.TORQUE, new Conversion(Conversion.TORQUE, R.string.torque, R.drawable.ic_torque, units));
    }

    private void getVolumeConversions() {
        // Base unit - cubic metre
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(TEASPOON, R.string.teaspoon, "0.00000492892159375", "tsp"));
        units.add(new Unit(TABLESPOON, R.string.tablespoon, "0.00001478676478125", "tbsp"));
        units.add(new Unit(CUP, R.string.cup, "0.0002365882365", "cup"));
        units.add(new Unit(FLUID_OUNCE, R.string.fluid_ounce, "0.0000295735295625", "fl oz"));
        units.add(new Unit(FLUID_OUNCE_UK, R.string.fluid_ounce_uk, "0.0000284130625", "fl oz"));
        units.add(new Unit(PINT, R.string.pint, "0.000473176473", "pt"));
        units.add(new Unit(PINT_UK, R.string.pint_uk, "0.00056826125", "pt"));
        units.add(new Unit(QUART, R.string.quart, "0.000946352946", "qt"));
        units.add(new Unit(QUART_UK, R.string.quart_uk, "0.0011365225", "qt"));
        units.add(new Unit(GALLON, R.string.gallon, "0.003785411784", "gal"));
        units.add(new Unit(GALLON_UK, R.string.gallon_uk, "0.00454609", "gal"));
        units.add(new Unit(BARREL, R.string.barrel, "0.119240471196", "bbl"));
        units.add(new Unit(BARREL_UK, R.string.barrel_uk, "0.16365924", "bbl"));
        units.add(new Unit(MILLILITRE, R.string.millilitre, "0.000001", "ml"));
        units.add(new Unit(LITRE, R.string.litre, "0.001", "l"));
        units.add(new Unit(CUBIC_CM, R.string.cubic_cm, "0.000001", "cm³"));
        units.add(new Unit(CUBIC_M, R.string.cubic_m, "1.0", "m³"));
        units.add(new Unit(CUBIC_INCH, R.string.cubic_inch, "0.000016387064", "in³"));
        units.add(new Unit(CUBIC_FOOT, R.string.cubic_foot, "0.028316846592", "ft³"));
        units.add(new Unit(CUBIC_YARD, R.string.cubic_yard, "0.764554857984", "yd³"));
        units.add(new Unit(CUBIC_MM, R.string.cubic_mm, "0.000000001", "mm³"));
        units.add(new Unit(CUBIC_KM, R.string.cubic_km, "1000000000.0", "km³"));
        units.add(new Unit(MICROLITRE, R.string.microlitre, "0.000000001", "µL"));
        units.add(new Unit(CENTILITRE, R.string.centilitre, "0.00001", "cL"));
        units.add(new Unit(DECILITRE, R.string.decilitre, "0.0001", "dL"));
        units.add(new Unit(OIL_BARREL_US, R.string.oil_barrel_us, "0.158987294928", "bbl (oil)"));
        for (Unit unit : getById(Conversion.COOKING).getUnits()) {
            if (unit.getId() >= METRIC_CUP && unit.getId() <= TABLESPOON_METRIC) units.add(unit);
        }
        addConversion(Conversion.VOLUME, new Conversion(Conversion.VOLUME, R.string.volume, R.drawable.ic_volume, units));
    }

    private void getAngleConversions() {
        // Base unit = degree. Rational turns/gradians are exact; radians use a guard-digit π.
        List<Unit> units = new ArrayList<>();
        units.add(new Unit(DEGREE, R.string.degree, "1", "°"));
        units.add(new Unit(RADIAN, R.string.radian, "180", "3.141592653589793238462643383279502884197169399375105820974944592307816406", "rad"));
        units.add(new Unit(GRADIAN, R.string.gradian, "0.9", "gon"));
        units.add(new Unit(REVOLUTION, R.string.revolution, "360", "rev"));
        addConversion(Conversion.ANGLE, new Conversion(Conversion.ANGLE, R.string.angle, R.drawable.ic_angle, units));
    }

    public double convertTemperatureValue(double value, Unit from, Unit to) {
        double result = value;
        if (from.getId() != to.getId()) {
            switch (to.getId()) {
                case (CELSIUS):
                    result = toCelsius(from.getId(), value);
                    break;

                case (Unit.FAHRENHEIT):
                    result = toFahrenheit(from.getId(), value);
                    break;

                case (Unit.KELVIN):
                    result = toKelvin(from.getId(), value);
                    break;

                case (Unit.RANKINE):
                    result = toRankine(from.getId(), value);
                    break;

                case (Unit.DELISLE):
                    result = toDelisle(from.getId(), value);
                    break;

                case (Unit.NEWTON):
                    result = toNewton(from.getId(), value);
                    break;

                case (Unit.REAUMUR):
                    result = toReaumur(from.getId(), value);
                    break;

                case (Unit.ROMER):
                    result = toRomer(from.getId(), value);
                    break;

                case (Unit.GAS_MARK):
                    result = toGasMark(from.getId(), value);
                    break;
            }
        }

        return result;
    }

    /**
     * Convert a Fuel Consumption value from one unit to another
     *
     * @param value the value to convert
     * @param from  the unit to be converted from
     * @param to    the unit to be converted to
     */
    public double convertFuelValue(double value, Unit from, Unit to) {
        return convertFuelValue(BigDecimal.valueOf(value), from, to).doubleValue();
    }

    public BigDecimal convertFuelValue(BigDecimal value, Unit from, Unit to) {
        // Retain the existing explicit zero convention for this tool.
        if (from.getId() == to.getId() || value.signum() == 0) return DecimalMath.checked(value);
        BigDecimal fromFactor = BigDecimal.valueOf(from.getConversionToBaseUnit());
        BigDecimal toFactor = BigDecimal.valueOf(to.getConversionFromBaseUnit());
        BigDecimal result;
        if (from.getId() == Unit.L_100K) {
            result = DecimalMath.divide(fromFactor.multiply(toFactor), value);
        } else if (to.getId() == Unit.L_100K) {
            result = DecimalMath.divide(toFactor, value.multiply(fromFactor));
        } else {
            result = value.multiply(fromFactor).multiply(toFactor);
        }
        return DecimalMath.checked(result);
    }

    /**
     * Convert a value from one unit to another
     *
     * @param value the value to convert
     * @param from  the unit to be converted from
     * @param to    the unit to be converted to
     */
    public double convert(double value, Unit from, Unit to) {
        return convert(BigDecimal.valueOf(value), from, to).doubleValue();
    }

    public BigDecimal convert(BigDecimal value, Unit from, Unit to) {
        if (from.getId() == to.getId()) return DecimalMath.checked(value);
        // Cross-multiply exact definitions before a single rounded division.
        BigDecimal numerator = value.multiply(from.getBaseNumerator()).multiply(to.getBaseDenominator());
        BigDecimal denominator = from.getBaseDenominator().multiply(to.getBaseNumerator());
        return DecimalMath.divide(numerator, denominator);
    }

    private double toCelsius(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (Unit.FAHRENHEIT):    // F to C
                result = (value - 32) * 5 / 9;
                break;

            case (Unit.KELVIN):    // K to C
                result = value - 273.15;
                break;

            case (Unit.RANKINE):    // R to C
                result = (value - 491.67) * 5 / 9;
                break;

            case (Unit.DELISLE):    // D to C
                result = 100 - value * 2 / 3;
                break;

            case (Unit.NEWTON):    //N to C
                result = value * 100 / 33;
                break;

            case (Unit.REAUMUR):    //Re to C
                result = value * 5 / 4;
                break;

            case (Unit.ROMER):    //Ro to C
                result = (value - 7.5) * 40 / 21;
                break;

            case (Unit.GAS_MARK): //GM to C
                //Convert from GM to F, then from F to C
                result = (fromGasMark(value) - 32) * 5 / 9;
                break;
        }

        return result;
    }

    private double toFahrenheit(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to F
                result = value * 9 / 5 + 32;
                break;

            case (Unit.KELVIN):    // K to F
                result = value * 9 / 5 - 459.67;
                break;

            case (Unit.RANKINE):    // R to F
                result = value - 459.67;
                break;

            case (Unit.DELISLE):    //D to F
                result = 212 - value * 6 / 5;
                break;

            case (Unit.NEWTON):    //N to F
                result = value * 60 / 11 + 32;
                break;

            case (Unit.REAUMUR):    //Re to F
                result = value * 9 / 4 + 32;
                break;

            case (Unit.ROMER):    //Ro to F
                result = (value - 7.5) * 24 / 7 + 32;
                break;

            case (Unit.GAS_MARK):
                result = fromGasMark(value);
                break;
        }

        return result;
    }

    private double toKelvin(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to K
                result = value + 273.15;
                break;

            case (Unit.FAHRENHEIT):    // F to K
                result = (value + 459.67) * 5 / 9;
                break;

            case (Unit.RANKINE):    // R to K
                result = value * 5 / 9;
                break;

            case (Unit.DELISLE):    //D to K
                result = 373.15 - value * 2 / 3;
                break;

            case (Unit.NEWTON):    //N to K
                result = value * 100 / 33 + 273.15;
                break;

            case (Unit.REAUMUR):    //Re to K
                result = value * 5 / 4 + 273.15;
                break;

            case (Unit.ROMER):    //Ro to K
                result = (value - 7.5) * 40 / 21 + 273.15;
                break;

            case (Unit.GAS_MARK):
                result = (fromGasMark(value) + 459.67) * 5 / 9;
                break;
        }

        return result;
    }

    private double toRankine(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to R
                result = (value + 273.15) * 9 / 5;
                break;

            case (Unit.FAHRENHEIT):    // F to R
                result = value + 459.67;
                break;

            case (Unit.KELVIN):    // K to R
                result = value * 9 / 5;
                break;

            case (Unit.DELISLE):    //D to R
                result = 671.67 - value * 6 / 5;
                break;

            case (Unit.NEWTON):    //N to R
                result = value * 60 / 11 + 491.67;
                break;

            case (Unit.REAUMUR):    //Re to R
                result = value * 9 / 4 + 491.67;
                break;

            case (Unit.ROMER):    //Ro to R
                result = (value - 7.5) * 24 / 7 + 491.67;
                break;

            case (Unit.GAS_MARK):
                result = fromGasMark(value) + 459.67;
                break;
        }

        return result;
    }

    private double toDelisle(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to D
                result = (100 - value) * 1.5;
                break;

            case (Unit.FAHRENHEIT):    // F to D
                result = (212 - value) * 5 / 6;
                break;

            case (Unit.KELVIN):    // K to D
                result = (373.15 - value) * 1.5;
                break;

            case (Unit.RANKINE):    // R to D
                result = (671.67 - value) * 5 / 6;
                break;

            case (Unit.NEWTON):    //N to D
                result = (33 - value) * 50 / 11;
                break;

            case (Unit.REAUMUR):    //Re to D
                result = (80 - value) * 1.875;
                break;

            case (Unit.ROMER):    //Ro to D
                result = (60 - value) * 20 / 7;
                break;

            case (Unit.GAS_MARK):
                result = (212 - fromGasMark(value)) * 5 / 6;
                break;
        }

        return result;
    }

    private double toNewton(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to N
                result = value * 33 / 100;
                break;

            case (Unit.FAHRENHEIT):    // F to N
                result = (value - 32) * 11 / 60;
                break;

            case (Unit.KELVIN):    // K to N
                result = (value - 273.15) * 33 / 100;
                break;

            case (Unit.RANKINE):    // R to N
                result = (value - 491.67) * 11 / 60;
                break;

            case (Unit.DELISLE):    //D to N
                result = 33 - value * 11 / 50;
                break;

            case (Unit.REAUMUR):    //Re to N
                result = value * 33 / 80;
                break;

            case (Unit.ROMER):    //Ro to N
                result = (value - 7.5) * 22 / 35;
                break;

            case (Unit.GAS_MARK):
                result = (fromGasMark(value) - 32) * 11 / 60;
                break;
        }

        return result;
    }

    private double toReaumur(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to Re
                result = value * 4 / 5;
                break;

            case (Unit.FAHRENHEIT):    // F to Re
                result = (value - 32) * 4 / 9;
                break;

            case (Unit.KELVIN):    // K to Re
                result = (value - 273.15) * 4 / 5;
                break;

            case (Unit.RANKINE):    // R to Re
                result = (value - 491.67) * 4 / 9;
                break;

            case (Unit.DELISLE):    //D to Re
                result = 80 - value * 8 / 15;
                break;

            case (Unit.NEWTON):    //N to Re
                result = value * 80 / 33;
                break;

            case (Unit.ROMER):    //Ro to Re
                result = (value - 7.5) * 32 / 21;
                break;

            case (Unit.GAS_MARK):
                result = (fromGasMark(value) - 32) * 4 / 9;
                break;
        }

        return result;
    }

    private double toRomer(int fromId, double value) {
        double result = value;

        switch (fromId) {
            case (CELSIUS):    // C to Ro
                result = value * 21 / 40 + 7.5;
                break;

            case (Unit.FAHRENHEIT):    // F to Ro
                result = (value - 32) * 7 / 24 + 7.5;
                break;

            case (Unit.KELVIN):    // K to Ro
                result = (value - 273.15) * 21 / 40 + 7.5;
                break;

            case (Unit.RANKINE):    // R to Ro
                result = (value - 491.67) * 7 / 24 + 7.5;
                break;

            case (Unit.DELISLE):    //D to Ro
                result = 60 - value * 7 / 20;
                break;

            case (Unit.NEWTON):    //N to Ro
                result = value * 35 / 22 + 7.5;
                break;

            case (Unit.REAUMUR):    //Re to Ro
                result = value * 21 / 32 + 7.5;
                break;

            case (Unit.GAS_MARK):
                result = (fromGasMark(value) - 32) * 7 / 24 + 7.5;
                break;
        }

        return result;
    }

    private double toGasMark(int fromId, double value) {
        //Convert incoming temperature to Fahrenheit, then convert from F to Gas Mark
        double resultF = toFahrenheit(fromId, value);
        double resultGM = 0;

        if (resultF >= 275) {
            resultGM = 0.04 * resultF - 10;
        }
        else if (resultF < 275) {
            resultGM = 0.01 * resultF - 2;
        }

        if (resultGM < 0) resultGM = 0;

        return resultGM;
    }

    private double fromGasMark(double value) {
        double resultF = 0;

        //Convert incoming Gas Mark to Fahrenheit, which will then be subsequently converted to desired unit
        if (value >= 1) {
            resultF = 25 * value + 250;
        }
        else if (value < 1) {
            resultF = 100 * value + 200;
        }

        return resultF;
    }
}

