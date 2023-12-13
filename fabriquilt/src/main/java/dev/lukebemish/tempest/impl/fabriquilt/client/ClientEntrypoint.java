package dev.lukebemish.tempest.impl.fabriquilt.client;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.client.ClientNetworking;
import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import dev.lukebemish.tempest.impl.data.world.LevelIdMap;
import dev.lukebemish.tempest.impl.fabriquilt.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ClientEntrypoint {
    public static void init() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableOverlaySpriteListener());

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.LEVEL_ID_MAP, (msg, player, responseSender) -> LevelIdMap.recieve(msg.data()));
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UPDATE_WEATHER_CHUNK, (msg, player, responseSender) -> ClientNetworking.recieveWeatherUpdate(msg.data()));
    }

    private static final class IdentifiableOverlaySpriteListener extends OverlaySpriteListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return Constants.id("reset_overlay_sprites");
        }
    }
}
