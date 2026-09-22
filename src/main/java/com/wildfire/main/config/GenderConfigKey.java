package com.wildfire.main.config;

import com.google.gson.JsonObject;
import com.wildfire.main.Gender;

public class GenderConfigKey extends ConfigKey<Gender> {

    public GenderConfigKey(String key) {
        super(key, Gender.MALE);
    }

    @Override
    public Gender read(JsonObject obj) {
        try {
            if (obj != null && obj.has(key)) {
                return Gender.byIndex(obj.get(key).getAsInt());
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject obj, Gender value) {
        obj.addProperty(key, value.ordinal());
    }
}
