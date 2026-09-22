package com.wildfire.main;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = WildfireGender.MODID, name = "Female Gender Mod", version = Tags.VERSION, acceptableRemoteVersions = "*")
public class WildfireGender {
    public static final String MODID = "femalegender";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }
}
