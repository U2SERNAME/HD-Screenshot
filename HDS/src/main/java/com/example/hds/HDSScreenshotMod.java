package com.example.hds;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("hds")
public class HDSScreenshotMod {
    public static final String MOD_ID = "hds";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SuppressWarnings("removal")
    public HDSScreenshotMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        LOGGER.info("HD Screenshot mod initialized – press ALT+F2 to capture screen and save as ~1.8GB TGA");
    }
}