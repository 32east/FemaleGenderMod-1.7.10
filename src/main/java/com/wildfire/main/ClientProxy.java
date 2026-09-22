package com.wildfire.main;

import com.wildfire.render.GenderLayer;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

public class ClientProxy extends CommonProxy {

    public static KeyBinding openGenderMenu;

    @Override
    public void preInit() {
        super.preInit();
        openGenderMenu = new KeyBinding("key.femalegender.gender_menu", Keyboard.KEY_H, "key.categories.femalegender");
        ClientRegistry.registerKeyBinding(openGenderMenu);
    }

    @Override
    public void init() {
        super.init();
        WildfireClientEventHandler handler = new WildfireClientEventHandler();
        FMLCommonHandler.instance().bus().register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
        MinecraftForge.EVENT_BUS.register(new GenderLayer());

        if (com.wildfire.debug.AutoTest.ENABLED) {
            com.wildfire.debug.AutoTest autoTest = new com.wildfire.debug.AutoTest();
            FMLCommonHandler.instance().bus().register(autoTest);
            MinecraftForge.EVENT_BUS.register(autoTest);
        }
    }
}
