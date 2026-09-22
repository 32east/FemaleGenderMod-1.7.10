package com.wildfire.main;

import com.wildfire.main.config.Configuration;

/**
 * The purely geometric part of a player's configuration.
 */
public class Breasts {

    private float xOffset = Configuration.BREASTS_OFFSET_X.getDefault();
    private float yOffset = Configuration.BREASTS_OFFSET_Y.getDefault();
    private float zOffset = Configuration.BREASTS_OFFSET_Z.getDefault();
    private float cleavage = Configuration.BREASTS_CLEAVAGE.getDefault();
    private boolean uniboob = Configuration.BREASTS_UNIBOOB.getDefault();
    private BreastShape shape = Configuration.BREASTS_SHAPE.getDefault();

    public float getXOffset() {
        return xOffset;
    }

    public boolean updateXOffset(float value) {
        if (Configuration.BREASTS_OFFSET_X.validate(value)) {
            xOffset = value;
            return true;
        }
        return false;
    }

    public float getYOffset() {
        return yOffset;
    }

    public boolean updateYOffset(float value) {
        if (Configuration.BREASTS_OFFSET_Y.validate(value)) {
            yOffset = value;
            return true;
        }
        return false;
    }

    public float getZOffset() {
        return zOffset;
    }

    public boolean updateZOffset(float value) {
        if (Configuration.BREASTS_OFFSET_Z.validate(value)) {
            zOffset = value;
            return true;
        }
        return false;
    }

    public float getCleavage() {
        return cleavage;
    }

    public boolean updateCleavage(float value) {
        if (Configuration.BREASTS_CLEAVAGE.validate(value)) {
            cleavage = value;
            return true;
        }
        return false;
    }

    public boolean isUniboob() {
        return uniboob;
    }

    public boolean updateUniboob(boolean value) {
        uniboob = value;
        return true;
    }

    public BreastShape getShape() {
        return shape;
    }

    public boolean updateShape(BreastShape value) {
        if (Configuration.BREASTS_SHAPE.validate(value)) {
            shape = value;
            return true;
        }
        return false;
    }
}
