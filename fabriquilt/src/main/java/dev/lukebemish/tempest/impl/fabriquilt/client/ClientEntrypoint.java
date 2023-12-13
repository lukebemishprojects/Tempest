package dev.lukebemish.tempest.impl.fabriquilt.client;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class ClientEntrypoint {
    public static void init() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableOverlaySpriteListener());
    }

    private static final class IdentifiableOverlaySpriteListener extends OverlaySpriteListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return Constants.id("reset_overlay_sprites");
        }
    }
}
