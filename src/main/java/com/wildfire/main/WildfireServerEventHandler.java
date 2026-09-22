package com.wildfire.main;

import com.wildfire.main.networking.PacketGenderInfo;
import com.wildfire.main.networking.WildfireNetwork;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;

import java.util.Map;
import java.util.UUID;

public class WildfireServerEventHandler {

    private static final String PLAYER_HURT = "game.player.hurt";

    /**
     * Brings a joining player up to date with everyone already online.
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        for (Map.Entry<UUID, GenderPlayer> entry : WildfireGender.CLOTHING_PLAYERS.entrySet()) {
            if (entry.getKey().equals(player.getUniqueID())) {
                continue;
            }
            WildfireNetwork.CHANNEL.sendTo(new PacketGenderInfo(entry.getValue()), player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null && !event.player.worldObj.isRemote) {
            WildfireGender.CLOTHING_PLAYERS.remove(event.player.getUniqueID());
        }
    }

    /**
     * Swaps the hurt sound before it goes out over the wire, so every client hears the right one.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundAtEntityEvent event) {
        if (event.entity == null || !PLAYER_HURT.equals(event.name) || !(event.entity instanceof EntityPlayer)) {
            return;
        }
        GenderPlayer plr = WildfireGender.getPlayerById(event.entity.getUniqueID());
        if (plr != null && plr.hasHurtSounds() && plr.getGender().hasFemaleHurtSounds()) {
            event.name = Math.random() > 0.5 ? WildfireSounds.FEMALE_HURT1 : WildfireSounds.FEMALE_HURT2;
        }
    }
}
