package dev.lukebemish.tempest.impl.forge;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.client.ClientNetworking;
import dev.lukebemish.tempest.impl.data.world.LevelIdMap;
import dev.lukebemish.tempest.impl.data.world.UpdateWeatherChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@AutoService(UpdateWeatherChunk.Sender.class)
public final class ModNetworking implements UpdateWeatherChunk.Sender {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        Constants.id("main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int id;
    public static void setup(IEventBus modBus) {
        INSTANCE.registerMessage(id++, LevelIdMap.class,
            LevelIdMap::encoder,
            LevelIdMap::decoder,
            (msg, c) -> {
                c.get().enqueueWork(() -> LevelIdMap.recieve(msg));
                c.get().setPacketHandled(true);
            }
        );
        INSTANCE.registerMessage(id++, UpdateWeatherChunk.class,
            UpdateWeatherChunk::encoder,
            UpdateWeatherChunk::decoder,
            (msg, c) -> {
                c.get().enqueueWork(() ->ClientNetworking.recieveWeatherUpdate(msg));
                c.get().setPacketHandled(true);
            }
        );
    }

    @Override
    public void send(UpdateWeatherChunk packet, LevelChunk level) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level), packet);
    }
}
