package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.Consumer;

public final class UpdateWeatherChunk {
    private final int level;
    private final ChunkPos chunkPos;
    final int[] posData;
    final int[] weatherData;

    final float[] precipitation;
    final float[] temperature;
    final float[] windX;
    final float[] windZ;
    final float[] thunder;

    public UpdateWeatherChunk(int level, ChunkPos chunkPos, int[] posData, int[] weatherData, float[] precipitation, float[] temperature, float[] windX, float[] windZ, float[] thunder) {
        this.level = level;
        this.precipitation = precipitation;
        this.temperature = temperature;
        this.windX = windX;
        this.windZ = windZ;
        this.thunder = thunder;
        if (posData.length != weatherData.length) {
            throw new IllegalArgumentException("posData and weatherData must be the same length");
        }
        this.chunkPos = chunkPos;
        this.weatherData = weatherData;
        this.posData = posData;
    }

    public void encoder(FriendlyByteBuf buffer) {
        for (int i = 0; i < 4; i++) {
            buffer.writeFloat(precipitation[i]);
            buffer.writeFloat(temperature[i]);
            buffer.writeFloat(windX[i]);
            buffer.writeFloat(windZ[i]);
            buffer.writeFloat(thunder[i]);
        }
        buffer.writeVarInt(level);
        buffer.writeLong(chunkPos.toLong());
        buffer.writeInt(posData.length);
        for (int i = 0; i < posData.length; i++) {
            buffer.writeInt(posData[i]);
            buffer.writeInt(weatherData[i]);
        }
    }

    public static UpdateWeatherChunk decoder(FriendlyByteBuf buffer) {
        float[] precipitation = new float[4];
        float[] temperature = new float[4];
        float[] windX = new float[4];
        float[] windZ = new float[4];
        float[] thunder = new float[4];
        for (int i = 0; i < 4; i++) {
            precipitation[i] = buffer.readFloat();
            temperature[i] = buffer.readFloat();
            windX[i] = buffer.readFloat();
            windZ[i] = buffer.readFloat();
            thunder[i] = buffer.readFloat();
        }
        int level = buffer.readVarInt();
        ChunkPos chunkPos = new ChunkPos(buffer.readLong());
        int l = buffer.readInt();
        int[] posData = new int[l];
        int[] weatherData = new int[l];
        for (int i = 0; i < l; i++) {
            posData[i] = buffer.readInt();
            weatherData[i] = buffer.readInt();
        }
        return new UpdateWeatherChunk(level, chunkPos, posData, weatherData, precipitation, temperature, windX, windZ, thunder);
    }

    public interface Sender {
        Sender SENDER = Services.load(Sender.class);

        void send(UpdateWeatherChunk packet, LevelChunk chunk);
    }

    public void apply(Level level, Consumer<BlockPos> posUpdater) {
        var chunk = level.getChunk(chunkPos.x, chunkPos.z);
        var chunkData = Services.PLATFORM.getChunkData(chunk);
        chunkData.update(this, posUpdater);
    }
}
