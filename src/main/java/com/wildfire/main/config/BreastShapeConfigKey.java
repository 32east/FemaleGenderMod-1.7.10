package com.wildfire.main.config;

import com.google.gson.JsonObject;
import com.wildfire.main.BreastShape;

/** Stores shape names so adding more shapes later does not renumber existing profiles. */
public class BreastShapeConfigKey extends ConfigKey<BreastShape> {

    public BreastShapeConfigKey(String key) {
        super(key, BreastShape.CLASSIC);
    }

    @Override
    public BreastShape read(JsonObject obj) {
        try {
            if (obj != null && obj.has(key)) {
                return BreastShape.byName(obj.get(key).getAsString());
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject obj, BreastShape value) {
        obj.addProperty(key, value.name());
    }
}
