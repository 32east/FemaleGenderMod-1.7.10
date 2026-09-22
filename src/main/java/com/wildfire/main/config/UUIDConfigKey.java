package com.wildfire.main.config;

import com.google.gson.JsonObject;

import java.util.UUID;

public class UUIDConfigKey extends ConfigKey<UUID> {

    public UUIDConfigKey(String key, UUID defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public UUID read(JsonObject obj) {
        try {
            if (obj != null && obj.has(key)) {
                return UUID.fromString(obj.get(key).getAsString());
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject obj, UUID value) {
        obj.addProperty(key, value.toString());
    }
}
