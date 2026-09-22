package com.wildfire.render;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.Breasts;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireHelper;
import com.wildfire.physics.BreastPhysics;
import com.wildfire.render.WildfireModelRenderer.BreastModelBox;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.OverlayModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the breasts on top of the vanilla player model.
 *
 * <p>Hooks {@link RenderPlayerEvent.Specials.Post}, which Forge fires from
 * {@code RenderPlayer.renderEquippedItems} while the matrix is still in model space -- the same space
 * {@code ModelBiped} renders in, one unit per block. That keeps this working with renderer replacements
 * (SkinPort, Battlegear, ...) as long as they call through to the vanilla method.</p>
 */
public class GenderLayer {

    private static final float DEG_PER_RAD = 180F / (float) Math.PI;

    /** Breast boxes keyed by (depth, skin height), since both change the geometry and the UVs. */
    private final Map<Integer, BreastModelBox[]> breastCache = new HashMap<Integer, BreastModelBox[]>();

    private final OverlayModelBox lBreastWear;
    private final OverlayModelBox rBreastWear;
    private final BreastModelBox lBoobArmor;
    private final BreastModelBox rBoobArmor;

    public GenderLayer() {
        lBreastWear = new OverlayModelBox(true, 64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        rBreastWear = new OverlayModelBox(false, 64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);

        lBoobArmor = new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3, 0.0F, false);
        rBoobArmor = new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3, 0.0F, false);
    }

    private BreastModelBox[] getBreasts(int depth, int skinHeight) {
        int key = depth * 128 + skinHeight;
        BreastModelBox[] boxes = breastCache.get(key);
        if (boxes == null) {
            boxes = new BreastModelBox[] {
                    new BreastModelBox(64, skinHeight, 16, 17, -4F, 0.0F, 0F, 4, 5, depth, 0.0F, false),
                    new BreastModelBox(64, skinHeight, 20, 17, 0, 0.0F, 0F, 4, 5, depth, 0.0F, false) };
            breastCache.put(key, boxes);
        }
        return boxes;
    }

    @SubscribeEvent
    public void onRenderSpecials(RenderPlayerEvent.Specials.Post event) {
        if (!(event.entityPlayer instanceof AbstractClientPlayer)) {
            return;
        }
        AbstractClientPlayer ent = (AbstractClientPlayer) event.entityPlayer;
        try {
            render(ent, event.renderer.modelBipedMain, event.partialRenderTick);
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("Failed to render breasts for " + ent.getCommandSenderName(), t);
        }
    }

    private void render(AbstractClientPlayer ent, ModelBiped model, float partialTicks) {
        if (ent.isInvisible()) {
            return;
        }
        GenderPlayer plr = WildfireGender.getPlayerById(ent.getUniqueID());
        if (plr == null) {
            return;
        }

        ItemStack armorStack = ent.getEquipmentInSlot(3);
        IGenderArmor genderArmor = WildfireHelper.getArmorConfig(armorStack);
        boolean isChestplateOccupied = genderArmor.coversBreasts();
        if (genderArmor.alwaysHidesBreasts() || (!plr.showBreastsInArmor() && isChestplateOccupied)) {
            return;
        }

        Breasts breasts = plr.getBreasts();
        float breastOffsetX = Math.round((Math.round(breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
        float breastOffsetY = -Math.round((Math.round(breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
        float breastOffsetZ = -Math.round((Math.round(breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

        BreastPhysics leftBreastPhysics = plr.getLeftBreastPhysics();
        final float bSize = leftBreastPhysics.getBreastSize(partialTicks);
        float outwardAngle = (Math.round(breasts.getCleavage() * 100f) / 100f) * 100f;
        outwardAngle = Math.min(outwardAngle, 10);

        // Smaller breasts sit flatter against the chest
        int reducer = 0;
        if (bSize < 0.84f) {
            reducer++;
        }
        if (bSize < 0.72f) {
            reducer++;
        }

        int skinHeight = SkinUtils.getSkinHeight(ent.getLocationSkin());
        int depth = (int) (4 - breastOffsetZ - reducer);
        if (depth < 1) {
            depth = 1;
        }
        BreastModelBox[] boxes = getBreasts(depth, skinHeight);

        float lTotal = WildfireHelper.lerp(partialTicks, leftBreastPhysics.getPreBounceY(), leftBreastPhysics.getBounceY());
        float lTotalX = WildfireHelper.lerp(partialTicks, leftBreastPhysics.getPreBounceX(), leftBreastPhysics.getBounceX());
        float leftBounceRotation = WildfireHelper
                .lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
        float rTotal;
        float rTotalX;
        float rightBounceRotation;
        if (breasts.isUniboob()) {
            rTotal = lTotal;
            rTotalX = lTotalX;
            rightBounceRotation = leftBounceRotation;
        } else {
            BreastPhysics rightBreastPhysics = plr.getRightBreastPhysics();
            rTotal = WildfireHelper.lerp(partialTicks, rightBreastPhysics.getPreBounceY(), rightBreastPhysics.getBounceY());
            rTotalX = WildfireHelper.lerp(partialTicks, rightBreastPhysics.getPreBounceX(), rightBreastPhysics.getBounceX());
            rightBounceRotation = WildfireHelper
                    .lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
        }

        // Upstream's cutoff, expressed against the pre-scaled size it used there
        if (Math.min(bSize * 1.5f, 0.7f) < 0.02f) {
            return;
        }

        float zOff = 0.0625f - (bSize * 0.0625f);
        float breastSize = bSize + 0.5f * Math.abs(bSize - 0.7f) * 2f;

        float resistance = WildfireHelper.clamp(genderArmor.physicsResistance(), 0, 1);
        // Only bother with the breathing animation when the armor is not stiff enough to hide it anyway
        boolean breathingAnimation = resistance <= 0.5F
                && (!ent.isInsideOfMaterial(Material.water) || ent.isPotionActive(Potion.waterBreathing));
        boolean bounceEnabled = plr.hasBreastPhysics()
                && (!isChestplateOccupied || (plr.hasArmorBreastPhysics() && resistance < 1));

        ResourceLocation skin = ent.getLocationSkin();
        boolean wear = skinHeight >= 64 && SkinUtils.hasVisibleBodyWear(model);

        GL11.glColor4f(1f, 1f, 1f, 1f);
        renderBreastWithTransforms(ent, model, armorStack, skin, wear, boxes[0], lBreastWear, lBoobArmor, bounceEnabled,
                lTotalX, lTotal, leftBounceRotation, breastSize, breastOffsetX, breastOffsetY, breastOffsetZ, zOff,
                outwardAngle, breasts.isUniboob(), isChestplateOccupied, breathingAnimation, true);
        renderBreastWithTransforms(ent, model, armorStack, skin, wear, boxes[1], rBreastWear, rBoobArmor, bounceEnabled,
                rTotalX, rTotal, rightBounceRotation, breastSize, -breastOffsetX, breastOffsetY, breastOffsetZ, zOff,
                -outwardAngle, breasts.isUniboob(), isChestplateOccupied, breathingAnimation, false);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void renderBreastWithTransforms(AbstractClientPlayer entity, ModelBiped model, ItemStack armorStack,
            ResourceLocation skin, boolean wear, BreastModelBox breast, OverlayModelBox breastWear,
            BreastModelBox breastArmor, boolean bounceEnabled, float totalX, float total, float bounceRotation,
            float breastSize, float breastOffsetX, float breastOffsetY, float breastOffsetZ, float zOff,
            float outwardAngle, boolean uniboob, boolean isChestplateOccupied, boolean breathingAnimation,
            boolean left) {
        GL11.glPushMatrix();
        try {
            ModelRenderer body = model.bipedBody;
            GL11.glTranslatef(body.rotationPointX * 0.0625f, body.rotationPointY * 0.0625f, body.rotationPointZ * 0.0625f);
            if (body.rotateAngleZ != 0.0F) {
                GL11.glRotatef(body.rotateAngleZ * DEG_PER_RAD, 0F, 0F, 1F);
            }
            if (body.rotateAngleY != 0.0F) {
                GL11.glRotatef(body.rotateAngleY * DEG_PER_RAD, 0F, 1F, 0F);
            }
            if (body.rotateAngleX != 0.0F) {
                GL11.glRotatef(body.rotateAngleX * DEG_PER_RAD, 1F, 0F, 0F);
            }

            if (bounceEnabled) {
                GL11.glTranslatef(totalX / 32f, total / 32f, 0);
            }

            // Shift down onto the chest
            GL11.glTranslatef(breastOffsetX * 0.0625f, 0.05625f + (breastOffsetY * 0.0625f),
                    zOff - 0.0625f * 2f + (breastOffsetZ * 0.0625f));

            if (!uniboob) {
                GL11.glTranslatef(-0.0625f * 2 * (left ? 1 : -1), 0, 0);
            }
            if (bounceEnabled) {
                GL11.glRotatef(bounceRotation, 0F, 1F, 0F);
            }
            if (!uniboob) {
                GL11.glTranslatef(0.0625f * 2 * (left ? 1 : -1), 0, 0);
            }

            float rotationMultiplier = 0;
            if (bounceEnabled) {
                GL11.glTranslatef(0, -0.035f * breastSize, 0);
                rotationMultiplier = -total / 12f;
            }
            float totalRotation = bounceEnabled ? breastSize + rotationMultiplier : breastSize;
            if (totalRotation > breastSize + 0.2F) {
                totalRotation = breastSize + 0.2F;
            }
            totalRotation = Math.min(totalRotation, 1);

            if (isChestplateOccupied) {
                GL11.glTranslatef(0, 0, 0.01f);
            }

            GL11.glRotatef(outwardAngle, 0F, 1F, 0F);
            GL11.glRotatef(-35f * totalRotation, 1F, 0F, 0F);

            if (breathingAnimation) {
                float f5 = -MathHelper.cos(entity.ticksExisted * 0.09F) * 0.45F + 0.45F;
                GL11.glRotatef(f5, 1F, 0F, 0F);
            }

            GL11.glScalef(0.9995f, 1f, 1f); // nudge the two halves apart so they do not z-fight

            renderBreast(entity, armorStack, skin, wear, breast, breastWear, breastArmor, left);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void renderBreast(AbstractClientPlayer entity, ItemStack armorStack, ResourceLocation skin, boolean wear,
            BreastModelBox breast, OverlayModelBox breastWear, BreastModelBox breastArmor, boolean left) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(skin);
        renderBox(breast);

        if (wear) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, -0.015f);
            GL11.glScalef(1.05f, 1.05f, 1.05f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderBox(breastWear);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }

        if (armorStack != null && armorStack.getItem() instanceof ItemArmor) {
            ItemArmor armorItem = (ItemArmor) armorStack.getItem();
            GL11.glPushMatrix();
            GL11.glTranslatef(left ? 0.001f : -0.001f, 0.015f, -0.015f);
            GL11.glScalef(1.05f, 1, 1);

            // Armor pass 1 is the chestplate layer; a color other than -1 means it is dyeable and
            // gets a second, undyed overlay pass, the same way RenderPlayer does it.
            ResourceLocation armorTexture = RenderBiped.getArmorResource(entity, armorStack, 1, null);
            int color = armorItem.getColor(armorStack);
            if (armorTexture != null) {
                if (color != -1) {
                    GL11.glColor4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1F);
                }
                mc.getTextureManager().bindTexture(armorTexture);
                renderBox(breastArmor);
                GL11.glColor4f(1F, 1F, 1F, 1F);

                if (color != -1) {
                    ResourceLocation overlay = RenderBiped.getArmorResource(entity, armorStack, 1, "overlay");
                    if (overlay != null) {
                        mc.getTextureManager().bindTexture(overlay);
                        renderBox(breastArmor);
                    }
                }
            }
            GL11.glPopMatrix();
            mc.getTextureManager().bindTexture(skin);
        }
    }

    private static void renderBox(ModelBox box) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (TexturedQuad quad : box.quads) {
            tess.setNormal(quad.normalX, quad.normalY, quad.normalZ);
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                tess.addVertexWithUV(vertex.x / 16.0F, vertex.y / 16.0F, vertex.z / 16.0F, vertex.texturePositionX,
                        vertex.texturePositionY);
            }
        }
        tess.draw();
    }

    /** Ticks the physics for every player the client can see. */
    public static void updatePhysics(EntityPlayer player) {
        GenderPlayer plr = WildfireGender.getPlayerById(player.getUniqueID());
        if (plr == null) {
            return;
        }
        IGenderArmor armor = WildfireHelper.getArmorConfig(player.getEquipmentInSlot(3));
        plr.getLeftBreastPhysics().update(player, armor);
        if (!plr.getBreasts().isUniboob()) {
            plr.getRightBreastPhysics().update(player, armor);
        }
    }
}
