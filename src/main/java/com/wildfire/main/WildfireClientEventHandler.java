package com.wildfire.main;

import com.wildfire.gui.screen.WildfirePlayerListScreen;
import com.wildfire.main.networking.PacketGenderInfo;
import com.wildfire.main.networking.WildfireNetwork;
import com.wildfire.render.GenderLayer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class WildfireClientEventHandler {

    private int syncTimer = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) {
            if (!WildfireGender.CLOTHING_PLAYERS.isEmpty()) {
                WildfireGender.CLOTHING_PLAYERS.clear();
            }
            WildfireGender.localPlayerUUID = null;
            return;
        }

        EntityClientPlayerMP self = mc.thePlayer;
        if (self != null) {
            WildfireGender.localPlayerUUID = self.getUniqueID();
        }

        @SuppressWarnings("unchecked")
        List<EntityPlayer> players = world.playerEntities;
        for (int i = 0; i < players.size(); i++) {
            EntityPlayer player = players.get(i);
            UUID uuid = player.getUniqueID();
            if (!WildfireGender.CLOTHING_PLAYERS.containsKey(uuid)) {
                WildfireGender.getOrAddPlayerById(uuid);
                WildfireGender.loadGenderInfoAsync(uuid, self != null && uuid.equals(self.getUniqueID()));
            }
            GenderLayer.updatePhysics(player);
        }

        if (mc.currentScreen == null) {
            while (ClientProxy.openGenderMenu.isPressed()) {
                if (self != null) {
                    mc.displayGuiScreen(new WildfirePlayerListScreen(null, self.getUniqueID()));
                }
            }
        }

        syncTimer++;
        if (syncTimer >= 5) {
            syncTimer = 0;
            sendOwnSettings(self);
        }
    }

    private void sendOwnSettings(EntityClientPlayerMP self) {
        if (self == null) {
            return;
        }
        GenderPlayer plr = WildfireGender.getPlayerById(self.getUniqueID());
        if (plr == null || !plr.needsSync) {
            return;
        }
        plr.needsSync = false;
        try {
            WildfireNetwork.CHANNEL.sendToServer(new PacketGenderInfo(plr));
        } catch (Exception e) {
            // A server without the mod simply will not hear about it; nothing to do
            WildfireGender.LOGGER.debug("Could not send gender info to the server", e);
        }
    }
}
