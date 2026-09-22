package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireButton;
import com.wildfire.main.Gender;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class WardrobeBrowserScreen extends BaseWildfireScreen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(WildfireGender.MODID, "textures/gui/wardrobe_bg.png");

    private static final int ID_GENDER = 0;
    private static final int ID_APPEARANCE = 1;
    private static final int ID_SETTINGS = 2;
    private static final int ID_EXIT = 3;

    private WildfireButton genderButton;

    public WardrobeBrowserScreen(GuiScreen parent, UUID uuid) {
        super(StatCollector.translateToLocal("femalegender.wardrobe.title"), parent, uuid);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int j = this.height / 2;
        GenderPlayer plr = getPlayer();

        genderButton = new WildfireButton(ID_GENDER, this.width / 2 - 42, j - 52, 158, 20, getGenderLabel(plr.getGender()));
        buttonList.add(genderButton);
        buttonList.add(new WildfireButton(ID_APPEARANCE, this.width / 2 - 42, j - 32, 158, 20,
                StatCollector.translateToLocal("femalegender.appearance_settings.title") + "..."));
        buttonList.add(new WildfireButton(ID_SETTINGS, this.width / 2 - 42, j - 12, 158, 20,
                StatCollector.translateToLocal("femalegender.char_settings.title") + "..."));
        buttonList.add(new WildfireButton(ID_EXIT, this.width / 2 + 111, j - 63, 9, 9, "X"));
    }

    private String getGenderLabel(Gender gender) {
        return StatCollector.translateToLocal("femalegender.label.gender") + " - " + gender.getDisplayName();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        GenderPlayer plr = getPlayer();
        switch (button.id) {
            case ID_GENDER: {
                Gender gender = plr.getGender().next();
                if (plr.updateGender(gender)) {
                    genderButton.displayString = getGenderLabel(gender);
                    GenderPlayer.saveGenderInfo(plr);
                }
                break;
            }
            case ID_APPEARANCE:
                mc.displayGuiScreen(new WildfireBreastCustomizationScreen(this, playerUUID));
                break;
            case ID_SETTINGS:
                mc.displayGuiScreen(new WildfireCharacterSettingsScreen(this, playerUUID));
                break;
            case ID_EXIT:
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        int i = (this.width - 248) / 2;
        int j = (this.height - 134) / 2;
        drawTexturedModalRect(i, j, 0, 0, 248, 156);

        int x = this.width / 2;
        int y = this.height / 2;
        fontRendererObj.drawString(getScreenTitle(), x - 42, y - 62, 4473924);

        EntityPlayer ent = getPlayerEntity();
        if (ent != null) {
            int xP = this.width / 2 - 82;
            int yP = this.height / 2 + 32;
            GuiInventory.func_147046_a(xP, yP, 45, xP - mouseX, yP - 76 - mouseY, ent);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawButtonTooltips(mouseX, mouseY);
    }
}
