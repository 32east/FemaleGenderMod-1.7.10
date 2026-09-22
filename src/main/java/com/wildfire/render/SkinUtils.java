package com.wildfire.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for dealing with both 64x32 (vanilla 1.7.10) and 64x64 (1.8+ style, via SkinPort and friends) skins.
 */
public final class SkinUtils {

    private static final Map<ResourceLocation, Entry> HEIGHTS = new HashMap<ResourceLocation, Entry>();

    /** Cached lookup of the body overlay part that mods such as SkinPort add to their player model. */
    private static final Map<Class<?>, Field> BODY_WEAR_FIELDS = new HashMap<Class<?>, Field>();

    private static final class Entry {
        int height;
        long checkedAt;
    }

    private SkinUtils() {
    }

    /**
     * Height in pixels of the currently uploaded skin texture, or 32 if it cannot be determined.
     *
     * <p>A skin starts out as the 64x32 fallback and is replaced once the download finishes, so a
     * result of 32 is re-checked periodically rather than cached forever.</p>
     */
    public static int getSkinHeight(ResourceLocation skin) {
        if (skin == null) {
            return 32;
        }
        Entry entry = HEIGHTS.get(skin);
        long now = System.currentTimeMillis();
        if (entry != null && (entry.height > 32 || now - entry.checkedAt < 1000L)) {
            return entry.height;
        }
        if (entry == null) {
            entry = new Entry();
            entry.height = 32;
            HEIGHTS.put(skin, entry);
        }
        entry.checkedAt = now;
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            // Only the aspect ratio matters: an HD skin is the same layout scaled up. Anything smaller
            // than a skin is the missing-texture placeholder or a half-loaded download, so ignore it.
            if (width >= 64 && height >= 32) {
                entry.height = height >= width ? 64 : 32;
            }
        } catch (Throwable ignored) {
            // A driver that dislikes the query is not worth crashing the frame over
        }
        return entry.height;
    }

    /**
     * Whether the player model draws a body overlay ("jacket") layer, so the breast overlay should follow.
     *
     * <p>Vanilla 1.7.10 has no such part; mods that add one expose it as a {@code bipedBodyWear} field.</p>
     */
    public static boolean hasVisibleBodyWear(ModelBiped model) {
        if (model == null) {
            return false;
        }
        Class<?> cls = model.getClass();
        Field field;
        if (BODY_WEAR_FIELDS.containsKey(cls)) {
            field = BODY_WEAR_FIELDS.get(cls);
        } else {
            field = null;
            for (Class<?> c = cls; c != null && field == null; c = c.getSuperclass()) {
                try {
                    field = c.getDeclaredField("bipedBodyWear");
                    field.setAccessible(true);
                } catch (Exception ignored) {
                    field = null;
                }
            }
            BODY_WEAR_FIELDS.put(cls, field);
        }
        if (field == null) {
            return false;
        }
        try {
            Object part = field.get(model);
            return part instanceof ModelRenderer && ((ModelRenderer) part).showModel;
        } catch (Exception ignored) {
            return false;
        }
    }
}
