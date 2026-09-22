package com.wildfire.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

/**
 * Flat translucent button, matching the look of the upstream mod rather than the vanilla 3D one.
 */
public class WildfireButton extends GuiButton {

    private boolean transparent = false;
    private String tooltip = null;

    public WildfireButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public WildfireButton setTransparent(boolean b) {
        this.transparent = b;
        return this;
    }

    public WildfireButton setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getX() {
        return xPosition;
    }

    public int getY() {
        return yPosition;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHovered() {
        return field_146123_n;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width
                && mouseY < yPosition + height;

        int clr = 0x444444 + (84 << 24);
        if (field_146123_n) {
            clr = 0x666666 + (84 << 24);
        }
        if (!enabled) {
            clr = 0x222222 + (84 << 24);
        }
        if (!transparent) {
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, clr);
        }

        font.drawStringWithShadow(displayString,
                xPosition + (width / 2) - (font.getStringWidth(displayString) / 2) + 1,
                yPosition + (int) Math.ceil(height / 2f) - font.FONT_HEIGHT / 2,
                enabled ? 0xFFFFFF : 0x666666);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
