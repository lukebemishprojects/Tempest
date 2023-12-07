package dev.lukebemish.tempest.impl.data;

import dev.lukebemish.tempest.impl.Constants;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class WeatherChunkData {
    private static final String DATA_NAME = Constants.id("weather_status").toString();

    final Int2ObjectMap<WeatherData> data = new Int2ObjectOpenHashMap<>();

    private final LevelChunk chunk;
    private final IntFunction<WeatherData> factory = key -> new WeatherData(this, key);

    private final Int2IntMap updateQueue = new Int2IntOpenHashMap();

    public WeatherChunkData(LevelChunk chunk) {
        this.chunk = chunk;
    }

    void update(int pos, int data) {
        synchronized (updateQueue) {
            updateQueue.put(pos, data);
        }
    }

    public @Nullable UpdateWeatherChunk update() {
        int[] posData;
        int[] weatherData;
        synchronized (updateQueue) {
            if (updateQueue.isEmpty()) {
                return null;
            }
            posData = new int[updateQueue.size()];
            weatherData = new int[updateQueue.size()];
            int i = 0;
            for (Int2IntMap.Entry entry : updateQueue.int2IntEntrySet()) {
                posData[i] = entry.getIntKey();
                weatherData[i] = entry.getIntValue();
                i++;
            }
            updateQueue.clear();
        }
        return new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData);
    }

    public WeatherData query(BlockPos pos) {
        int x = pos.getX() & 0xFF;
        int y = (pos.getY() - chunk.getMinBuildHeight()) & 0xFFFF;
        int z = pos.getZ() & 0xFF;
        int key = (y << 16) | (x << 8) | z;
        return data.computeIfAbsent(key, factory);
    }

    public @NotNull CompoundTag save(CompoundTag tag) {
        List<Integer> keys = new ArrayList<>();
        ListTag values = new ListTag();
        data.forEach((k, v) -> {
            if (!v.boring()) {
                keys.add(k);
                values.add(v.save());
            }
        });
        tag.putIntArray("positions", keys);
        tag.put("data", values);
        return tag;
    }

    public void load(CompoundTag tag) {
        int[] keys = tag.getIntArray("positions");
        ListTag values = tag.getList("data", 10);
        if (keys.length != values.size()) {
            throw new IllegalStateException("Positions and data are not the same size");
        }
        for (int i = 0; i < keys.length; i++) {
            WeatherData weatherData = new WeatherData(this, keys[i]);
            weatherData.load(values.getCompound(i));
            data.put(keys[i], weatherData);
        }
    }
}
