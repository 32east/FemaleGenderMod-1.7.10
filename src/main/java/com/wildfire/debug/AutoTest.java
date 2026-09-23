package com.wildfire.debug;

import com.wildfire.main.Gender;
import com.wildfire.main.BreastShape;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.wildfire.main.WildfireSounds;
import cpw.mods.fml.common.eventhandler.EventPriority;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

import com.wildfire.render.GenderLayer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.List;

/**
 * Scripted smoke test: boots a flat world and writes a fixed set of screenshots, then quits. One step also
 * measures pixels: a held item must not darken the breasts of a player drawn at GUI size.
 *
 * <p>The shots are deliberately deterministic (fixed camera, physics off) so male/female pairs can be
 * diffed pixel by pixel; only the last few exercise the physics. Enabled with
 * {@code -Dfemalegender.autotest=true}, which {@code ./gradlew runClient -Pautotest} passes.</p>
 */
public class AutoTest {

    public static final boolean ENABLED = Boolean.getBoolean("femalegender.autotest");

    /** Each step holds the pose for this many ticks before the shot is taken. */
    private static final int SETTLE = 25;

    /** How long to wait at the menu before creating the world; big packs need longer. */
    private static final int MENU_WAIT = Integer.getInteger("femalegender.autotest.menuWait", 40);

    /** Existing save to load instead of creating a fresh flat world, if set. */
    private static final String WORLD = System.getProperty("femalegender.autotest.world", "fgmtest");

    private int state = 0;
    private int ticks = 0;
    private int bounceShots = 0;
    private boolean leatherRound = false;
    private int armorShot = 0;
    private int hurtShot = 0;

    private static final String[] HURT_SHOTS = {
            "11-female-hurt-flash", "11b-female-hurt-flash-round", "11c-female-hurt-flash-round-night" };
    private int roundShot = 0;

    /** Setups the held-item shading check measures, one per pass through its step. */
    private static final String[] SHADING_VARIANTS = {"classic", "round", "classic-iron"};
    private int shadingShot = 0;

    /** Offscreen frame for the held-item check: the player drawn as the inventory draws it, a bit larger. */
    private static final int PROBE_WIDTH = 160;
    private static final int PROBE_HEIGHT = 200;
    private static final int PROBE_SCALE = 80;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ticks++;

        if (state >= 2 && state <= 8 && mc.thePlayer != null) {
            // Camera yaw stays put; only the body turns, so the profile shots come for free
            holdStill(mc.thePlayer, 0F, bodyYaw());
        }

