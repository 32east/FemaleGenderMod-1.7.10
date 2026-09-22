package com.wildfire.main;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public enum Gender {
    FEMALE("female", EnumChatFormatting.LIGHT_PURPLE),
    MALE("male", EnumChatFormatting.BLUE),
    OTHER("other", EnumChatFormatting.GREEN);

    private static final Gender[] VALUES = values();

    private final String translationKey;
    private final EnumChatFormatting color;

    Gender(String key, EnumChatFormatting color) {
        this.translationKey = "femalegender.label." + key;
        this.color = color;
    }

    public static Gender byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : MALE;
    }

    public String getDisplayName() {
        return color + StatCollector.translateToLocal(translationKey) + EnumChatFormatting.RESET;
    }

    public boolean hasFemaleHurtSounds() {
        return this == FEMALE;
    }

    public boolean canHaveBreasts() {
        return this != MALE;
    }

    public Gender next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
