package dev.lukebemish.tempest.impl;

import dev.lukebemish.tempest.impl.client.ClientNetworking;
import dev.lukebemish.tempest.impl.data.LevelIdMap;
import dev.lukebemish.tempest.impl.data.UpdateWeatherChunk;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        Constants.id("main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private ModNetworking() {}

    private static int id;
    public static void setup(IEventBus modBus) {
        INSTANCE.registerMessage(id++, LevelIdMap.class,
            LevelIdMap::encoder,
            LevelIdMap::decoder,
            (msg, context) -> LevelIdMap.recieve(msg)
        );
        INSTANCE.registerMessage(id++, UpdateWeatherChunk.class,
            UpdateWeatherChunk::encoder,
            UpdateWeatherChunk::decoder,
            (msg, context) -> ClientNetworking.recieveWeatherUpdate(msg)
        );
    }
}