        switch (state) {
            case 0:
                // Big modpacks replace the main menu, so just wait for the game loop to settle down
                if (ticks > MENU_WAIT && mc.theWorld == null && mc.currentScreen != null) {
                    WildfireGender.LOGGER.info("[autotest] creating world " + WORLD
                            + " (menu: " + mc.currentScreen.getClass().getName() + ")");
                    WorldSettings settings = new WorldSettings(1234L, WorldSettings.GameType.CREATIVE, false, false,
                            WorldType.FLAT);
                    settings.enableCommands();
                    mc.launchIntegratedServer(WORLD, WORLD, settings);
                    next();
                }
                break;

            case 1:
                if (mc.thePlayer != null && mc.theWorld != null && ticks > 100) {
                    if (mc.currentScreen != null) {
                        // Packs open things on first join (GTNH pops the quest book); get it out of the way
                        WildfireGender.LOGGER
                                .info("[autotest] dismissing " + mc.currentScreen.getClass().getName());
                        mc.displayGuiScreen(null);
                    }
                    WildfireGender.LOGGER.info("[autotest] world ready");
                    reportDefaults();
                    reportSoundRegistration(mc);
                    reportTooltipSplitting();
                    reportSoundsJsonProviders(mc);
                    mc.gameSettings.thirdPersonView = 2; // front-facing third person
                    mc.gameSettings.hideGUI = true;
                    // Shots are compared against each other across runs, so neither the window the OS
                    // happens to open nor the user's field of view may decide the framing
                    mc.gameSettings.fovSetting = 40F;
                    resizeWindow(mc, 1280, 720);
                    // The hurt test ends at midnight, and a flat world at midnight spawns things that
                    // walk over and kill the subject mid-shot
                    setPeaceful();
                    // Without this an unfocused window opens the pause menu, which stops the
                    // integrated server: the world freezes and every physics frame comes out identical
                    mc.gameSettings.pauseOnLostFocus = false;
                    configure(mc, Gender.MALE, 0.8F, false);
                    next();
                }
                break;

            case 2: // baseline: no breasts at all
                if (ticks > SETTLE) {
                    shot(mc, "01-male-front");
                    configure(mc, Gender.FEMALE, 0.8F, false);
                    next();
                }
                break;

            case 3:
                if (ticks > SETTLE) {
                    // One pass per camera angle: a rounded shape is a volume, so the front view alone
                    // cannot tell a real curve from a flat plate with shading on it
                    switch (roundShot) {
                        case 0:
                            shot(mc, "02-female-front");
                            setShape(mc, BreastShape.MERGED);
                            break;
                        case 1:
                            shot(mc, "02b-female-merged-front");
                            break;
                        case 2:
                            shot(mc, "02c-female-merged-side");
                            break;
                        case 3:
                            shot(mc, "02d-female-merged-three-quarter");
                            setShape(mc, BreastShape.ROUND);
                            break;
                        case 4:
                            shot(mc, "02e-female-round-front");
                            break;
                        case 5:
                            shot(mc, "02f-female-round-side");
                            break;
                        default:
                            shot(mc, "02g-female-round-three-quarter");
                            setShape(mc, BreastShape.CLASSIC);
                            configure(mc, Gender.FEMALE, 1.0F, false);
                            next();
                            break;
                    }
                    if (state != 3) {
                        break;
                    }
                    roundShot++;
                    ticks = 0;
                }
                break;

            case 4:
                if (ticks > SETTLE) {
                    shot(mc, "03-female-front-max");
                    configure(mc, Gender.FEMALE, 0.8F, false);
                    mc.gameSettings.thirdPersonView = 1; // behind the player
                    next();
                }
                break;

            case 5:
                if (ticks > SETTLE) {
                    shot(mc, "04-female-back");
                    next();
                }
                break;

            case 6: // player turned 90 degrees, camera still behind: side profile
                if (ticks > SETTLE) {
                    shot(mc, "05-female-side");
                    giveChestplate(mc, net.minecraft.init.Items.iron_chestplate);
                    next();
                }
                break;

            case 7:
                if (ticks > SETTLE) {
                    mc.gameSettings.thirdPersonView = 2;
                    next();
                }
                break;

            case 8:
                if (ticks > SETTLE) {
                    if (armorShot == 0) {
                        shot(mc, "06-female-armor-front");
                        // The shimmer is a pass of its own over the armor box: the vanilla one runs
                        // before this layer is even called, so without it the breasts stay dull while
                        // the rest of the chestplate sparkles
                        enchantChestplate(mc);
                    } else if (armorShot == 1) {
                        shot(mc, "06b-female-armor-enchanted");
                        // Back to a plain chestplate, so the physics shots are not full of sparkles
                        giveChestplate(mc, net.minecraft.init.Items.iron_chestplate);
                        // The rounded layers nest by inflating along the normal instead of widening
                        // sideways, so the armor copies need a look of their own
                        setShape(mc, BreastShape.MERGED);
                    } else if (armorShot == 2) {
                        shot(mc, "06c-female-armor-merged");
                        setShape(mc, BreastShape.ROUND);
                    } else {
                        shot(mc, "06d-female-armor-round");
                        setShape(mc, BreastShape.CLASSIC);
                        configure(mc, Gender.FEMALE, 0.8F, true);
                        next();
                        break;
                    }
                    armorShot++;
                    ticks = 0;
                }
                break;

            case 9: // physics: jump around and capture frames, once in iron and once in leather
                if (mc.thePlayer != null) {
                    mc.thePlayer.rotationPitch = 0;
                    // jump() just sets upward motion, so it perturbs the physics even mid-air;
                    // requiring onGround left the player perfectly still in some worlds
                    if (ticks % 18 == 0) {
                        mc.thePlayer.jump();
                    }
                }
                if (ticks % 6 == 0) {
                    reportBounce(mc, leatherRound ? "leather" : "iron");
                    shot(mc, (leatherRound ? "07b-physics-leather-" : "07-physics-iron-") + bounceShots);
                    if (++bounceShots >= 6) {
                        if (leatherRound) {
                            next();
                        } else {
                            // Iron has physicsResistance 1, leather 0.3: the two rounds should differ
                            leatherRound = true;
                            bounceShots = 0;
                            ticks = 0;
                            giveChestplate(mc, net.minecraft.init.Items.leather_chestplate);
                        }
                    }
                }
                break;

            case 10:
                if (ticks > 10) {
                    mc.gameSettings.hideGUI = false;
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WildfirePlayerListScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 11:
                if (ticks > 15) {
                    shot(mc, "07b-gui-playerlist");
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WardrobeBrowserScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 12:
                if (ticks > 15) {
                    shot(mc, "08-gui-wardrobe");
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WildfireBreastCustomizationScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 13:
                if (ticks > 15) {
                    shot(mc, "09-gui-customization");
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WildfireCharacterSettingsScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 14:
                if (ticks > 15) {
                    shot(mc, "10-gui-settings");
                    mc.displayGuiScreen(null);
                    next();
                }
                break;

            case 15: // a held item must not darken the breasts of a player drawn at GUI size
                if (ticks == 1) {
                    setUpShading(mc, shadingShot);
                } else if (ticks > SETTLE) {
                    checkHeldItemShading(mc, SHADING_VARIANTS[shadingShot]);
                    if (++shadingShot < SHADING_VARIANTS.length) {
                        ticks = 0;
                    } else {
                        setShape(mc, BreastShape.CLASSIC);
                        giveChestplate(mc, net.minecraft.init.Items.leather_chestplate);
                        next();
                    }
                }
                break;

            case 16:
                // Damage inside the invulnerability window is swallowed, so the second round waits it out
                if (ticks > (hurtShot == 0 ? 5 : 25)) {
                    mc.gameSettings.hideGUI = true;
                    hurtSelf(mc);
                    next();
                }
                break;

            case 17:
                // hurtTime counts ten ticks down from the hit and the flash comes back from the
                // server, so a couple of ticks in is the safe place to catch it
                if (ticks > 3) {
                    WildfireGender.LOGGER.info("[autotest] hurtTime=" + mc.thePlayer.hurtTime
                            + " shape=" + hurtShape());
                    shot(mc, HURT_SHOTS[hurtShot]);
                    if (hurtShot == 0) {
                        // The red flash is a second pass of the same geometry under GL_EQUAL, so it is
                        // worth proving on the rounded mesh and not just on the box
                        setShape(mc, BreastShape.ROUND);
                    } else if (hurtShot == 1) {
                        // And again in the dark: the flash is meant to read the same whatever the light,
                        // so this is where a pass that forgot to turn the lightmap off shows up
                        setNight(mc);
                    }
                    if (hurtShot < HURT_SHOTS.length - 1) {
                        hurtShot++;
                        state = 16;
                        ticks = 0;
                    } else {
                        next();
                    }
                }
                break;

            case 18: // fall damage: falling plays a fall sound of its own as well as the hurt sound
                if (ticks > 20) {
                    dropFromHeight(mc, 14);
                    next();
                }
                break;

            case 19:
                if (ticks > 80) {
                    WildfireGender.LOGGER.info("[autotest] done, shutting down");
                    mc.shutdown();
                    state = 99;
                }
                break;

            default:
                break;
        }
    }

    /** Body yaw for the current step; the camera never moves, so this is what frames each shot. */
    private float bodyYaw() {
        if (state == 6 || (state == 3 && (roundShot == 2 || roundShot == 5))) {
            return 90F;
        }
        if (state == 3 && (roundShot == 3 || roundShot == 6)) {
            return 40F;
        }
        return 0F;
    }

    private String hurtShape() {
        return WildfireGender.getOrAddPlayerById(Minecraft.getMinecraft().thePlayer.getUniqueID())
                .getBreasts().getShape().name();
    }

    private static void setShape(Minecraft mc, BreastShape shape) {
        WildfireGender.getOrAddPlayerById(mc.thePlayer.getUniqueID()).getBreasts().updateShape(shape);
    }

    private void next() {
        state++;
        ticks = 0;
    }

    private static void resizeWindow(Minecraft mc, int width, int height) {
        try {
            org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(width, height));
            mc.resize(width, height);
        } catch (Exception e) {
            WildfireGender.LOGGER.warn("[autotest] could not resize to " + width + "x" + height, e);
        }
    }

    private static void holdStill(EntityPlayer player, float cameraYaw, float bodyYaw) {
        player.rotationYaw = cameraYaw;
        player.prevRotationYaw = cameraYaw;
        player.rotationYawHead = bodyYaw;
        player.prevRotationYawHead = bodyYaw;
        player.renderYawOffset = bodyYaw;
        player.prevRenderYawOffset = bodyYaw;
        player.rotationPitch = 0;
        player.prevRotationPitch = 0;
    }

    private static void configure(Minecraft mc, Gender gender, float bust, boolean physics) {
        GenderPlayer plr = WildfireGender.getOrAddPlayerById(mc.thePlayer.getUniqueID());
        plr.updateGender(gender);
        plr.updateBustSize(bust);
        plr.updateBreastPhysics(physics);
        plr.updateArmorBreastPhysics(true);
        plr.updateBounceMultiplier(0.6F);
        plr.getBreasts().updateUniboob(!physics);
        WildfireGender.LOGGER.info("[autotest] gender=" + gender + " bust=" + bust + " physics=" + physics);
    }

    private static void giveChestplate(Minecraft mc, net.minecraft.item.Item chestplate) {
        mc.thePlayer.inventory.armorInventory[2] = new net.minecraft.item.ItemStack(chestplate);
        com.wildfire.api.IGenderArmor armor =
                com.wildfire.main.WildfireHelper.getArmorConfig(mc.thePlayer.getEquipmentInSlot(3));
        GenderPlayer plr = WildfireGender.getOrAddPlayerById(mc.thePlayer.getUniqueID());
        float resistance = com.wildfire.main.WildfireHelper.clamp(armor.physicsResistance(), 0, 1);
        boolean bounce = plr.hasBreastPhysics()
                && (!armor.coversBreasts() || (plr.hasArmorBreastPhysics() && resistance < 1));
        WildfireGender.LOGGER.info("[autotest] equipped " + chestplate.getUnlocalizedName()
                + " resistance=" + resistance + " tightness=" + armor.tightness() + " bounceEnabled=" + bounce);
    }

    /** Enchants the worn chestplate, so the glint pass over the breasts gets exercised. */
    private static void enchantChestplate(Minecraft mc) {
        net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.armorInventory[2];
        if (stack == null) {
            WildfireGender.LOGGER.warn("[autotest] no chestplate to enchant");
            return;
        }
        stack.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking, 3);
        WildfireGender.LOGGER.info("[autotest] enchanted " + stack.getUnlocalizedName()
                + " isItemEnchanted=" + stack.isItemEnchanted());
    }

    /** Logs the live bounce offset, so "static in armor" can be checked as a number. */
    private static void reportBounce(Minecraft mc, String label) {
        GenderPlayer plr = WildfireGender.getPlayerById(mc.thePlayer.getUniqueID());
        if (plr != null) {
            WildfireGender.LOGGER.info(String.format("[autotest] bounce(%s) y=%.4f rot=%.4f", label,
                    plr.getLeftBreastPhysics().getBounceY(), plr.getLeftBreastPhysics().getBounceRotation()));
        }
    }

    /** Confirms the sounds.json in our jar was actually parsed and registered. */
    private static void reportSoundRegistration(Minecraft mc) {
        try {
            boolean one = mc.getSoundHandler()
                    .getSound(new ResourceLocation(WildfireSounds.FEMALE_HURT1)) != null;
            boolean two = mc.getSoundHandler()
                    .getSound(new ResourceLocation(WildfireSounds.FEMALE_HURT2)) != null;
            WildfireGender.LOGGER.info("[autotest] sounds registered: hurt1=" + one + " hurt2=" + two);
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] sound lookup failed", t);
        }
    }

