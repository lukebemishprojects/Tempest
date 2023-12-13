package dev.lukebemish.tempest.impl.fabriquilt;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.data.world.LevelIdMap;
import dev.lukebemish.tempest.impl.data.world.UpdateWeatherChunk;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

@AutoService(UpdateWeatherChunk.Sender.class)
public class ModNetworking implements UpdateWeatherChunk.Sender {
    public static final PacketType<LevelIdMapPacket> LEVEL_ID_MAP = PacketType.create(Constants.id("level_id_map"), buf -> new LevelIdMapPacket(LevelIdMap.decoder(buf)));

    public static final PacketType<UpdateWeatherChunkPacket> UPDATE_WEATHER_CHUNK = PacketType.create(Constants.id("update_weather_chunk"), buf -> new UpdateWeatherChunkPacket(UpdateWeatherChunk.decoder(buf)));

    @Override
    public void send(UpdateWeatherChunk data, LevelChunk chunk) {
        var tracking = PlayerLookup.tracking((ServerLevel) chunk.getLevel(), chunk.getPos());
        var packet = new UpdateWeatherChunkPacket(data);
        for (var player : tracking) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public record LevelIdMapPacket(LevelIdMap data) implements FabricPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            data.encoder(buf);
        }

        @Override
        public PacketType<?> getType() {
            return LEVEL_ID_MAP;
        }
    }

    public record UpdateWeatherChunkPacket(UpdateWeatherChunk data) implements FabricPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            data.encoder(buf);
        }

        @Override
        public PacketType<?> getType() {
            return UPDATE_WEATHER_CHUNK;
        }
    }
}
