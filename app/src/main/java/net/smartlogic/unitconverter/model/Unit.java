package net.smartlogic.unitconverter.model;

import java.math.BigDecimal;
import net.smartlogic.unitconverter.utils.DecimalMath;

public class Unit {

    public static final int SQ_KILOMETRES = 0;
    public static final int SQ_METRES = 1;
    public static final int SQ_CENTIMETRES = 2;
    public static final int HECTARE = 3;
    public static final int SQ_MILE = 4;
    public static final int SQ_YARD = 5;
    public static final int SQ_FOOT = 6;
    public static final int SQ_INCH = 7;
    public static final int ACRE = 8;
    public static final int SQ_DECIMETRE = 10;

    public static final int AUD = 1300;
    public static final int BGN = 1301;
    public static final int BRL = 1302;
    public static final int CDN = 1303;
    public static final int CHF = 1304;
    public static final int CNY = 1305;
    public static final int CZK = 1306;
    public static final int DKK = 1307;
    public static final int EUR = 1331;
    public static final int GBP = 1308;
    public static final int HKD = 1309;
    public static final int HRK = 1310;
    public static final int HUF = 1311;
    public static final int IDR = 1312;
    public static final int ILS = 1313;
    public static final int INR = 1314;
    public static final int ISK = 1332;
    public static final int JPY = 1315;
    public static final int KRW = 1316;
    public static final int MXN = 1317;
    public static final int MYR = 1318;
    public static final int NOK = 1319;
    public static final int NZD = 1320;
    public static final int PHP = 1321;
    public static final int PLN = 1322;
    public static final int RON = 1323;
    public static final int RUB = 1324;
    public static final int SEK = 1325;
    public static final int SGD = 1326;
    public static final int THB = 1327;
    public static final int LIRA = 1328;
    public static final int USD = 1329;
    public static final int ZAR = 1330;

    public static final int BIT = 100;
    public static final int BYTE = 101;
    public static final int KILOBIT = 102;
    public static final int KILOBYTE = 103;
    public static final int MEGABIT = 104;
    public static final int MEGABYTE = 105;
    public static final int GIGABIT = 106;
    public static final int GIGABYTE = 107;
    public static final int TERABIT = 108;
    public static final int TERABYTE = 109;
    public static final int PETABIT = 110;
    public static final int PETABYTE = 111;
    public static final int EXABIT = 112;
    public static final int EXABYTE = 113;

    public static final int JOULE = 200;
    public static final int KILOJOULE = 201;
    public static final int CALORIE = 208;
    public static final int KILOCALORIE = 202;
    public static final int BTU = 203;
    public static final int FT_LBF = 204;
    public static final int IN_LBF = 205;
    public static final int KILOWATT_HOUR = 206;
    public static final int ELECTRON_VOLT = 207;
    public static final int WATT_HOUR = 209;
    public static final int MEGAJOULE = 210;
    public static final int MEGAWATT_HOUR = 211;

    public static final int MPG_US = 300;
    public static final int MPG_UK = 301;
    public static final int L_100K = 302;
    public static final int KM_L = 303;
    public static final int MILES_L = 304;

    public static final int KILOMETRE = 400;
    public static final int MILE = 401;
    public static final int METRE = 402;
    public static final int CENTIMETRE = 403;
    public static final int MILLIMETRE = 404;
    public static final int MICROMETRE = 405;
    public static final int NANOMETRE = 406;
    public static final int YARD = 407;
    public static final int FEET = 408;
    public static final int INCH = 409;
    public static final int NAUTICAL_MILE = 410;
    public static final int FURLONG = 411;
    public static final int LIGHT_YEAR = 412;
    public static final int DECIMETRE = 413;
    public static final int ANGSTROM = 414;
    public static final int PICOMETRE = 415;

