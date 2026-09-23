package com.wildfire.render;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.Breasts;
import com.wildfire.main.BreastShape;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireHelper;
import com.wildfire.physics.BreastPhysics;
import com.wildfire.render.WildfireModelRenderer.BreastModelBox;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.OverlayModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.RoundBreastModelBox;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
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
import org.lwjgl.opengl.GL12;

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

    private static final ResourceLocation ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    /** How far a full-size bust swings out from the chest, and the scale the physics rides on. */
    private static final float TILT_DEGREES = 35F;

    /**
     * How far each layer sits outside the one below it, in model pixels.
     *
     * <p>Skin, jacket and armor are the same box drawn three times, so without this they share faces and
     * the depth test picks a winner per pixel -- the speckled "flickering skin" over the chest. The top
     * face was the worst of it: scaling a box whose origin is its top inner front corner moves every face
     * except that one. Inflating the boxes instead keeps the nesting honest, and X is left to the
     * widening below so the inner faces stay on the body centre line.</p>
     */
    private static final float WEAR_INFLATE = 0.25F;

    private static final float ARMOR_INFLATE = 0.55F;

    /** Outward widening per layer; the boxes pivot on their inner edge, so this only moves the outer face. */
    private static final float WEAR_WIDEN = 1.05F;

    private static final float ARMOR_WIDEN = 1.1F;

    /** Breast boxes keyed by (depth, skin height), since both change the geometry and the UVs. */
    private final Map<Integer, BreastModelBox[]> breastCache = new HashMap<Integer, BreastModelBox[]>();

    /**
     * Size steps the rounded mesh is cached at.
     *
     * <p>Unlike the classic box, whose size lives in the tilt, the rounded volume has to be rebuilt to
     * change size -- scaling it with the matrix would shorten its normals and darken the whole bust.
     * A 24th of full size is well under a screen pixel at any sane render distance.</p>
     */
    private static final int ROUND_SIZE_STEPS = 24;

    /** Rounded meshes keyed by (size step, skin height); one entry carries all three layers. */
    private final Map<Integer, RoundModelSet> roundCache = new HashMap<Integer, RoundModelSet>();

    private final ModelBox[] classicWear;
    private final ModelBox[] classicArmor;

    /** Lets the in-game test draw a player without this layer, to find out which pixels are the breasts. */
    public static boolean hiddenForTest;

    public GenderLayer() {
        classicWear = new ModelBox[] {
                new OverlayModelBox(true, 64, 64, 17, 34, -4F, 0.0F, 0F, 4, 5, 3,
                        0F, WEAR_INFLATE, WEAR_INFLATE, false),
                new OverlayModelBox(false, 64, 64, 21, 34, 0, 0.0F, 0F, 4, 5, 3,
                        0F, WEAR_INFLATE, WEAR_INFLATE, false) };

        classicArmor = new ModelBox[] {
                new BreastModelBox(64, 32, 16, 17, -4F, 0.0F, 0F, 4, 5, 3,
                        0F, ARMOR_INFLATE, ARMOR_INFLATE, false),
                new BreastModelBox(64, 32, 20, 17, 0, 0.0F, 0F, 4, 5, 3,
                        0F, ARMOR_INFLATE, ARMOR_INFLATE, false) };
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

    /**
     * The rounded pair at one size, with the same skin, jacket and armor texture windows the classic
     * boxes use -- the mesh projects them from the front, so existing skins keep working unchanged.
     */
    private RoundModelSet getRoundBreasts(int sizeStep, int skinHeight, boolean merged) {
        int key = (sizeStep * 2 + (merged ? 1 : 0)) * 128 + skinHeight;
        RoundModelSet models = roundCache.get(key);
        if (models == null) {
            float scale = sizeStep / (float) ROUND_SIZE_STEPS;
            models = new RoundModelSet(
                    new ModelBox[] {
                            new RoundBreastModelBox(64, skinHeight, 20, 21, true, merged, scale, 0F),
                            new RoundBreastModelBox(64, skinHeight, 24, 21, false, merged, scale, 0F) },
                    new ModelBox[] {
                            new RoundBreastModelBox(64, 64, 20, 37, true, merged, scale, WEAR_INFLATE),
                            new RoundBreastModelBox(64, 64, 24, 37, false, merged, scale, WEAR_INFLATE) },
                    new ModelBox[] {
                            new RoundBreastModelBox(64, 32, 20, 21, true, merged, scale, ARMOR_INFLATE),
                            new RoundBreastModelBox(64, 32, 24, 21, false, merged, scale, ARMOR_INFLATE) });
            roundCache.put(key, models);
        }
        return models;
    }

    private static final class RoundModelSet {

        private final ModelBox[] skin;
        private final ModelBox[] wear;
        private final ModelBox[] armor;

        private RoundModelSet(ModelBox[] skin, ModelBox[] wear, ModelBox[] armor) {
            this.skin = skin;
            this.wear = wear;
            this.armor = armor;
        }
    }

    @SubscribeEvent
    public void onRenderSpecials(RenderPlayerEvent.Specials.Post event) {
        if (hiddenForTest || !(event.entityPlayer instanceof AbstractClientPlayer)) {
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
        BreastShape shape = breasts.getShape();
        boolean roundShape = shape != BreastShape.CLASSIC;
        ModelBox[] boxes;
        ModelBox[] wearBoxes;
        ModelBox[] armorBoxes;
        if (roundShape) {
            // The rounded mesh carries its own size, so it steps with the slider rather than with the
            // coarse box depth. The exponent keeps the default 0.6 setting close to a full bust while
            // still collapsing to nothing at zero.
            float sizeFactor = (float) Math.pow(WildfireHelper.clamp(bSize, 0F, 1F), 0.65D);
            RoundModelSet rounded = getRoundBreasts(Math.max(1, Math.round(sizeFactor * ROUND_SIZE_STEPS)),
                    skinHeight, shape == BreastShape.MERGED);
            boxes = rounded.skin;
            wearBoxes = rounded.wear;
            armorBoxes = rounded.armor;
        } else {
            boxes = getBreasts(depth, skinHeight);
            wearBoxes = classicWear;
            armorBoxes = classicArmor;
        }

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

        // The rounded mesh is modelled against the chest plane itself, so it does not want the classic
        // box's size-driven Z nudge.
        float shapeZOff = roundShape ? 0F : zOff;

        // ItemRenderer switches GL_RESCALE_NORMAL off after drawing a flat held item, and this layer comes after
        // the held item. In the world that hardly shows, but a GUI or a HUD doll draws the player tens of times
        // larger, and without the rescale the normals shrink to next to nothing: the breasts kept only the
        // ambient light and went dark next to a body drawn with the rescale on
        boolean rescaleWasOff = !GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
        if (rescaleWasOff) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
        try {
            renderBreastWithTransforms(ent, model, armorStack, skin, wear, boxes[0], wearBoxes[0], armorBoxes[0],
                    bounceEnabled, lTotalX, lTotal, leftBounceRotation, breastSize, breastOffsetX, breastOffsetY,
                    breastOffsetZ, shapeZOff, outwardAngle, breasts.isUniboob(), isChestplateOccupied,
                    breathingAnimation, true, roundShape, partialTicks);
            renderBreastWithTransforms(ent, model, armorStack, skin, wear, boxes[1], wearBoxes[1], armorBoxes[1],
                    bounceEnabled, rTotalX, rTotal, rightBounceRotation, breastSize, -breastOffsetX, breastOffsetY,
                    breastOffsetZ, shapeZOff, -outwardAngle, breasts.isUniboob(), isChestplateOccupied,
                    breathingAnimation, false, roundShape, partialTicks);
        } finally {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            if (rescaleWasOff) {
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            }
        }
    }

    private void renderBreastWithTransforms(AbstractClientPlayer entity, ModelBiped model, ItemStack armorStack,
            ResourceLocation skin, boolean wear, ModelBox breast, ModelBox breastWear,
            ModelBox breastArmor, boolean bounceEnabled, float totalX, float total, float bounceRotation,
            float breastSize, float breastOffsetX, float breastOffsetY, float breastOffsetZ, float zOff,
            float outwardAngle, boolean uniboob, boolean isChestplateOccupied, boolean breathingAnimation,
            boolean left, boolean roundShape, float partialTicks) {
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
            // The classic box lies flat against the chest until this swings it out, so its whole pose is
            // in the tilt. The rounded mesh is already modelled hanging, so only the physics part of the
            // same angle is left to apply -- otherwise size would tip the bust up towards the chin.
            float tilt = roundShape
                    ? TILT_DEGREES * WildfireHelper.clamp(rotationMultiplier, -0.6F, 0.25F)
                    : TILT_DEGREES * totalRotation;
            GL11.glRotatef(-tilt, 1F, 0F, 0F);

            if (breathingAnimation) {
                float f5 = -MathHelper.cos(entity.ticksExisted * 0.09F) * 0.45F + 0.45F;
                GL11.glRotatef(f5, 1F, 0F, 0F);
            }

            GL11.glScalef(0.9995f, 1f, 1f); // nudge the two halves apart so they do not z-fight

            renderBreast(entity, armorStack, skin, wear, breast, breastWear, breastArmor, left, roundShape,
                    partialTicks);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void renderBreast(AbstractClientPlayer entity, ItemStack armorStack, ResourceLocation skin, boolean wear,
            ModelBox breast, ModelBox breastWear, ModelBox breastArmor, boolean left, boolean roundShape,
            float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(skin);
        renderBox(breast);

        if (wear) {
            GL11.glPushMatrix();
            if (!roundShape) {
                wearTransform();
            }
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderBox(breastWear);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }

        boolean armorDrawn = false;
        if (armorStack != null && armorStack.getItem() instanceof ItemArmor) {
            ItemArmor armorItem = (ItemArmor) armorStack.getItem();
            GL11.glPushMatrix();
            if (!roundShape) {
                armorTransform(left);
            }

            // Armor pass 1 is the chestplate layer; a color other than -1 means it is dyeable and
            // gets a second, undyed overlay pass, the same way RenderPlayer does it.
            ResourceLocation armorTexture = RenderBiped.getArmorResource(entity, armorStack, 1, null);
            int color = armorItem.getColor(armorStack);
            if (armorTexture != null) {
                armorDrawn = true;
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

                if (armorStack.isItemEnchanted()) {
                    renderGlint(mc, breastArmor, entity.ticksExisted + partialTicks);
                }
            }
            GL11.glPopMatrix();
            mc.getTextureManager().bindTexture(skin);
        }

        if (entity.hurtTime > 0 || entity.deathTime > 0) {
            renderHurtOverlay(entity, partialTicks, breast, wear ? breastWear : null,
                    armorDrawn ? breastArmor : null, left, roundShape);
        }
    }

    /** Widening of the classic jacket box; the hurt overlay has to repeat it, so it lives in one place. */
    private static void wearTransform() {
        // Y and Z clearance comes from the box inflation; this only pushes the outward face out
        GL11.glScalef(WEAR_WIDEN, 1f, 1f);
    }

    /** As {@link #wearTransform()}, for the armor copy. */
    private static void armorTransform(boolean left) {
        GL11.glTranslatef(left ? 0.001f : -0.001f, 0f, 0f);
        GL11.glScalef(ARMOR_WIDEN, 1f, 1f);
    }

    /**
     * The enchantment shimmer, lifted out of {@code RendererLivingEntity}.
     *
     * <p>Vanilla draws it as part of the armor passes, which are long finished by the time
     * {@code renderEquippedItems} fires this layer, so the breast copy of the chestplate has to redo it
     * for itself. {@code GL_EQUAL} lights up only fragments the armor box just wrote, so this has to run
     * under the same matrix and with the same geometry as the pass above.</p>
     */
    private static void renderGlint(Minecraft mc, ModelBox box, float animationTime) {
        mc.getTextureManager().bindTexture(ITEM_GLINT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        for (int pass = 0; pass < 2; pass++) {
            float brightness = 0.76F;
            GL11.glColor4f(0.5F * brightness, 0.25F * brightness, 0.8F * brightness, 1F);
            GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glLoadIdentity();
            GL11.glScalef(0.33333334F, 0.33333334F, 0.33333334F);
            GL11.glRotatef(30F - pass * 60F, 0F, 0F, 1F);
            GL11.glTranslatef(0F, animationTime * (0.001F + pass * 0.003F) * 20F, 0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            renderBox(box);
        }
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    /**
     * The red flash of a hurt entity, also out of {@code RendererLivingEntity}.
     *
     * <p>Vanilla draws it over the main model and every armor pass <em>after</em>
     * {@code renderEquippedItems}, and it only ever re-renders models it knows about, so the breasts
     * stayed their normal colour while the rest of the player went red. Every box that could have won a
     * pixel is repeated here; {@code GL_EQUAL} then tints only the fragments that actually did.</p>
     */
    private static void renderHurtOverlay(AbstractClientPlayer entity, float partialTicks, ModelBox breast,
            ModelBox breastWear, ModelBox breastArmor, boolean left, boolean roundShape) {
        // Vanilla turns the lightmap off before its own copy of this pass, so the flash reads the same
        // in a cave as at noon. Leaving it on multiplies the red by the scene light: in the dark the
        // breasts came out all but black while the rest of the player went red.
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glColor4f(entity.getBrightness(partialTicks), 0F, 0F, 0.4F);

        renderBox(breast);
        if (breastWear != null) {
            GL11.glPushMatrix();
            if (!roundShape) {
                wearTransform();
            }
            renderBox(breastWear);
            GL11.glPopMatrix();
        }
        if (breastArmor != null) {
            GL11.glPushMatrix();
            if (!roundShape) {
                armorTransform(left);
            }
            renderBox(breastArmor);
            GL11.glPopMatrix();
        }

        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Draws a box under smooth shading.
     *
     * <p>Minecraft leaves the shade model wherever the terrain pass left it, and that is
     * {@code GL_FLAT} whenever smooth lighting is off. Flat shading takes one normal per quad and
     * throws the rest away, which turns the rounded mesh into a bag of visible facets; the boxes are
     * unaffected either way, since every vertex of a box face already shares one normal.</p>
     */
    private static void renderBox(ModelBox box) {
        int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        if (shadeModel != GL11.GL_SMOOTH) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (TexturedQuad quad : box.quads) {
            for (PositionTextureVertex vertex : quad.vertexPositions) {
                tess.setNormal(vertex.hasNormal ? vertex.normalX : quad.normalX,
                        vertex.hasNormal ? vertex.normalY : quad.normalY,
                        vertex.hasNormal ? vertex.normalZ : quad.normalZ);
                tess.addVertexWithUV(vertex.x / 16.0F, vertex.y / 16.0F, vertex.z / 16.0F, vertex.texturePositionX,
                        vertex.texturePositionY);
            }
        }
        tess.draw();
        if (shadeModel != GL11.GL_SMOOTH) {
            GL11.glShadeModel(shadeModel);
        }
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
