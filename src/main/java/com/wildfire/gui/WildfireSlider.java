package com.wildfire.gui;

import com.wildfire.main.config.FloatConfigKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

/**
 * Horizontal slider over a {@link FloatConfigKey} range.
 *
 * <p>Dragging is handled the way vanilla 1.7.10 sliders do it: a {@code dragging} flag set on press
 * and re-sampled every frame, so the screen does not need to forward drag events.</p>
 */
public class WildfireSlider extends GuiButton {

    /** Receives the value as the user drags. */
    public interface ValueConsumer {
        void accept(float value);
    }

    /** Turns the current value into the label drawn on the slider. */
    public interface LabelFormatter {
        String format(float value);
    }

    private double value;
    private final double minValue;
    private final double maxValue;
    private final ValueConsumer valueUpdate;
    private final LabelFormatter messageUpdate;
    private final ValueConsumer onSave;

    private float lastValue;
    private boolean changed;
    private boolean dragging;

    public WildfireSlider(int id, int xPos, int yPos, int width, int height, FloatConfigKey config, double currentVal,
            ValueConsumer valueUpdate, LabelFormatter messageUpdate, ValueConsumer onSave) {
        this(id, xPos, yPos, width, height, config.min, config.max, currentVal, valueUpdate, messageUpdate, onSave);
    }

    public WildfireSlider(int id, int xPos, int yPos, int width, int height, double minVal, double maxVal,
            double currentVal, ValueConsumer valueUpdate, LabelFormatter messageUpdate, ValueConsumer onSave) {
        super(id, xPos, yPos, width, height, "");
        this.minValue = minVal;
        this.maxValue = maxVal;
        this.valueUpdate = valueUpdate;
        this.messageUpdate = messageUpdate;
        this.onSave = onSave;
        setValueInternal(currentVal);
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

    private void updateMessage() {
        displayString = messageUpdate.format(lastValue);
    }

    private void applyValue() {
        float newValue = getFloatValue();
        if (lastValue != newValue) {
            valueUpdate.accept(newValue);
            lastValue = newValue;
            changed = true;
        }
    }

    public void save() {
        if (changed) {
            onSave.accept(lastValue);
            changed = false;
        }
    }

    public float getFloatValue() {
        return (float) (this.value * (maxValue - minValue) + minValue);
    }

    private void setValueInternal(double newValue) {
        double normalized = (newValue - this.minValue) / (this.maxValue - this.minValue);
        this.value = normalized < 0 ? 0 : (normalized > 1 ? 1 : normalized);
        this.lastValue = (float) newValue;
        updateMessage();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            setValueFromMouse(mouseX);
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
        save();
    }

    private void setValueFromMouse(int mouseX) {
        double v = (mouseX - (double) (this.xPosition + 4)) / (double) (this.width - 8);
        this.value = v < 0 ? 0 : (v > 1 ? 1 : v);
        applyValue();
        updateMessage();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        if (dragging) {
            setValueFromMouse(mouseX);
        }
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width
                && mouseY < yPosition + height;

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawRect(xPosition + 2, yPosition + 1, xPosition + width - 1, yPosition + height - 1, 0x222222 + (128 << 24));
        int xPos = xPosition + 4 + (int) (this.value * (float) (this.width - 6));
        drawRect(xPosition + 3, yPosition + 2, xPos - 1, yPosition + height - 2, 0x222266 + (180 << 24));
        int xPos2 = xPosition + 2 + (int) (this.value * (float) (this.width - 4));
        drawRect(xPos2 - 2, yPosition + 1, xPos2, yPosition + height - 1, 0xFFFFFF + (120 << 24));
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        FontRenderer font = mc.fontRenderer;
        int color = (field_146123_n || changed) ? 0xFFFF55 : 0xFFFFFF;
        font.drawStringWithShadow(displayString,
                xPosition + width / 2 - font.getStringWidth(displayString) / 2,
                yPosition + (height - 8) / 2, color);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
