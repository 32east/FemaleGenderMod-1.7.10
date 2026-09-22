package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireButton;
import com.wildfire.gui.WildfireSlider;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.Configuration;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class WildfireCharacterSettingsScreen extends BaseWildfireScreen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(WildfireGender.MODID, "textures/gui/settings_bg.png");

    private static final int ID_EXIT = 0;
    private static final int ID_PHYSICS = 1;
    private static final int ID_ARMOR_PHYSICS = 2;
    private static final int ID_HIDE_IN_ARMOR = 3;
    private static final int ID_HURT_SOUNDS = 4;

    private WildfireButton physicsButton, armorPhysicsButton, hideInArmorButton, hurtSoundsButton;
    private WildfireSlider bounceSlider, floppySlider;
    private int yPos = 0;
    private boolean bounceWarning;

    public WildfireCharacterSettingsScreen(GuiScreen parent, UUID uuid) {
        super(StatCollector.translateToLocal("femalegender.char_settings.title"), parent, uuid);
    }

    private static String enabled() {
        return EnumChatFormatting.GREEN + StatCollector.translateToLocal("femalegender.label.enabled")
                + EnumChatFormatting.RESET;
    }

    private static String disabled() {
        return EnumChatFormatting.RED + StatCollector.translateToLocal("femalegender.label.disabled")
                + EnumChatFormatting.RESET;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        final GenderPlayer aPlr = getPlayer();

        int x = this.width / 2;
        int y = this.height / 2;
        yPos = y - 47;
        int xPos = x - 156 / 2 - 1;

        buttonList.add(new WildfireButton(ID_EXIT, this.width / 2 + 73, yPos - 11, 9, 9, "X"));

        physicsButton = new WildfireButton(ID_PHYSICS, xPos, yPos, 157, 20,
                StatCollector.translateToLocalFormatted("femalegender.char_settings.physics",
                        aPlr.hasBreastPhysics() ? enabled() : disabled()))
                                .setTooltip(StatCollector.translateToLocal("femalegender.tooltip.breast_physics"));
        buttonList.add(physicsButton);

        armorPhysicsButton = new WildfireButton(ID_ARMOR_PHYSICS, xPos, yPos + 20, 157, 20,
                StatCollector.translateToLocalFormatted("femalegender.char_settings.armor_physics",
                        aPlr.hasArmorBreastPhysics() ? enabled() : disabled()))
                                .setTooltip(StatCollector.translateToLocal("femalegender.tooltip.armor_physics"));
        buttonList.add(armorPhysicsButton);

        hideInArmorButton = new WildfireButton(ID_HIDE_IN_ARMOR, xPos, yPos + 40, 157, 20,
                StatCollector.translateToLocalFormatted("femalegender.char_settings.hide_in_armor",
                        aPlr.showBreastsInArmor() ? disabled() : enabled()))
                                .setTooltip(StatCollector.translateToLocal("femalegender.tooltip.hide_in_armor"));
        buttonList.add(hideInArmorButton);

        bounceSlider = new WildfireSlider(100, xPos, yPos + 60, 158, 22, Configuration.BOUNCE_MULTIPLIER,
                aPlr.getBounceMultiplierRaw(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        float bounceText = 3 * value;
                        float v = Math.round(bounceText * 10) / 10f;
                        bounceWarning = v > 1;
                        if (v == 3) {
                            return StatCollector.translateToLocal("femalegender.slider.max_bounce");
                        } else if (Math.round(bounceText * 100) / 100f == 0) {
                            return StatCollector.translateToLocal("femalegender.slider.min_bounce");
                        }
                        return StatCollector.translateToLocalFormatted("femalegender.slider.bounce", v);
                    }
                }, new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        if (aPlr.updateBounceMultiplier(value)) {
                            GenderPlayer.saveGenderInfo(aPlr);
                        }
                    }
                });
        buttonList.add(bounceSlider);

        floppySlider = new WildfireSlider(101, xPos, yPos + 80, 158, 22, Configuration.FLOPPY_MULTIPLIER,
                aPlr.getFloppiness(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.slider.floppy",
                                Math.round(value * 100));
                    }
                }, new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        if (aPlr.updateFloppiness(value)) {
                            GenderPlayer.saveGenderInfo(aPlr);
                        }
                    }
                });
        buttonList.add(floppySlider);

        hurtSoundsButton = new WildfireButton(ID_HURT_SOUNDS, xPos, yPos + 100, 157, 20,
                StatCollector.translateToLocalFormatted("femalegender.char_settings.hurt_sounds",
                        aPlr.hasHurtSounds() ? enabled() : disabled()))
                                .setTooltip(StatCollector.translateToLocal("femalegender.tooltip.hurt_sounds"));
        buttonList.add(hurtSoundsButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        GenderPlayer aPlr = getPlayer();
        switch (button.id) {
            case ID_EXIT:
                mc.displayGuiScreen(parent);
                break;
            case ID_PHYSICS: {
                boolean enable = !aPlr.hasBreastPhysics();
                if (aPlr.updateBreastPhysics(enable)) {
                    physicsButton.displayString = StatCollector
                            .translateToLocalFormatted("femalegender.char_settings.physics",
                                    enable ? enabled() : disabled());
                    GenderPlayer.saveGenderInfo(aPlr);
                }
                break;
            }
            case ID_ARMOR_PHYSICS: {
                boolean enable = !aPlr.hasArmorBreastPhysics();
                if (aPlr.updateArmorBreastPhysics(enable)) {
                    armorPhysicsButton.displayString = StatCollector
                            .translateToLocalFormatted("femalegender.char_settings.armor_physics",
                                    enable ? enabled() : disabled());
                    GenderPlayer.saveGenderInfo(aPlr);
                }
                break;
            }
            case ID_HIDE_IN_ARMOR: {
                boolean show = !aPlr.showBreastsInArmor();
                if (aPlr.updateShowBreastsInArmor(show)) {
                    hideInArmorButton.displayString = StatCollector
                            .translateToLocalFormatted("femalegender.char_settings.hide_in_armor",
                                    show ? disabled() : enabled());
                    GenderPlayer.saveGenderInfo(aPlr);
                }
                break;
            }
            case ID_HURT_SOUNDS: {
                boolean enable = !aPlr.hasHurtSounds();
                if (aPlr.updateHurtSounds(enable)) {
                    hurtSoundsButton.displayString = StatCollector
                            .translateToLocalFormatted("femalegender.char_settings.hurt_sounds",
                                    enable ? enabled() : disabled());
                    GenderPlayer.saveGenderInfo(aPlr);
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        super.mouseMovedOrUp(mouseX, mouseY, which);
        if (which == 0) {
            bounceSlider.save();
            floppySlider.save();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        int i = (this.width - 172) / 2;
        int j = (this.height - 124) / 2;
        drawTexturedModalRect(i, j, 0, 0, 172, 144);

        int x = this.width / 2;
        int y = this.height / 2;
        fontRendererObj.drawString(getScreenTitle(), x - 79, yPos - 10, 4473924);

        super.drawScreen(mouseX, mouseY, partialTicks);

        EntityPlayer plrEntity = getPlayerEntity();
        if (plrEntity != null) {
            drawCenteredString(fontRendererObj, plrEntity.getCommandSenderName(), x, yPos - 30, 0xFFFFFF);
        }
        if (bounceWarning) {
            drawCenteredString(fontRendererObj,
                    EnumChatFormatting.ITALIC + StatCollector.translateToLocal("femalegender.tooltip.bounce_warning"),
                    x, y + 90, 0xFF6666);
        }
        drawButtonTooltips(mouseX, mouseY);
    }
}
