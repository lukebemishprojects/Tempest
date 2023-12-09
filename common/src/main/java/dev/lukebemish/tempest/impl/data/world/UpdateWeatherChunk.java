package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.Services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class UpdateWeatherChunk {
    private final int level;
    private final ChunkPos chunkPos;
    final int[] posData;
    final int[] weatherData;

    final float precipitation;
    final float temperature;
    final float windSpeed;
    final float windDirection;

    public UpdateWeatherChunk(int level, ChunkPos chunkPos, int[] posData, int[] weatherData, float precipitation, float temperature, float windSpeed, float windDirection) {
        this.level = level;
        this.precipitation = precipitation;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        if (posData.length != weatherData.length) {
            throw new IllegalArgumentException("posData and weatherData must be the same length");
        }
        this.chunkPos = chunkPos;
        this.weatherData = weatherData;
        this.posData = posData;
    }

    public void encoder(FriendlyByteBuf buffer) {
        buffer.writeFloat(precipitation);
        buffer.writeFloat(temperature);
        buffer.writeFloat(windSpeed);
        buffer.writeFloat(windDirection);
        buffer.writeVarInt(level);
        buffer.writeLong(chunkPos.toLong());
        buffer.writeInt(posData.length);
        for (int i = 0; i < posData.length; i++) {
            buffer.writeInt(posData[i]);
            buffer.writeInt(weatherData[i]);
        }
    }

    public static UpdateWeatherChunk decoder(FriendlyByteBuf buffer) {
        float precipitation = buffer.readFloat();
        float temperature = buffer.readFloat();
        float windSpeed = buffer.readFloat();
        float windDirection = buffer.readFloat();
        int level = buffer.readVarInt();
        ChunkPos chunkPos = new ChunkPos(buffer.readLong());
        int l = buffer.readInt();
        int[] posData = new int[l];
        int[] weatherData = new int[l];
        for (int i = 0; i < l; i++) {
            posData[i] = buffer.readInt();
            weatherData[i] = buffer.readInt();
        }
        return new UpdateWeatherChunk(level, chunkPos, posData, weatherData, precipitation, temperature, windSpeed, windDirection);
    }

    public interface Sender {
        Sender SENDER = Services.load(Sender.class);

        void send(UpdateWeatherChunk packet, LevelChunk chunk);
    }

    public void apply(Level level) {
        var chunk = level.getChunk(chunkPos.x, chunkPos.z);
        var chunkData = Services.PLATFORM.getChunkData(chunk);
        chunkData.update(level, this);
    }
}
