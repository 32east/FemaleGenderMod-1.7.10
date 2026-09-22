package com.wildfire.main.config;

import com.google.gson.JsonObject;

/**
 * A single named setting: knows its default, how to validate a value, and how to read/write it as JSON.
 */
public abstract class ConfigKey<TYPE> {

    public final String key;
    public final TYPE defaultValue;

    protected ConfigKey(String key, TYPE defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public TYPE getDefault() {
        return defaultValue;
    }

    public boolean validate(TYPE value) {
        return value != null;
    }

    public abstract TYPE read(JsonObject obj);

    public abstract void save(JsonObject obj, TYPE value);
}
