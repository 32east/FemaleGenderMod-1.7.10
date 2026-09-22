package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireButton;
import com.wildfire.gui.WildfireSlider;
import com.wildfire.main.Breasts;
import com.wildfire.main.BreastShape;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.config.Configuration;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class WildfireBreastCustomizationScreen extends BaseWildfireScreen {

    private static final int ID_EXIT = 0;
    private static final int ID_UNIBOOB = 1;
    private static final int ID_SHAPE = 2;

    private WildfireSlider breastSlider, xOffsetSlider, yOffsetSlider, zOffsetSlider, cleavageSlider;
    private WildfireButton uniboobButton, shapeButton;

    public WildfireBreastCustomizationScreen(GuiScreen parent, UUID uuid) {
        super(StatCollector.translateToLocal("femalegender.appearance_settings.title"), parent, uuid);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int j = this.height / 2;

        final GenderPlayer plr = getPlayer();
        final Breasts breasts = plr.getBreasts();
        WildfireSlider.ValueConsumer onSave = new WildfireSlider.ValueConsumer() {
            @Override
            public void accept(float value) {
                GenderPlayer.saveGenderInfo(plr);
            }
        };

        buttonList.add(new WildfireButton(ID_EXIT, this.width / 2 + 178, j - 61, 9, 9, "X"));

        breastSlider = new WildfireSlider(100, this.width / 2 + 30, j - 48, 158, 20, Configuration.BUST_SIZE,
                plr.getBustSize(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        plr.updateBustSize(value);
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.wardrobe.slider.breast_size",
                                Math.round(value * 100));
                    }
                }, onSave);

        xOffsetSlider = new WildfireSlider(101, this.width / 2 + 30, j - 27, 158, 20, Configuration.BREASTS_OFFSET_X,
                breasts.getXOffset(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        breasts.updateXOffset(value);
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.wardrobe.slider.separation",
                                Math.round((Math.round(value * 100f) / 100f) * 10));
                    }
                }, onSave);

        yOffsetSlider = new WildfireSlider(102, this.width / 2 + 30, j - 6, 158, 20, Configuration.BREASTS_OFFSET_Y,
                breasts.getYOffset(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        breasts.updateYOffset(value);
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.wardrobe.slider.height",
                                Math.round((Math.round(value * 100f) / 100f) * 10));
                    }
                }, onSave);

        zOffsetSlider = new WildfireSlider(103, this.width / 2 + 30, j + 15, 158, 20, Configuration.BREASTS_OFFSET_Z,
                breasts.getZOffset(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        breasts.updateZOffset(value);
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.wardrobe.slider.depth",
                                Math.round((Math.round(value * 100f) / 100f) * 10));
                    }
                }, onSave);

        cleavageSlider = new WildfireSlider(104, this.width / 2 + 30, j + 36, 158, 20, Configuration.BREASTS_CLEAVAGE,
                breasts.getCleavage(), new WildfireSlider.ValueConsumer() {
                    @Override
                    public void accept(float value) {
                        breasts.updateCleavage(value);
                    }
                }, new WildfireSlider.LabelFormatter() {
                    @Override
                    public String format(float value) {
                        return StatCollector.translateToLocalFormatted("femalegender.wardrobe.slider.rotation",
                                Math.round((Math.round(value * 100f) / 100f) * 100));
                    }
                }, onSave);

        buttonList.add(breastSlider);
        buttonList.add(xOffsetSlider);
        buttonList.add(yOffsetSlider);
        buttonList.add(zOffsetSlider);
        buttonList.add(cleavageSlider);

        uniboobButton = new WildfireButton(ID_UNIBOOB, this.width / 2 + 30, j + 57, 158, 20, dualPhysicsLabel(breasts));
        buttonList.add(uniboobButton);

        shapeButton = new WildfireButton(ID_SHAPE, this.width / 2 + 30, j + 78, 158, 20, shapeLabel(breasts));
        buttonList.add(shapeButton);
    }

    private static String dualPhysicsLabel(Breasts breasts) {
        return StatCollector.translateToLocalFormatted("femalegender.breast_customization.dual_physics",
                StatCollector.translateToLocal(breasts.isUniboob() ? "femalegender.label.no" : "femalegender.label.yes"));
    }

    private static String shapeLabel(Breasts breasts) {
        BreastShape shape = breasts.getShape();
        return StatCollector.translateToLocalFormatted("femalegender.breast_customization.shape",
                StatCollector.translateToLocal(shape.getTranslationKey()));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        GenderPlayer plr = getPlayer();
        Breasts breasts = plr.getBreasts();
        if (button.id == ID_EXIT) {
            mc.displayGuiScreen(parent);
        } else if (button.id == ID_UNIBOOB) {
            if (breasts.updateUniboob(!breasts.isUniboob())) {
                uniboobButton.displayString = dualPhysicsLabel(breasts);
                GenderPlayer.saveGenderInfo(plr);
            }
        } else if (button.id == ID_SHAPE) {
            if (breasts.updateShape(breasts.getShape().next())) {
                shapeButton.displayString = shapeLabel(breasts);
                GenderPlayer.saveGenderInfo(plr);
            }
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        super.mouseMovedOrUp(mouseX, mouseY, which);
        if (which == 0) {
            breastSlider.save();
            xOffsetSlider.save();
            yOffsetSlider.save();
            zOffsetSlider.save();
            cleavageSlider.save();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GenderPlayer plr = getPlayer();
        EntityPlayer ent = getPlayerEntity();
        if (ent != null) {
            int xP = this.width / 2 - 102;
            int yP = this.height / 2 + 275;
            GuiInventory.func_147046_a(xP, yP, 200, -20, -20, ent);
        }

        boolean canHaveBreasts = plr.getGender().canHaveBreasts();
        breastSlider.visible = canHaveBreasts;
        xOffsetSlider.visible = canHaveBreasts;
        yOffsetSlider.visible = canHaveBreasts;
        zOffsetSlider.visible = canHaveBreasts;
        cleavageSlider.visible = canHaveBreasts;
        uniboobButton.visible = canHaveBreasts;
        shapeButton.visible = canHaveBreasts;

        int x = this.width / 2;
        int y = this.height / 2;
        drawRect(x + 28, y - 64, x + 190, y + 100, 0x55000000);
        drawRect(x + 29, y - 63, x + 189, y - 50, 0x55000000);
        fontRendererObj.drawString(getScreenTitle(), x + 32, y - 60, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawButtonTooltips(mouseX, mouseY);
    }
}
