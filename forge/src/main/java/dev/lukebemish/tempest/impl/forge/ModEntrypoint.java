package dev.lukebemish.tempest.impl.forge;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.AttachedWeatherMapReloadListener;
import dev.lukebemish.tempest.impl.data.WeatherSpawnProvider;
import dev.lukebemish.tempest.impl.forge.client.ClientEntrypoint;
import dev.lukebemish.tempest.impl.forge.compat.embeddium.EmbeddiumCompat;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;

@Mod(Constants.MOD_ID)
public final class ModEntrypoint {

    public ModEntrypoint() {
        Constants.bootstrap();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(WeatherDataProvider::registerCapabilities);

        MinecraftForge.EVENT_BUS.addGenericListener(LevelChunk.class, WeatherDataProvider::attachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(this::onChunkSend);
        MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);

        ModNetworking.setup(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEntrypoint.init(modBus);
        }

        if (ModList.get().isLoaded("embeddium")) {
            EmbeddiumCompat.addCompat(modBus);
        }

        for (var registry : ModPlatform.REGISTRIES.values()) {
            registry.register(modBus);
        }
    }

    private void onChunkSend(ChunkWatchEvent.Watch event) {
        var data = Services.PLATFORM.getChunkData(event.getChunk());
        var packet = data.full();
        if (packet != null) {
            ModNetworking.INSTANCE.send(
                PacketDistributor.PLAYER.with(event::getPlayer),
                packet
            );
        }
    }

    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AttachedWeatherMapReloadListener(event.getRegistryAccess()));
        event.addListener(new WeatherSpawnProvider.ReloadListener());
    }

    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            AttachedWeatherMapReloadListener.applyToServer(event.getPlayerList().getServer());
        }
    }

    private void onServerStarting(ServerStartingEvent event) {
        AttachedWeatherMapReloadListener.applyToServer(event.getServer());
    }
}
