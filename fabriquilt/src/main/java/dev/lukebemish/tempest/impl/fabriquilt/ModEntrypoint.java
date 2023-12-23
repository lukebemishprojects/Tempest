package dev.lukebemish.tempest.impl.fabriquilt;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.data.AttachedWeatherMapReloadListener;
import dev.lukebemish.tempest.impl.data.WeatherSpawnProvider;
import dev.lukebemish.tempest.impl.fabriquilt.client.ClientEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ModEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.bootstrap();

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) ->
            AttachedWeatherMapReloadListener.applyToServer(server)
        );

        ServerLifecycleEvents.SERVER_STARTED.register(AttachedWeatherMapReloadListener::applyToServer);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableWeatherSpawnProviderListener());

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientEntrypoint.init();
        }
    }

    private static final class IdentifiableWeatherSpawnProviderListener extends WeatherSpawnProvider.ReloadListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return Constants.id("spawn_providers");
        }
    }
}
