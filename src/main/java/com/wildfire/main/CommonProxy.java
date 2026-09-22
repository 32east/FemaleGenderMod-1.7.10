package com.wildfire.main;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

    public void preInit() {
    }

    public void init() {
        WildfireServerEventHandler handler = new WildfireServerEventHandler();
        FMLCommonHandler.instance().bus().register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }
}
