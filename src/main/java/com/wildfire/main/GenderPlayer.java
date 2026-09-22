package com.wildfire.main;

import com.google.gson.JsonObject;
import com.wildfire.main.config.Configuration;
import com.wildfire.physics.BreastPhysics;

import java.util.UUID;

/**
 * Everything the mod knows about one player: their settings plus the live physics state.
 */
public class GenderPlayer {

    public boolean needsSync;
    public final UUID uuid;

    private Gender gender;
    private float pBustSize = Configuration.BUST_SIZE.getDefault();
    private boolean hurtSounds = Configuration.HURT_SOUNDS.getDefault();

    private boolean breastPhysics = Configuration.BREAST_PHYSICS.getDefault();
    private boolean armorBreastPhysics = Configuration.BREAST_PHYSICS_ARMOR.getDefault();
    private boolean showBreastsInArmor = Configuration.SHOW_IN_ARMOR.getDefault();
    private float bounceMultiplier = Configuration.BOUNCE_MULTIPLIER.getDefault();
    private float floppyMultiplier = Configuration.FLOPPY_MULTIPLIER.getDefault();

    public SyncStatus syncStatus = SyncStatus.UNKNOWN;

    private Configuration cfg;
    private final BreastPhysics lBreastPhysics;
    private final BreastPhysics rBreastPhysics;
    private final Breasts breasts;

    public GenderPlayer(UUID uuid) {
        this(uuid, Configuration.GENDER.getDefault());
    }

    public GenderPlayer(UUID uuid, Gender gender) {
        this.lBreastPhysics = new BreastPhysics(this);
        this.rBreastPhysics = new BreastPhysics(this);
        this.breasts = new Breasts();
        this.uuid = uuid;
        this.gender = gender;
    }

    /**
     * Config files are only touched on the client; a dedicated server keeps everything in memory.
     */
    public Configuration getConfig() {
        if (cfg == null) {
            cfg = new Configuration("WildfireGender", uuid.toString());
            cfg.set(Configuration.USERNAME, uuid);
            cfg.setDefault(Configuration.GENDER);
            cfg.setDefault(Configuration.BUST_SIZE);
            cfg.setDefault(Configuration.HURT_SOUNDS);
            cfg.setDefault(Configuration.BREASTS_OFFSET_X);
            cfg.setDefault(Configuration.BREASTS_OFFSET_Y);
            cfg.setDefault(Configuration.BREASTS_OFFSET_Z);
            cfg.setDefault(Configuration.BREASTS_UNIBOOB);
            cfg.setDefault(Configuration.BREASTS_CLEAVAGE);
            cfg.setDefault(Configuration.BREASTS_SHAPE);
            cfg.setDefault(Configuration.BREAST_PHYSICS);
            cfg.setDefault(Configuration.BREAST_PHYSICS_ARMOR);
            cfg.setDefault(Configuration.SHOW_IN_ARMOR);
            cfg.setDefault(Configuration.BOUNCE_MULTIPLIER);
            cfg.setDefault(Configuration.FLOPPY_MULTIPLIER);
            cfg.finish();
        }
        return cfg;
    }

    public Gender getGender() {
        return gender;
    }

    public boolean updateGender(Gender value) {
        if (value == null) {
            return false;
        }
        gender = value;
        return true;
    }

    public float getBustSize() {
        return pBustSize;
    }

    public boolean updateBustSize(float value) {
        if (Configuration.BUST_SIZE.validate(value)) {
            pBustSize = value;
            return true;
        }
        return false;
    }

    public boolean hasHurtSounds() {
        return hurtSounds;
    }

    public boolean updateHurtSounds(boolean value) {
        hurtSounds = value;
        return true;
    }

    public boolean hasBreastPhysics() {
        return breastPhysics;
    }

    public boolean updateBreastPhysics(boolean value) {
        breastPhysics = value;
        return true;
    }

    public boolean hasArmorBreastPhysics() {
        return armorBreastPhysics;
    }

    public boolean updateArmorBreastPhysics(boolean value) {
        armorBreastPhysics = value;
        return true;
    }

    public boolean showBreastsInArmor() {
        return showBreastsInArmor;
    }

    public boolean updateShowBreastsInArmor(boolean value) {
        showBreastsInArmor = value;
        return true;
    }

    /** The bounce slider maps 0..1 onto 0..3 internally. */
    public float getBounceMultiplier() {
        return Math.round((getBounceMultiplierRaw() * 3) * 100) / 100f;
    }

    public float getBounceMultiplierRaw() {
        return bounceMultiplier;
    }

    public boolean updateBounceMultiplier(float value) {
        if (Configuration.BOUNCE_MULTIPLIER.validate(value)) {
            bounceMultiplier = value;
            return true;
        }
        return false;
    }

    public float getFloppiness() {
        return floppyMultiplier;
    }

