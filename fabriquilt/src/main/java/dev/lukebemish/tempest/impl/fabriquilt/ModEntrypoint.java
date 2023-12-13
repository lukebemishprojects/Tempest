package dev.lukebemish.tempest.impl.fabriquilt;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.data.AttachedWeatherMapReloadListener;
import dev.lukebemish.tempest.impl.fabriquilt.client.ClientEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class ModEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.bootstrap();

        // TODO: attach chunk data

        // TODO: send data on chunk watch

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) ->
            AttachedWeatherMapReloadListener.applyToServer(server)
        );

        ServerLifecycleEvents.SERVER_STARTED.register(AttachedWeatherMapReloadListener::applyToServer);

        ModNetworking.setup();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientEntrypoint.init();
        }
    }
}
