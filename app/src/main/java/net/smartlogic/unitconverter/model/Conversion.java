package net.smartlogic.unitconverter.model;

import java.util.List;

public final class Conversion {

    // Conversion types. Use integer over enum as value is stored in database and shared prefs
    // Use @IntDef for type safety
    public static final int LENGTH = 0;
    public static final int AREA = 1;
    public static final int TIME = 2;
    public static final int TEMPERATURE = 3;
    public static final int MASS = 4;
    public static final int STORAGE = 5;
    public static final int FUEL = 6;
    public static final int COOKING = 7;
    public static final int SPEED = 8;
    public static final int VOLUME = 9;
    public static final int POWER = 10;
    public static final int PRESSURE = 11;
    public static final int ENERGY = 12;
    public static final int TORQUE = 13;
    public static final int CURRENCY = 14;

    private int id;
    private int labelResource;
    private int imageResource;
    private List<Unit> units;

   /* @IntDef({AREA, COOKING, CURRENCY, STORAGE, ENERGY, FUEL, LENGTH, MASS, POWER, PRESSURE, SPEED,
            TEMPERATURE, TIME, TORQUE, VOLUME})
    public @interface id {
    }*/

    /**
     * Create a Conversion object
     *
     * @param id            id of the conversion
     * @param labelResource string resource id for the conversion
     * @param units         list of units contained in conversion
     */
    public Conversion(int id, int labelResource, int imageResource, List<Unit> units) {
        this.id = id;
        this.labelResource = labelResource;
        this.units = units;
        this.imageResource = imageResource;
    }

    public Conversion() {

    }

    public int getId() {
        return id;
    }

    public int getLabelResource() {
        return labelResource;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public Unit getUnitById(int id) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).getId() == id) {
                return units.get(i);
            }
        }

        throw new IllegalArgumentException("Invalid unit id supplied");
    }

    public Unit getUnitByLabelResource(int id) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).getLabelResource() == id) {
                return units.get(i);
            }
        }

        throw new IllegalArgumentException("Invalid unit id supplied");
    }

    public int getImageResource() {
        return imageResource;
    }

}