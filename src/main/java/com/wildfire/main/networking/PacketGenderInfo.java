package com.wildfire.main.networking;

import com.wildfire.main.Breasts;
import com.wildfire.main.Gender;
import com.wildfire.main.GenderPlayer;
import com.wildfire.main.WildfireGender;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

/**
 * One player's settings. Sent client to server when they change, and server to client to share them around.
 */
public class PacketGenderInfo implements IMessage {

    private UUID uuid;
    private Gender gender;
    private float bustSize;
    private boolean hurtSounds;
    private boolean breastPhysics;
    private boolean armorBreastPhysics;
    private boolean showInArmor;
    private float bounceMultiplier;
    private float floppyMultiplier;
    private float xOffset;
    private float yOffset;
    private float zOffset;
    private boolean uniboob;
    private float cleavage;

    public PacketGenderInfo() {
    }

    public PacketGenderInfo(GenderPlayer plr) {
        this.uuid = plr.uuid;
        this.gender = plr.getGender();
        this.bustSize = plr.getBustSize();
        this.hurtSounds = plr.hasHurtSounds();
        this.breastPhysics = plr.hasBreastPhysics();
        this.armorBreastPhysics = plr.hasArmorBreastPhysics();
        this.showInArmor = plr.showBreastsInArmor();
        this.bounceMultiplier = plr.getBounceMultiplierRaw();
        this.floppyMultiplier = plr.getFloppiness();
        Breasts b = plr.getBreasts();
        this.xOffset = b.getXOffset();
        this.yOffset = b.getYOffset();
        this.zOffset = b.getZOffset();
        this.uniboob = b.isUniboob();
        this.cleavage = b.getCleavage();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        uuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        gender = Gender.byIndex(buf.readByte());
        bustSize = buf.readFloat();
        hurtSounds = buf.readBoolean();
        breastPhysics = buf.readBoolean();
        armorBreastPhysics = buf.readBoolean();
        showInArmor = buf.readBoolean();
        bounceMultiplier = buf.readFloat();
        floppyMultiplier = buf.readFloat();
        xOffset = buf.readFloat();
        yOffset = buf.readFloat();
        zOffset = buf.readFloat();
        uniboob = buf.readBoolean();
        cleavage = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, uuid.toString());
        buf.writeByte(gender.ordinal());
        buf.writeFloat(bustSize);
        buf.writeBoolean(hurtSounds);
        buf.writeBoolean(breastPhysics);
        buf.writeBoolean(armorBreastPhysics);
        buf.writeBoolean(showInArmor);
        buf.writeFloat(bounceMultiplier);
        buf.writeFloat(floppyMultiplier);
        buf.writeFloat(xOffset);
        buf.writeFloat(yOffset);
        buf.writeFloat(zOffset);
        buf.writeBoolean(uniboob);
        buf.writeFloat(cleavage);
    }

    private void applyTo(GenderPlayer plr) {
        plr.updateGender(gender);
        plr.updateBustSize(bustSize);
        plr.updateHurtSounds(hurtSounds);
        plr.updateBreastPhysics(breastPhysics);
        plr.updateArmorBreastPhysics(armorBreastPhysics);
        plr.updateShowBreastsInArmor(showInArmor);
        plr.updateBounceMultiplier(bounceMultiplier);
        plr.updateFloppiness(floppyMultiplier);
        Breasts b = plr.getBreasts();
        b.updateXOffset(xOffset);
        b.updateYOffset(yOffset);
        b.updateZOffset(zOffset);
        b.updateUniboob(uniboob);
        b.updateCleavage(cleavage);
        plr.syncStatus = GenderPlayer.SyncStatus.SYNCED;
    }

    public static class ServerHandler implements IMessageHandler<PacketGenderInfo, IMessage> {

        @Override
        public IMessage onMessage(PacketGenderInfo message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            // Only ever let a client speak for itself
            if (sender == null || !sender.getUniqueID().equals(message.uuid)) {
                return null;
            }
            message.applyTo(WildfireGender.getOrAddPlayerById(message.uuid));
            // Relay to everyone, including the sender, so all clients agree on what to draw
            WildfireNetwork.CHANNEL.sendToAll(message);
            return null;
        }
    }

    public static class ClientHandler implements IMessageHandler<PacketGenderInfo, IMessage> {

        @Override
        public IMessage onMessage(PacketGenderInfo message, MessageContext ctx) {
            // Our own settings are authoritative locally; an echo of them would fight the settings screen
            if (message.uuid.equals(WildfireGender.localPlayerUUID)) {
                return null;
            }
            message.applyTo(WildfireGender.getOrAddPlayerById(message.uuid));
            return null;
        }
    }
}
