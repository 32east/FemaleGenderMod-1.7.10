package com.wildfire.debug;

import com.wildfire.main.Gender;
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

import java.io.File;
import java.util.List;

/**
 * Scripted smoke test: boots a flat world and writes a fixed set of screenshots, then quits.
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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ticks++;

        if (state >= 2 && state <= 8 && mc.thePlayer != null) {
            // Camera yaw stays put; only the body turns, so step 6 gives a side profile
            holdStill(mc.thePlayer, 0F, state == 6 ? 90F : 0F);
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
                if (mc.thePlayer != null && mc.theWorld != null && mc.currentScreen == null && ticks > 100) {
                    WildfireGender.LOGGER.info("[autotest] world ready");
                    reportSoundRegistration(mc);
                    reportTooltipSplitting();
                    mc.gameSettings.thirdPersonView = 2; // front-facing third person
                    mc.gameSettings.hideGUI = true;
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
                    shot(mc, "02-female-front");
                    configure(mc, Gender.FEMALE, 1.0F, false);
                    next();
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
                    shot(mc, "06-female-armor-front");
                    configure(mc, Gender.FEMALE, 0.8F, true);
                    next();
                }
                break;

            case 9: // physics: jump around and capture frames, once in iron and once in leather
                if (mc.thePlayer != null) {
                    mc.thePlayer.rotationPitch = 0;
                    if (mc.thePlayer.onGround && ticks % 18 == 0) {
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

            case 15:
                if (ticks > 5) {
                    hurtSelf(mc);
                    next();
                }
                break;

            case 16:
                if (ticks > 20) {
                    WildfireGender.LOGGER.info("[autotest] done, shutting down");
                    mc.shutdown();
                    state = 99;
                }
                break;

            default:
                break;
        }
    }

    private void next() {
        state++;
        ticks = 0;
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

    /** Confirms the lang files' line breaks survive translation and actually split. */
    private static void reportTooltipSplitting() {
        String raw = net.minecraft.util.StatCollector.translateToLocal("femalegender.tooltip.hide_in_armor");
        java.util.List<String> lines = com.wildfire.gui.screen.BaseWildfireScreen.splitLines(raw);
        WildfireGender.LOGGER.info("[autotest] tooltip lines=" + lines.size() + " " + lines);
    }

    /** Damages the player server-side so the real hurt-sound path runs. */
    @SuppressWarnings("unchecked")
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

    /** Runs after the mod's own handler, so it sees the final sound name. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundAtEntityEvent event) {
        if (event.name != null && event.name.contains("hurt")) {
            WildfireGender.LOGGER.info("[autotest] hurt sound on "
                    + (event.entity.worldObj.isRemote ? "client" : "server") + ": " + event.name);
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
