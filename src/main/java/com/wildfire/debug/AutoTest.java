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

import java.io.File;

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
                    giveChestplate(mc);
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

            case 9: // physics: jump around and capture a few frames
                if (mc.thePlayer != null) {
                    mc.thePlayer.rotationPitch = 0;
                    if (mc.thePlayer.onGround && ticks % 18 == 0) {
                        mc.thePlayer.jump();
                    }
                }
                if (ticks % 6 == 0) {
                    shot(mc, "07-physics-" + bounceShots);
                    if (++bounceShots >= 6) {
                        next();
                    }
                }
                break;

            case 10:
                if (ticks > 10) {
                    mc.gameSettings.hideGUI = false;
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WardrobeBrowserScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 11:
                if (ticks > 15) {
                    shot(mc, "08-gui-wardrobe");
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WildfireBreastCustomizationScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 12:
                if (ticks > 15) {
                    shot(mc, "09-gui-customization");
                    mc.displayGuiScreen(new com.wildfire.gui.screen.WildfireCharacterSettingsScreen(null,
                            mc.thePlayer.getUniqueID()));
                    next();
                }
                break;

            case 13:
                if (ticks > 15) {
                    shot(mc, "10-gui-settings");
                    mc.displayGuiScreen(null);
                    next();
                }
                break;

            case 14:
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

    private static void giveChestplate(Minecraft mc) {
        mc.thePlayer.inventory.armorInventory[2] =
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_chestplate);
        WildfireGender.LOGGER.info("[autotest] equipped iron chestplate");
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
