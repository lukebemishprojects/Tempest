package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public final class UpdateWeatherChunk {
    private final int level;
    private final ChunkPos chunkPos;
    private final int[] posData;
    private final int[] weatherData;

    public UpdateWeatherChunk(int level, ChunkPos chunkPos, int[] posData, int[] weatherData) {
        this.level = level;
        if (posData.length != weatherData.length) {
            throw new IllegalArgumentException("posData and weatherData must be the same length");
        }
        this.chunkPos = chunkPos;
        this.weatherData = weatherData;
        this.posData = posData;
    }

    public void encoder(FriendlyByteBuf buffer) {
        buffer.writeVarInt(level);
        buffer.writeLong(chunkPos.toLong());
        buffer.writeInt(posData.length);
        for (int i = 0; i < posData.length; i++) {
            buffer.writeInt(posData[i]);
            buffer.writeInt(weatherData[i]);
        }
    }

    public static UpdateWeatherChunk decoder(FriendlyByteBuf buffer) {
        int level = buffer.readVarInt();
        ChunkPos chunkPos = new ChunkPos(buffer.readLong());
        int l = buffer.readInt();
        int[] posData = new int[l];
        int[] weatherData = new int[l];
        for (int i = 0; i < l; i++) {
            posData[i] = buffer.readInt();
            weatherData[i] = buffer.readInt();
        }
        return new UpdateWeatherChunk(level, chunkPos, posData, weatherData);
    }

    public interface Sender {
        Sender SENDER = Services.load(Sender.class);

        void send(UpdateWeatherChunk packet, LevelChunk chunk);
    }
}
