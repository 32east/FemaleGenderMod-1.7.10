package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireButton;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import com.wildfire.render.SkinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Lists the players in the world so their wardrobe can be opened.
 *
 * <p>Your own settings are saved and synced to the server; everyone else's are a local override of
 * how you see them, which is what upstream does too.</p>
 */
public class WildfirePlayerListScreen extends BaseWildfireScreen {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(WildfireGender.MODID, "textures/gui/player_list.png");

    private static final int ROWS = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int ID_EXIT = 0;
    private static final int ID_PREV = 1;
    private static final int ID_NEXT = 2;
    private static final int ID_FIRST_ROW = 10;

    private final List<EntityPlayer> players = new ArrayList<EntityPlayer>();
    private int page = 0;

    public WildfirePlayerListScreen(GuiScreen parent, UUID self) {
        super(StatCollector.translateToLocal("femalegender.player_list.title"), parent, self);
    }

    @SuppressWarnings("unchecked")
    private void collectPlayers() {
        players.clear();
        if (mc.theWorld == null) {
            return;
        }
        players.addAll((List<EntityPlayer>) mc.theWorld.playerEntities);
        // Local player first, then alphabetical, so the list does not shuffle between frames
        final UUID self = playerUUID;
        Collections.sort(players, new Comparator<EntityPlayer>() {
            @Override
            public int compare(EntityPlayer a, EntityPlayer b) {
                boolean aSelf = a.getUniqueID().equals(self);
                boolean bSelf = b.getUniqueID().equals(self);
                if (aSelf != bSelf) {
                    return aSelf ? -1 : 1;
                }
                return a.getCommandSenderName().compareToIgnoreCase(b.getCommandSenderName());
            }
        });
    }

    private int listX() {
        return this.width / 2 - 59;
    }

    private int listTop() {
        return this.height / 2 - 81;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        collectPlayers();
        if (page >= pageCount()) {
            page = 0;
        }

        int y = this.height / 2 - 20;
        buttonList.add(new WildfireButton(ID_EXIT, this.width / 2 + 53, y - 74, 9, 9, "X"));

        int rowX = listX();
        int top = listTop();
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            if (index >= players.size()) {
                break;
            }
            EntityPlayer player = players.get(index);
            GenderPlayer plr = WildfireGender.getOrAddPlayerById(player.getUniqueID());
            String label = player.getCommandSenderName() + EnumChatFormatting.GRAY + " - "
                    + plr.getGender().getDisplayName();
            buttonList.add(new WildfireButton(ID_FIRST_ROW + row, rowX + 21, top + row * ROW_HEIGHT, 97, 19, label));
        }

        if (pageCount() > 1) {
            int navY = top + ROWS * ROW_HEIGHT + 2;
            buttonList.add(new WildfireButton(ID_PREV, rowX, navY, 20, 16, "<"));
            buttonList.add(new WildfireButton(ID_NEXT, rowX + 98, navY, 20, 16, ">"));
        }
    }

    private int pageCount() {
        return Math.max(1, (players.size() + ROWS - 1) / ROWS);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_EXIT) {
            mc.displayGuiScreen(parent);
        } else if (button.id == ID_PREV) {
            page = (page - 1 + pageCount()) % pageCount();
            initGui();
        } else if (button.id == ID_NEXT) {
            page = (page + 1) % pageCount();
            initGui();
        } else if (button.id >= ID_FIRST_ROW) {
            int index = page * ROWS + (button.id - ID_FIRST_ROW);
            if (index < players.size()) {
                mc.displayGuiScreen(new WardrobeBrowserScreen(this, players.get(index).getUniqueID()));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Players can join or leave while the screen is open
        if (players.size() != (mc.theWorld == null ? 0 : mc.theWorld.playerEntities.size())) {
            initGui();
        }

        drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        int i = (this.width - 132) / 2;
        int j = (this.height - 156) / 2 - 20;
        drawTexturedModalRect(i, j, 0, 0, 192, 174);

        int y = this.height / 2 - 20;
        fontRendererObj.drawString(getScreenTitle(), this.width / 2 - 60, y - 73, 4473924);

        super.drawScreen(mouseX, mouseY, partialTicks);

        // Faces go on last so they sit above the row backgrounds
        int rowX = listX();
        int top = listTop();
        for (int row = 0; row < ROWS; row++) {
            int index = page * ROWS + row;
            if (index >= players.size()) {
                break;
            }
            drawFace(players.get(index), rowX, top + row * ROW_HEIGHT);
        }

        if (pageCount() > 1) {
            drawCenteredString(fontRendererObj, (page + 1) + " / " + pageCount(), this.width / 2,
                    top + ROWS * ROW_HEIGHT + 6, 0xFFFFFF);
        }
        drawButtonTooltips(mouseX, mouseY);
    }

    private void drawFace(EntityPlayer player, int x, int y) {
        if (!(player instanceof AbstractClientPlayer)) {
            return;
        }
        ResourceLocation skin = ((AbstractClientPlayer) player).getLocationSkin();
        int skinHeight = SkinUtils.getSkinHeight(skin);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        func_152125_a(x, y, 8F, 8F, 8, 8, 19, 19, 64F, skinHeight);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        func_152125_a(x, y, 40F, 8F, 8, 8, 19, 19, 64F, skinHeight);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
