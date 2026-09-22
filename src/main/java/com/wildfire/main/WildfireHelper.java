package com.wildfire.main;

import com.wildfire.api.IGenderArmor;
import com.wildfire.render.armor.EmptyGenderArmor;
import com.wildfire.render.armor.SimpleGenderArmor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WildfireHelper {

    private static final Random RAND = new Random();

    private static final Map<Item, IGenderArmor> GENDER_ARMORS = new HashMap<Item, IGenderArmor>();

    public static float randFloat(float min, float max) {
        return min + RAND.nextFloat() * (max - min);
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    /**
     * Lets other mods describe how one of their chest items should behave. Call during init.
     */
    public static void addGenderArmor(Item item, float physicsResistance, float tightness) {
        GENDER_ARMORS.put(item, new SimpleGenderArmor(physicsResistance, tightness));
    }

    public static void addGenderArmor(Item item, IGenderArmor armor) {
        GENDER_ARMORS.put(item, armor);
    }

    public static IGenderArmor getArmorConfig(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return EmptyGenderArmor.INSTANCE;
        }

        IGenderArmor registered = GENDER_ARMORS.get(stack.getItem());
        if (registered != null) {
            return registered;
        }

        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armorItem = (ItemArmor) stack.getItem();
            // armorType 1 is the chestplate slot
            if (armorItem.armorType != 1) {
                return EmptyGenderArmor.INSTANCE;
            }
            // Match on the material rather than the item so mods reusing a vanilla material get sane defaults
            ItemArmor.ArmorMaterial material = armorItem.getArmorMaterial();
            if (material == ItemArmor.ArmorMaterial.CLOTH) {
                return SimpleGenderArmor.LEATHER;
            } else if (material == ItemArmor.ArmorMaterial.CHAIN) {
                return SimpleGenderArmor.CHAIN_MAIL;
            } else if (material == ItemArmor.ArmorMaterial.GOLD) {
                return SimpleGenderArmor.GOLD;
            } else if (material == ItemArmor.ArmorMaterial.IRON) {
                return SimpleGenderArmor.IRON;
            } else if (material == ItemArmor.ArmorMaterial.DIAMOND) {
                return SimpleGenderArmor.DIAMOND;
            }
            return SimpleGenderArmor.FALLBACK;
        }

        // Anything else in the chest slot (elytra-likes, backpacks, ...) is treated as not covering the chest
        return EmptyGenderArmor.INSTANCE;
    }
}
