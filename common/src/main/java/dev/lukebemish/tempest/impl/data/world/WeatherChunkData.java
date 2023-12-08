package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.data.WeatherMapData;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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

    private float precipitation;
    private float temperature;
    private float windSpeed;
    private float windDirection;

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
            var packet = new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData, precipitation, temperature, windSpeed, windDirection);
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
        return new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData, precipitation, temperature, windSpeed, windDirection);
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

    public void tick(ServerLevel level, WeatherMapData.Built weatherMap) {
        int x = chunk.getPos().getMinBlockX();
        int z = chunk.getPos().getMinBlockZ();

        if (level.random.nextInt(16) == 0) {
            long gameTime = chunk.getLevel().getGameTime();
            temperature = weatherMap.temperature().query(x, z, gameTime);
            precipitation = weatherMap.precipitation().query(x, z, gameTime);
            windSpeed = weatherMap.windSpeed().query(x, z, gameTime);
            windDirection = weatherMap.windDirection().query(x, z, gameTime);
            this.dirty = true;
        }

        if (temperature < -0.5) {
            // add new black ice
            var targetPosAbove = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, level.getBlockRandomPos(x, 0, z, 0xFF));
            var state = level.getBlockState(targetPosAbove);
            if (state.getFluidState().isEmpty()) {
                var targetPos = targetPosAbove.below();
                var data = query(targetPos);
                int current = data.blackIce();
                if (current < 15) {
                    data.blackIce(current + 1);
                }
            }
            tryFreezeBlock(level, x, z);
        }

        this.update();
    }

    private void tryFreezeBlock(ServerLevel level, int x, int z) {
        BlockPos toFreeze = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(x, 0, z, 15)).below();
        if (shouldFreeze(level, toFreeze)) {
            level.setBlockAndUpdate(toFreeze, Blocks.ICE.defaultBlockState());
            var data = query(toFreeze);
            int current = data.blackIce();
            if (current < 15) {
                data.blackIce(current + 3);
            }
        }
    }

    private boolean shouldFreeze(ServerLevel level, BlockPos toFreeze) {
        if (toFreeze.getY() >= level.getMinBuildHeight() && toFreeze.getY() < level.getMaxBuildHeight() && level.getBrightness(LightLayer.BLOCK, toFreeze) < 10) {
            BlockState blockstate = level.getBlockState(toFreeze);
            FluidState fluidstate = level.getFluidState(toFreeze);
            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                if (!level.isLoaded(toFreeze.west()) || level.isLoaded(toFreeze.east()) || level.isLoaded(toFreeze.north()) || level.isLoaded(toFreeze.south())) {
                    return false;
                }

                boolean flag = level.isWaterAt(toFreeze.west()) && level.isWaterAt(toFreeze.east()) && level.isWaterAt(toFreeze.north()) && level.isWaterAt(toFreeze.south());
                return !flag;
            }
        }
        return false;
    }
}
