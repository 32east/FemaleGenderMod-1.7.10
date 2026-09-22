package com.wildfire.render.armor;

import com.wildfire.api.IGenderArmor;

/**
 * Used when nothing is worn in the chest slot, or the worn item doesn't cover the chest.
 */
public final class EmptyGenderArmor implements IGenderArmor {

    public static final EmptyGenderArmor INSTANCE = new EmptyGenderArmor();

    private EmptyGenderArmor() {
    }

    @Override
    public boolean coversBreasts() {
        return false;
    }

    @Override
    public boolean alwaysHidesBreasts() {
        return false;
    }

    @Override
    public float physicsResistance() {
        return 0;
    }

    @Override
    public float tightness() {
        return 0;
    }
}