    public boolean updateFloppiness(float value) {
        if (Configuration.FLOPPY_MULTIPLIER.validate(value)) {
            floppyMultiplier = value;
            return true;
        }
        return false;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public Breasts getBreasts() {
        return breasts;
    }

    public BreastPhysics getLeftBreastPhysics() {
        return lBreastPhysics;
    }

    public BreastPhysics getRightBreastPhysics() {
        return rBreastPhysics;
    }

    public static JsonObject toJsonObject(GenderPlayer plr) {
        JsonObject obj = new JsonObject();
        Configuration.USERNAME.save(obj, plr.uuid);
        Configuration.GENDER.save(obj, plr.getGender());
        Configuration.BUST_SIZE.save(obj, plr.getBustSize());
        Configuration.HURT_SOUNDS.save(obj, plr.hasHurtSounds());
        Configuration.BREAST_PHYSICS.save(obj, plr.hasBreastPhysics());
        Configuration.BREAST_PHYSICS_ARMOR.save(obj, plr.hasArmorBreastPhysics());
        Configuration.SHOW_IN_ARMOR.save(obj, plr.showBreastsInArmor());
        Configuration.BOUNCE_MULTIPLIER.save(obj, plr.getBounceMultiplierRaw());
        Configuration.FLOPPY_MULTIPLIER.save(obj, plr.getFloppiness());

        Breasts b = plr.getBreasts();
        Configuration.BREASTS_OFFSET_X.save(obj, b.getXOffset());
        Configuration.BREASTS_OFFSET_Y.save(obj, b.getYOffset());
        Configuration.BREASTS_OFFSET_Z.save(obj, b.getZOffset());
        Configuration.BREASTS_UNIBOOB.save(obj, b.isUniboob());
        Configuration.BREASTS_CLEAVAGE.save(obj, b.getCleavage());
        Configuration.BREASTS_SHAPE.save(obj, b.getShape());
        return obj;
    }

    /**
     * Loads this player's settings from disk. Returns the same instance, or null if it is not tracked.
     */
    public static GenderPlayer loadCachedPlayer(UUID uuid, boolean markForSync) {
        GenderPlayer plr = WildfireGender.getPlayerById(uuid);
        if (plr == null || plr.syncStatus == SyncStatus.SYNCED) {
            return null;
        }
        Configuration config = plr.getConfig();
        if (plr.syncStatus == SyncStatus.SYNCED) {
            // A sync packet landed while we were reading the file; the server wins
            return null;
        }
        plr.syncStatus = SyncStatus.CACHED;
        plr.updateGender(config.get(Configuration.GENDER));
        plr.updateBustSize(config.get(Configuration.BUST_SIZE));
        plr.updateHurtSounds(config.get(Configuration.HURT_SOUNDS));
        plr.updateBreastPhysics(config.get(Configuration.BREAST_PHYSICS));
        plr.updateArmorBreastPhysics(config.get(Configuration.BREAST_PHYSICS_ARMOR));
        plr.updateShowBreastsInArmor(config.get(Configuration.SHOW_IN_ARMOR));
        plr.updateBounceMultiplier(config.get(Configuration.BOUNCE_MULTIPLIER));
        plr.updateFloppiness(config.get(Configuration.FLOPPY_MULTIPLIER));

        Breasts b = plr.getBreasts();
        b.updateXOffset(config.get(Configuration.BREASTS_OFFSET_X));
        b.updateYOffset(config.get(Configuration.BREASTS_OFFSET_Y));
        b.updateZOffset(config.get(Configuration.BREASTS_OFFSET_Z));
        b.updateUniboob(config.get(Configuration.BREASTS_UNIBOOB));
        b.updateCleavage(config.get(Configuration.BREASTS_CLEAVAGE));
        b.updateShape(config.get(Configuration.BREASTS_SHAPE));
        if (markForSync) {
            plr.needsSync = true;
        }
        return plr;
    }

    public static void saveGenderInfo(GenderPlayer plr) {
        Configuration config = plr.getConfig();
        config.set(Configuration.USERNAME, plr.uuid);
        config.set(Configuration.GENDER, plr.getGender());
        config.set(Configuration.BUST_SIZE, plr.getBustSize());
        config.set(Configuration.HURT_SOUNDS, plr.hasHurtSounds());
        config.set(Configuration.BREAST_PHYSICS, plr.hasBreastPhysics());
        config.set(Configuration.BREAST_PHYSICS_ARMOR, plr.hasArmorBreastPhysics());
        config.set(Configuration.SHOW_IN_ARMOR, plr.showBreastsInArmor());
        config.set(Configuration.BOUNCE_MULTIPLIER, plr.getBounceMultiplierRaw());
        config.set(Configuration.FLOPPY_MULTIPLIER, plr.getFloppiness());
        config.set(Configuration.BREASTS_OFFSET_X, plr.getBreasts().getXOffset());
        config.set(Configuration.BREASTS_OFFSET_Y, plr.getBreasts().getYOffset());
        config.set(Configuration.BREASTS_OFFSET_Z, plr.getBreasts().getZOffset());
        config.set(Configuration.BREASTS_UNIBOOB, plr.getBreasts().isUniboob());
        config.set(Configuration.BREASTS_CLEAVAGE, plr.getBreasts().getCleavage());
        config.set(Configuration.BREASTS_SHAPE, plr.getBreasts().getShape());
        config.save();
        plr.needsSync = true;
    }

    public enum SyncStatus {
        CACHED, SYNCED, UNKNOWN
    }
}