    public static final int KILOGRAM = 500;
    public static final int POUND = 501;
    public static final int GRAM = 502;
    public static final int MILLIGRAM = 503;
    public static final int OUNCE = 504;
    public static final int GRAIN = 505;
    public static final int STONE = 506;
    public static final int METRIC_TON = 507;
    public static final int SHORT_TON = 508;
    public static final int LONG_TON = 509;
    public static final int MICROGRAM = 510;
    public static final int CARAT = 511;

    public static final int WATT = 600;
    public static final int KILOWATT = 601;
    public static final int MEGAWATT = 602;
    public static final int HP = 603;
    public static final int HP_UK = 604;
    public static final int FT_LBF_S = 605;
    public static final int CALORIE_S = 606;
    public static final int BTU_S = 607;
    public static final int KVA = 608;

    public static final int MEGAPASCAL = 700;
    public static final int KILOPASCAL = 701;
    public static final int PASCAL = 702;
    public static final int BAR = 703;
    public static final int PSI = 704;
    public static final int PSF = 705;
    public static final int ATMOSPHERE = 706;
    public static final int TECHNICAL_ATMOSPHERE = 709;
    public static final int MMHG = 707;
    public static final int TORR = 708;
    public static final int HECTOPASCAL = 710;

    public static final int KM_HR = 800;
    public static final int MPH = 801;
    public static final int M_S = 802;
    public static final int FPS = 803;
    public static final int KNOT = 804;
    public static final int M_MIN = 805;
    public static final int KM_S = 806;

    public static final int CELSIUS = 900;
    public static final int FAHRENHEIT = 901;
    public static final int KELVIN = 902;
    public static final int RANKINE = 903;
    public static final int DELISLE = 904;
    public static final int NEWTON = 905;
    public static final int REAUMUR = 906;
    public static final int ROMER = 907;
    public static final int GAS_MARK = 908;

    public static final int YEAR = 1000;
    public static final int MONTH = 1001;
    public static final int WEEK = 1002;
    public static final int DAY = 1003;
    public static final int HOUR = 1004;
    public static final int MINUTE = 1005;
    public static final int SECOND = 1006;
    public static final int MILLISECOND = 1007;
    public static final int NANOSECOND = 1008;
    public static final int MICROSECOND = 1009;
    public static final int PICOSECOND = 1010;

    public static final int N_M = 1100;

    public static final int TEASPOON = 1200;
    public static final int TABLESPOON = 1201;
    public static final int CUP = 1202;
    public static final int FLUID_OUNCE = 1203;
    public static final int QUART = 1204;
    public static final int PINT = 1205;
    public static final int GALLON = 1206;
    public static final int BARREL = 1207;
    public static final int FLUID_OUNCE_UK = 1208;
    public static final int QUART_UK = 1209;
    public static final int PINT_UK = 1210;
    public static final int GALLON_UK = 1211;
    public static final int BARREL_UK = 1212;
    public static final int MILLILITRE = 1213;
    public static final int LITRE = 1214;
    public static final int CUBIC_CM = 1215;
    public static final int CUBIC_M = 1216;
    public static final int CUBIC_INCH = 1217;
    public static final int CUBIC_FOOT = 1218;
    public static final int CUBIC_YARD = 1219;
    public static final int CUBIC_MM = 1220;
    public static final int CUBIC_KM = 1221;

