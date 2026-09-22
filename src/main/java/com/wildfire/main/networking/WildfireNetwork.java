package com.wildfire.main.networking;

import com.wildfire.main.WildfireGender;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class WildfireNetwork {

    public static SimpleNetworkWrapper CHANNEL;

    private WildfireNetwork() {
    }

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(WildfireGender.MODID);
        // client -> server: "here are my settings"
        CHANNEL.registerMessage(PacketGenderInfo.ServerHandler.class, PacketGenderInfo.class, 0, Side.SERVER);
        // server -> client: "here are someone's settings"
        CHANNEL.registerMessage(PacketGenderInfo.ClientHandler.class, PacketGenderInfo.class, 1, Side.CLIENT);
    }
}