    /**
     * Lists every resource pack that answers for our sounds.json.
     *
     * <p>Some modpacks log "Invalid sounds.json" once our domain exists: a second provider answers
     * for the same path with something that is not JSON. Our own copy still registers, but this
     * says who the other one is.</p>
     */
    private static void reportSoundsJsonProviders(Minecraft mc) {
        try {
            ResourceLocation loc = new ResourceLocation(WildfireGender.MODID, "sounds.json");
            java.util.List<?> resources = mc.getResourceManager().getAllResources(loc);
            WildfireGender.LOGGER.info("[autotest] " + resources.size() + " provider(s) answer for " + loc);
            for (Object o : resources) {
                net.minecraft.client.resources.IResource resource = (net.minecraft.client.resources.IResource) o;
                byte[] head = new byte[40];
                int read = resource.getInputStream().read(head);
                // 1.7.10's IResource does not carry the pack name, so identify it by its content
                String preview = read > 0 ? new String(head, 0, read, "UTF-8").trim() : "<empty>";
                WildfireGender.LOGGER.info("[autotest] sounds.json starts with: " + preview);
            }
        } catch (Throwable t) {
            WildfireGender.LOGGER.warn("[autotest] could not list sounds.json providers", t);
        }
    }

    /** Confirms the lang files' line breaks survive translation and actually split. */
    private static void reportTooltipSplitting() {
        String raw = net.minecraft.util.StatCollector.translateToLocal("femalegender.tooltip.hide_in_armor");
        java.util.List<String> lines = com.wildfire.gui.screen.BaseWildfireScreen.splitLines(raw);
        WildfireGender.LOGGER.info("[autotest] tooltip lines=" + lines.size() + " " + lines);
    }

