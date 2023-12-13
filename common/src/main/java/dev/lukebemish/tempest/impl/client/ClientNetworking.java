package dev.lukebemish.tempest.impl.client;

import dev.lukebemish.tempest.impl.data.world.UpdateWeatherChunk;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class ClientNetworking {

    public static void recieveWeatherUpdate(UpdateWeatherChunk msg) {
        var level = Minecraft.getInstance().level;
        msg.apply(Objects.requireNonNull(level), pos -> {
            Minecraft.getInstance().levelRenderer.setBlocksDirty(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ()
            );
        });
    }

    private ClientNetworking() {}
}
