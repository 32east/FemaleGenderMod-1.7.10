package com.wildfire.render.armor;

import com.wildfire.api.IGenderArmor;

/**
 * Plain {@link IGenderArmor} holder, plus the defaults used for vanilla armor materials.
 */
public class SimpleGenderArmor implements IGenderArmor {

    public static final SimpleGenderArmor FALLBACK = new SimpleGenderArmor(0.5F);
    public static final SimpleGenderArmor LEATHER = new SimpleGenderArmor(0.3F, 0.5F);
    public static final SimpleGenderArmor CHAIN_MAIL = new SimpleGenderArmor(0.5F, 0.2F);
    public static final SimpleGenderArmor GOLD = new SimpleGenderArmor(0.85F);
    public static final SimpleGenderArmor IRON = new SimpleGenderArmor(1);
    public static final SimpleGenderArmor DIAMOND = new SimpleGenderArmor(1);

    private final float physicsResistance;
    private final float tightness;
    private final boolean coversBreasts;
    private final boolean alwaysHidesBreasts;

    public SimpleGenderArmor(float physicsResistance) {
        this(physicsResistance, 0);
    }

    public SimpleGenderArmor(float physicsResistance, float tightness) {
        this(physicsResistance, tightness, true, false);
    }

    public SimpleGenderArmor(float physicsResistance, float tightness, boolean coversBreasts, boolean alwaysHidesBreasts) {
        this.physicsResistance = physicsResistance;
        this.tightness = tightness;
        this.coversBreasts = coversBreasts;
        this.alwaysHidesBreasts = alwaysHidesBreasts;
    }

    @Override
    public boolean coversBreasts() {
        return coversBreasts;
    }

    @Override
    public boolean alwaysHidesBreasts() {
        return alwaysHidesBreasts;
    }

    @Override
    public float physicsResistance() {
        return physicsResistance;
    }

    @Override
    public float tightness() {
        return tightness;
    }
}
