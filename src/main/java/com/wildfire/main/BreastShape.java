package com.wildfire.main;

/** Available breast meshes. Keep {@link #CLASSIC} first for old configs and network packets. */
public enum BreastShape {

    CLASSIC("femalegender.breast_shape.classic"),
    MERGED("femalegender.breast_shape.merged"),
    ROUND("femalegender.breast_shape.round");

    private final String translationKey;

    BreastShape(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public BreastShape next() {
        BreastShape[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static BreastShape byIndex(int index) {
        BreastShape[] values = values();
        return index >= 0 && index < values.length ? values[index] : CLASSIC;
    }

    public static BreastShape byName(String name) {
        if (name != null) {
            for (BreastShape shape : values()) {
                if (shape.name().equalsIgnoreCase(name)) {
                    return shape;
                }
            }
        }
        return CLASSIC;
    }
}
