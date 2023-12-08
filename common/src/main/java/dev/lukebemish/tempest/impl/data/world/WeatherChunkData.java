package dev.lukebemish.tempest.impl.data.world;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class WeatherChunkData {
    final Int2ObjectMap<WeatherData> data = new Int2ObjectOpenHashMap<>();

    private final LevelChunk chunk;
    private final IntFunction<WeatherData> factory = key -> new WeatherData(this, key);

    private final Int2IntMap updateQueue = new Int2IntOpenHashMap();
    private boolean dirty = false;

    public WeatherChunkData(LevelChunk chunk) {
        this.chunk = chunk;
    }

    void update(int pos, int data) {
        synchronized (updateQueue) {
            updateQueue.put(pos, data);
            dirty = true;
        }
    }

    public void update() {
        if (this.dirty) {
            int[] posData;
            int[] weatherData;
            synchronized (updateQueue) {
                posData = new int[updateQueue.size()];
                weatherData = new int[updateQueue.size()];
                int i = 0;
                for (Int2IntMap.Entry entry : updateQueue.int2IntEntrySet()) {
                    posData[i] = entry.getIntKey();
                    weatherData[i] = entry.getIntValue();
                    i++;
                }
                updateQueue.clear();
                this.dirty = false;
            }
            var packet = new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData);
            UpdateWeatherChunk.Sender.SENDER.send(packet, chunk);
        }
    }

    public UpdateWeatherChunk full() {
        int[] posData;
        int[] weatherData;
        var entryList = new ArrayList<>(data.int2ObjectEntrySet());
        int size = entryList.size();
        posData = new int[size];
        weatherData = new int[size];
        int i = 0;
        for (Int2ObjectMap.Entry<WeatherData> entry : entryList) {
            posData[i] = entry.getIntKey();
            weatherData[i] = entry.getValue().data();
            i++;
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
