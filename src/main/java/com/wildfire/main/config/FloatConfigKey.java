package com.wildfire.main.config;

import com.google.gson.JsonObject;

public class FloatConfigKey extends ConfigKey<Float> {

    public final float min;
    public final float max;

    public FloatConfigKey(String key, float defaultValue, float min, float max) {
        super(key, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(Float value) {
        return value != null && !value.isNaN() && value >= min && value <= max;
    }

    public float clamp(float value) {
        if (Float.isNaN(value)) {
            return defaultValue;
        }
        return value < min ? min : (value > max ? max : value);
    }

    @Override
    public Float read(JsonObject obj) {
        try {
            if (obj != null && obj.has(key)) {
                return clamp(obj.get(key).getAsFloat());
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject obj, Float value) {
        obj.addProperty(key, value);
    }
}