    /** Damages the player server-side so the real hurt-sound path runs. */
    @SuppressWarnings("unchecked")
    /**
     * What a player with no profile yet actually gets, printed the way the appearance screen labels it.
     *
     * <p>Defaults only apply to a profile that does not exist yet, so reading them off a fresh player is
     * the only check that means anything -- the test account already has a file on disk.</p>
     */
    private static void reportDefaults() {
        GenderPlayer fresh = new GenderPlayer(java.util.UUID.randomUUID());
        com.wildfire.main.Breasts b = fresh.getBreasts();
        WildfireGender.LOGGER.info(String.format(
                "[autotest] defaults: size=%d%% separation=%d height=%d depth=%d rotation=%d dualPhysics=%s shape=%s",
                Math.round(fresh.getBustSize() * 100F),
                Math.round(b.getXOffset() * 10F), Math.round(b.getYOffset() * 10F),
                Math.round(b.getZOffset() * 10F), Math.round(b.getCleavage() * 100F),
                b.isUniboob() ? "no" : "yes", b.getShape().name()));
    }

    private static void setPeaceful() {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                // setDifficultyForAllWorlds; the name is unmapped in this MCP version
                server.func_147139_a(net.minecraft.world.EnumDifficulty.PEACEFUL);
            }
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] could not set the difficulty", t);
        }
    }

    /** Midnight, so the lightmap is dark enough to tell a lit pass from an unlit one. */
    private static void setNight(Minecraft mc) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                for (net.minecraft.world.WorldServer world : server.worldServers) {
                    world.setWorldTime(18000L);
                }
            }
            if (mc.theWorld != null) {
                mc.theWorld.setWorldTime(18000L);
            }
            WildfireGender.LOGGER.info("[autotest] world set to midnight");
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] could not set the time", t);
        }
    }

    /** Drops the player from a height in survival, which is the only way fall damage happens at all. */
    private static void dropFromHeight(Minecraft mc, int height) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                return;
            }
            List<EntityPlayerMP> list = server.getConfigurationManager().playerEntityList;
            if (list.isEmpty()) {
                return;
            }
            EntityPlayerMP player = list.get(0);
            // Creative flight cancels the fall before any of this runs
            player.setGameType(WorldSettings.GameType.SURVIVAL);
            player.capabilities.disableDamage = false;
            player.setHealth(player.getMaxHealth());
            player.setPositionAndUpdate(player.posX, player.posY + height, player.posZ);
            WildfireGender.LOGGER.info("[autotest] dropping " + player.getCommandSenderName() + " from +" + height);
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] fall test failed", t);
        }
    }

    private static void hurtSelf(Minecraft mc) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null) {
                WildfireGender.LOGGER.info("[autotest] no integrated server, skipping hurt test");
                return;
            }
            List<EntityPlayerMP> list = server.getConfigurationManager().playerEntityList;
            if (list.isEmpty()) {
                return;
            }
            EntityPlayerMP player = list.get(0);
            player.capabilities.disableDamage = false;
            WildfireGender.LOGGER.info("[autotest] hurting " + player.getCommandSenderName());
            player.attackEntityFrom(DamageSource.generic, 2.0F);
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] hurt test failed", t);
        }
    }

    /**
     * Says whether anything swallowed a hit before vanilla could voice it.
     *
     * <p>Vanilla plays the hurt sound from {@code attackEntityFrom}, and only if the hit gets that far:
     * a cancelled {@code LivingAttackEvent} returns before the sound, and so does a second hit inside
     * the ten-tick invulnerability window. A pack whose extra hearts take the damage themselves leaves
     * nothing for the sound swap to work on, and this line is what tells the two cases apart.</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }
        WildfireGender.LOGGER.info("[autotest] attack " + event.source.damageType
                + " amount=" + event.ammount
                + " cancelled=" + event.isCanceled()
                + " hurtResistant=" + ((EntityPlayer) event.entity).hurtResistantTime
                + " on " + (event.entity.worldObj.isRemote ? "client" : "server"));
    }

    /** Runs after the mod's own handler, so it sees the final sound name. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundAtEntityEvent event) {
        if (event.name != null && (event.name.contains("hurt") || state >= 18)) {
            WildfireGender.LOGGER.info("[autotest] sound on "
                    + (event.entity.worldObj.isRemote ? "client" : "server") + ": " + event.name
                    + (event.isCanceled() ? " (cancelled)" : ""));
        }
    }

    /** Bust, shape and armor for one pass of the held-item check; the size needs ticks to settle after. */
    private static void setUpShading(Minecraft mc, int variant) {
        configure(mc, Gender.FEMALE, 0.8F, false);
        setShape(mc, variant == 1 ? BreastShape.ROUND : BreastShape.CLASSIC);
        if (variant == 2) {
            giveChestplate(mc, net.minecraft.init.Items.iron_chestplate);
        } else {
            mc.thePlayer.inventory.armorInventory[2] = null;
        }
    }

    /**
     * Holding an item must not darken the breasts.
     *
     * <p>{@code ItemRenderer} switches {@code GL_RESCALE_NORMAL} off after drawing a flat item, and this
     * layer draws after the held item. In the world that hardly shows, but a GUI draws the player tens of
     * times larger -- 30 in the inventory, 200 in the customization screen, more in a HUD doll -- and without
     * the rescale the normals shrink to next to nothing, leaving the breasts only the ambient light. The
     * player is drawn the way the inventory draws it, offscreen, with and without a stick in hand; the breast
     * pixels are the ones that change when this layer is left out, minus the ones the stick and its arm
     * change.</p>
     */
    private static void checkHeldItemShading(Minecraft mc, String variant) {
        InventoryPlayer inventory = mc.thePlayer.inventory;
        ItemStack held = inventory.mainInventory[inventory.currentItem];
        try {
            inventory.mainInventory[inventory.currentItem] = null;
            int[] bare = renderLikeInventory(mc);
            GenderLayer.hiddenForTest = true;
            int[] bareNoLayer = renderLikeInventory(mc);
            inventory.mainInventory[inventory.currentItem] = new ItemStack(Items.stick);
            int[] holdingNoLayer = renderLikeInventory(mc);
            GenderLayer.hiddenForTest = false;
            int[] holding = renderLikeInventory(mc);

            long lumBare = 0;
            long lumHolding = 0;
            int pixels = 0;
            int changed = 0;
            for (int i = 0; i < bare.length; i++) {
                if (bare[i] != bareNoLayer[i] && bareNoLayer[i] == holdingNoLayer[i]) {
                    lumBare += luminance(bare[i]);
                    lumHolding += luminance(holding[i]);
                    pixels++;
                    if (bare[i] != holding[i]) {
                        changed++;
                    }
                }
            }
            double ratio = lumBare == 0 ? 0 : (double) lumHolding / lumBare;
            boolean pass = pixels > 100 && Math.abs(ratio - 1) < 0.01;
            WildfireGender.LOGGER.info(String.format("[autotest] %s a held item leaves the breast shading alone (%s):"
                    + " %d breast pixels, %d changed, brightness with a stick %.1f%% of without",
                    pass ? "PASS" : "FAIL", variant, pixels, changed, ratio * 100));
            saveProbe(mc, bare, "12-held-item-" + variant + "-empty-hand");
            saveProbe(mc, holding, "12-held-item-" + variant + "-stick");
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] FAIL held item check (" + variant + ") threw", t);
        } finally {
            GenderLayer.hiddenForTest = false;
            inventory.mainInventory[inventory.currentItem] = held;
        }
    }

    private static int luminance(int argb) {
        return ((argb >> 16 & 255) * 299 + (argb >> 8 & 255) * 587 + (argb & 255) * 114) / 1000;
    }

    /** Draws the player the way the inventory screen does, into a framebuffer of its own, and reads it back. */
    private static int[] renderLikeInventory(Minecraft mc) {
        Framebuffer framebuffer = new Framebuffer(PROBE_WIDTH, PROBE_HEIGHT, true);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            framebuffer.setFramebufferColor(0F, 0F, 0F, 0F);
            framebuffer.framebufferClear();
            framebuffer.bindFramebuffer(true);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, PROBE_WIDTH, PROBE_HEIGHT, 0, 1000, 3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(0F, 0F, -2000F);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GuiInventory.func_147046_a(PROBE_WIDTH / 2, PROBE_HEIGHT - 20, PROBE_SCALE, 0F, 0F, mc.thePlayer);
            IntBuffer buffer = BufferUtils.createIntBuffer(PROBE_WIDTH * PROBE_HEIGHT);
            GL11.glReadPixels(0, 0, PROBE_WIDTH, PROBE_HEIGHT, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
            int[] pixels = new int[PROBE_WIDTH * PROBE_HEIGHT];
            buffer.get(pixels);
            return pixels;
        } finally {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            mc.getFramebuffer().bindFramebuffer(true);
        }
    }

    private static void saveProbe(Minecraft mc, int[] pixels, String name) {
        try {
            BufferedImage image = new BufferedImage(PROBE_WIDTH, PROBE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < PROBE_HEIGHT; y++) {
                // glReadPixels starts from the bottom row
                image.setRGB(0, PROBE_HEIGHT - 1 - y, PROBE_WIDTH, 1, pixels, y * PROBE_WIDTH, PROBE_WIDTH);
            }
            File dir = new File(mc.mcDataDir, "screenshots");
            dir.mkdirs();
            ImageIO.write(image, "png", new File(dir, name + ".png"));
        } catch (Throwable t) {
            WildfireGender.LOGGER.warn("[autotest] could not save " + name, t);
        }
    }

    private void shot(Minecraft mc, String name) {
        try {
            File dir = mc.mcDataDir;
            ScreenShotHelper.saveScreenshot(dir, name + ".png", mc.displayWidth, mc.displayHeight,
                    mc.getFramebuffer());
            WildfireGender.LOGGER.info("[autotest] shot " + name);
        } catch (Throwable t) {
            WildfireGender.LOGGER.error("[autotest] screenshot failed", t);
        }
    }
}
