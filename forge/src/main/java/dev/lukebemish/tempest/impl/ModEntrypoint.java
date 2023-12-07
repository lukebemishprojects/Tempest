package dev.lukebemish.tempest.impl;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public final class ModEntrypoint {
    public ModEntrypoint() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(WeatherDataProvider::registerCapabilities);

        MinecraftForge.EVENT_BUS.addListener(WeatherDataProvider::attachCapabilities);

        ModNetworking.setup(modBus);
    }
}
