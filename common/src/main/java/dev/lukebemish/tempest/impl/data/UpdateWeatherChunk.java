package dev.lukebemish.tempest.impl.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;

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
        buffer.writeInt(level);
        buffer.writeLong(chunkPos.toLong());
        buffer.writeInt(posData.length);
        for (int i = 0; i < posData.length; i++) {
            buffer.writeInt(posData[i]);
            buffer.writeInt(weatherData[i]);
        }
    }

    public static UpdateWeatherChunk decoder(FriendlyByteBuf buffer) {
        int level = buffer.readInt();
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
}
