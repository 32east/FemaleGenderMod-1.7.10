package com.wildfire.main.config;

import com.google.gson.JsonObject;

public class BooleanConfigKey extends ConfigKey<Boolean> {

    public BooleanConfigKey(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Boolean read(JsonObject obj) {
        try {
            if (obj != null && obj.has(key)) {
                return obj.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject obj, Boolean value) {
        obj.addProperty(key, value);
    }
}