    // Additional definitions: never reuse IDs or reorder existing persisted unit rows.
    public static final int SQ_MILLIMETRE = 11;
    public static final int GUNTHA = 12;
    public static final int CENT = 13;
    public static final int MARLA = 14;
    // Retired Bigha IDs remain reserved; never reuse them for another unit.
    public static final int BIGHA_ASSAM = 15;
    public static final int BIGHA_RAJASTHAN_PUCCA = 16;
    public static final int BIGHA_RAJASTHAN_KACCHA = 17;
    public static final int QUINTAL = 512;
    public static final int TOLA = 513;
    public static final int TROY_OUNCE = 514;
    public static final int MICROLITRE = 1222;
    public static final int CENTILITRE = 1223;
    public static final int DECILITRE = 1224;
    public static final int OIL_BARREL_US = 1225;
    public static final int METRIC_CUP = 1226;
    public static final int TABLESPOON_AU = 1227;
    public static final int TEASPOON_IMPERIAL = 1228;
    public static final int TABLESPOON_IMPERIAL = 1229;
    public static final int TEASPOON_METRIC = 1230;
    public static final int TABLESPOON_METRIC = 1231;
    public static final int MILLIBAR = 711;
    public static final int INCH_MERCURY = 712;
    public static final int CM_WATER = 713;
    public static final int INCH_WATER = 714;
    public static final int KGF_M = 1101;
    public static final int N_CM = 1102;
    public static final int DEGREE = 1400;
    public static final int RADIAN = 1401;
    public static final int GRADIAN = 1402;
    public static final int REVOLUTION = 1403;
    public static final int DECIMAL_KILOBIT = 114;
    public static final int DECIMAL_KILOBYTE = 115;
    public static final int DECIMAL_MEGABIT = 116;
    public static final int DECIMAL_MEGABYTE = 117;
    public static final int DECIMAL_GIGABIT = 118;
    public static final int DECIMAL_GIGABYTE = 119;
    public static final int DECIMAL_TERABIT = 120;
    public static final int DECIMAL_TERABYTE = 121;
    public static final int DECIMAL_PETABIT = 122;
    public static final int DECIMAL_PETABYTE = 123;
    public static final int DECIMAL_EXABIT = 124;
    public static final int DECIMAL_EXABYTE = 125;

    private final int id;
    private final int labelResource;
    private final double conversionToBase;
    private final double conversionFromBase;
    private final String symbol;
    private final BigDecimal baseNumerator;
    private final BigDecimal baseDenominator;

    /**
     * Create a unit object
     *
     * @param id                 id of the unit
     * @param labelResource      string resource id of the label
     * @param conversionToBase   the value to convert to the base unit of the conversion
     * @param conversionFromBase the value to convert from the base unit of the conversion
     * @param symbol             unit symbol (e.g., km, kg, $)
     */
    public Unit(int id, int labelResource, double conversionToBase, double conversionFromBase, String symbol) {
        this.id = id;
        this.labelResource = labelResource;
        this.conversionToBase = conversionToBase;
        this.conversionFromBase = conversionFromBase;
        this.symbol = symbol;
        this.baseNumerator = BigDecimal.valueOf(conversionToBase);
        this.baseDenominator = BigDecimal.ONE;
    }

    /** Canonical base-unit definition; inverse factors are derived, never stored independently. */
    public Unit(int id, int labelResource, String numerator, String symbol) {
        this(id, labelResource, numerator, "1", symbol);
    }

    public Unit(int id, int labelResource, String numerator, String denominator, String symbol) {
        this.id = id;
        this.labelResource = labelResource;
        this.baseNumerator = new BigDecimal(numerator);
        this.baseDenominator = new BigDecimal(denominator);
        if (baseNumerator.signum() <= 0 || baseDenominator.signum() <= 0) {
            throw new IllegalArgumentException("Unit factors must be positive");
        }
        this.conversionToBase = baseNumerator.divide(baseDenominator, DecimalMath.CONTEXT).doubleValue();
        this.conversionFromBase = baseDenominator.divide(baseNumerator, DecimalMath.CONTEXT).doubleValue();
        this.symbol = symbol;
    }

    public BigDecimal getBaseNumerator() { return baseNumerator; }
    public BigDecimal getBaseDenominator() { return baseDenominator; }


    public int getId() {
        return id;
    }

    public double getConversionToBaseUnit() {
        return conversionToBase;
    }

    public double getConversionFromBaseUnit() {
        return conversionFromBase;
    }

    public int getLabelResource() {
        return labelResource;
    }

    public String getSymbol() {
        return symbol;
    }
}
