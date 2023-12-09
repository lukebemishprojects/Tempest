package dev.lukebemish.tempest.impl.client;

import dev.lukebemish.tempest.impl.data.world.UpdateWeatherChunk;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class ClientNetworking {

    public static void recieveWeatherUpdate(UpdateWeatherChunk msg) {
        var level = Minecraft.getInstance().level;
        msg.apply(Objects.requireNonNull(level));
    }

    private ClientNetworking() {}
}
