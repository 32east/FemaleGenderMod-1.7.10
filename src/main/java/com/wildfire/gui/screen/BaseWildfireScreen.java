package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireButton;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.UUID;

public abstract class BaseWildfireScreen extends GuiScreen {

    protected final GuiScreen parent;
    protected final UUID playerUUID;
    private final String screenTitle;

    protected BaseWildfireScreen(String title, GuiScreen parent, UUID uuid) {
        this.screenTitle = title;
        this.parent = parent;
        this.playerUUID = uuid;
    }

    public GenderPlayer getPlayer() {
        return WildfireGender.getOrAddPlayerById(playerUUID);
    }

    public String getScreenTitle() {
        return screenTitle;
    }

    protected EntityPlayer getPlayerEntity() {
        return mc.theWorld == null ? null : mc.theWorld.func_152378_a(playerUUID);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Draws the tooltip of whichever {@link WildfireButton} the cursor is over, if it has one.
     */
    protected void drawButtonTooltips(int mouseX, int mouseY) {
        @SuppressWarnings("unchecked")
        List<GuiButton> buttons = this.buttonList;
        for (int i = 0; i < buttons.size(); i++) {
            GuiButton button = buttons.get(i);
            if (!(button instanceof WildfireButton)) {
                continue;
            }
            WildfireButton wb = (WildfireButton) button;
            if (wb.visible && wb.isHovered() && wb.getTooltip() != null) {
                drawHoveringText(java.util.Collections.singletonList(wb.getTooltip()), mouseX, mouseY, fontRendererObj);
                return;
            }
        }
    }
}
