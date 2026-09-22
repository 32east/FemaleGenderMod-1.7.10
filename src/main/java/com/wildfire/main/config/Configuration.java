package com.wildfire.main.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.wildfire.main.WildfireGender;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player settings, stored as one JSON file in {@code config/WildfireGender/&lt;uuid&gt;.json}.
 */
public class Configuration {

    public static final UUIDConfigKey USERNAME =
            new UUIDConfigKey("username", UUID.nameUUIDFromBytes("UNKNOWN".getBytes(StandardCharsets.UTF_8)));
    public static final GenderConfigKey GENDER = new GenderConfigKey("gender");
    public static final FloatConfigKey BUST_SIZE = new FloatConfigKey("bust_size", 0.6F, 0, 1);
    public static final BooleanConfigKey HURT_SOUNDS = new BooleanConfigKey("hurt_sounds", true);

    public static final FloatConfigKey BREASTS_OFFSET_X = new FloatConfigKey("breasts_xOffset", 0.0F, -1, 1);
    public static final FloatConfigKey BREASTS_OFFSET_Y = new FloatConfigKey("breasts_yOffset", 0.0F, -1, 1);
    public static final FloatConfigKey BREASTS_OFFSET_Z = new FloatConfigKey("breasts_zOffset", 0.0F, -1, 0);
    public static final BooleanConfigKey BREASTS_UNIBOOB = new BooleanConfigKey("breasts_uniboob", true);
    public static final FloatConfigKey BREASTS_CLEAVAGE = new FloatConfigKey("breasts_cleavage", 0.05F, 0, 0.1F);

    public static final BooleanConfigKey BREAST_PHYSICS = new BooleanConfigKey("breast_physics", true);
    public static final BooleanConfigKey BREAST_PHYSICS_ARMOR = new BooleanConfigKey("breast_physics_armor", true);
    public static final BooleanConfigKey SHOW_IN_ARMOR = new BooleanConfigKey("show_in_armor", true);
    public static final FloatConfigKey BOUNCE_MULTIPLIER = new FloatConfigKey("bounce_multiplier", 0.34F, 0, 1);
    public static final FloatConfigKey FLOPPY_MULTIPLIER = new FloatConfigKey("floppy_multiplier", 0.95F, 0, 1);

    private static final Gson GSON = new Gson();

    private final File cfgFile;
    public JsonObject saveValues = new JsonObject();

    public Configuration(String saveLoc, String cfgName) {
        File saveDir = new File(WildfireGender.configDir, saveLoc);
        if (!saveDir.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            saveDir.mkdirs();
        }
        cfgFile = new File(saveDir, cfgName + ".json");
    }

    public void finish() {
        if (cfgFile.exists()) {
            load();
        }
    }

    public <TYPE> void set(ConfigKey<TYPE> key, TYPE value) {
        key.save(saveValues, value);
    }

    public <TYPE> void setDefault(ConfigKey<TYPE> key) {
        if (!saveValues.has(key.key)) {
            set(key, key.defaultValue);
        }
    }

    public <TYPE> TYPE get(ConfigKey<TYPE> key) {
        return key.read(saveValues);
    }

    public void save() {
        FileWriter writer = null;
        JsonWriter jsonWriter = null;
        try {
            writer = new FileWriter(cfgFile);
            jsonWriter = new JsonWriter(writer);
            GSON.toJson(saveValues, jsonWriter);
        } catch (Exception e) {
            WildfireGender.LOGGER.warn("Failed to save " + cfgFile, e);
        } finally {
            close(jsonWriter);
            close(writer);
        }
    }

    public void load() {
        FileReader reader = null;
        try {
            reader = new FileReader(cfgFile);
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj != null) {
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    saveValues.add(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            WildfireGender.LOGGER.warn("Failed to load " + cfgFile, e);
        } finally {
            close(reader);
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
