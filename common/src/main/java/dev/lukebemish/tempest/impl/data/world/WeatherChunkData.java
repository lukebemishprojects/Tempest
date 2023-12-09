package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.data.WeatherMapData;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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

public class WeatherChunkData {
    final Int2ObjectMap<WeatherData.Concrete> data = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    private final LevelChunk chunk;

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
        for (Int2ObjectMap.Entry<WeatherData.Concrete> entry : entryList) {
            posData[i] = entry.getIntKey();
            weatherData[i] = entry.getValue().data();
            i++;
        }
        return new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData, precipitation, temperature, windSpeed, windDirection);
    }

    public WeatherData query(BlockPos pos) {
        int x = (pos.getX() - chunk.getPos().getMinBlockX()) & 0xFF;
        int y = (pos.getY() - chunk.getMinBuildHeight()) & 0xFFFF;
        int z = (pos.getZ() - chunk.getPos().getMinBlockZ()) & 0xFF;
        int key = (y << 16) | (x << 8) | z;
        var found = data.getOrDefault(key, null);
        if (found == null) {
            return new WeatherData.Reference(this, key);
        }
        return found;
    }

    void decode(int key, BlockPos.MutableBlockPos pos) {
        int x = (key >> 8) & 0xFF;
        int y = (key >> 16) & 0xFFFF;
        int z = key & 0xFF;
        pos.set(chunk.getPos().getMinBlockX() + x, chunk.getMinBuildHeight() + y, chunk.getPos().getMinBlockZ() + z);
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
            var weatherData = new WeatherData.Concrete(this, keys[i]);
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

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int key : new IntArrayList(data.keySet())) {
            decode(key, pos);
            BlockState state = level.getBlockState(pos);
            //noinspection deprecation
            if (!state.blocksMotion()) {
                data.remove(key);
            }
            pos.setY(pos.getY() + 1);
            state = level.getBlockState(pos);
            //noinspection deprecation
            if (state.blocksMotion()) {
                data.remove(key);
            }
        }

        BlockPos waterySurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(x, 0, z, 15)).below();
        float localTemperature = temperature;
        if (!level.getBiome(waterySurface).value().warmEnoughToRain(waterySurface)) {
            localTemperature -= 0.85f;
        }
        if (localTemperature < -0.25f) {
            // add new black ice or ice
            tryFreezeBlock(level, waterySurface);
        }

        if (localTemperature > 0) {
            // melt black ice or ice
            tryMeltBlock(level, waterySurface);
        }

        this.update();
    }

    private void tryFreezeBlock(ServerLevel level, BlockPos toFreeze) {
        if (shouldFreeze(level, toFreeze)) {
            if (isWater(level, toFreeze)) {
                level.setBlockAndUpdate(toFreeze, Blocks.ICE.defaultBlockState());
                var data = query(toFreeze);
                int current = data.blackIce();
                if (current < 15) {
                    data.blackIce(current + 5);
                }
            } else {
                var data = query(toFreeze);
                int current = data.blackIce();
                if (current < 15) {
                    data.blackIce(current + 2);
                }
            }
        }
    }

    private void tryMeltBlock(ServerLevel level, BlockPos toMelt) {
        if (shouldFreeze(level, toMelt)) {
            if (isIce(level, toMelt)) {
                level.setBlockAndUpdate(toMelt, Blocks.WATER.defaultBlockState());
                var data = query(toMelt);
                int current = data.blackIce();
                if (current > 0) {
                    data.blackIce(0);
                }
            } else {
                var data = query(toMelt);
                int current = data.blackIce();
                if (current > 0) {
                    data.blackIce(Math.max(0, current - 2));
                }
            }
        }
    }

    private boolean shouldFreeze(ServerLevel level, BlockPos toFreeze) {
        return toFreeze.getY() >= level.getMinBuildHeight() && toFreeze.getY() < level.getMaxBuildHeight() && level.getBrightness(LightLayer.BLOCK, toFreeze) < 10;
    }

    private boolean isWater(ServerLevel level, BlockPos toFreeze) {
        BlockState blockstate = level.getBlockState(toFreeze);
        FluidState fluidstate = level.getFluidState(toFreeze);
        if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
            if (!level.isLoaded(toFreeze.west()) || !level.isLoaded(toFreeze.east()) || !level.isLoaded(toFreeze.north()) || !level.isLoaded(toFreeze.south())) {
                return false;
            }

            boolean flag = level.isWaterAt(toFreeze.west()) && level.isWaterAt(toFreeze.east()) && level.isWaterAt(toFreeze.north()) && level.isWaterAt(toFreeze.south());
            return !flag;
        }
        return false;
    }

    private boolean isIce(ServerLevel level, BlockPos toFreeze) {
        BlockState blockstate = level.getBlockState(toFreeze);
        return blockstate.getBlock() == Blocks.ICE;
    }

    void update(Level level, UpdateWeatherChunk updateWeatherChunk) {
        this.temperature = updateWeatherChunk.temperature;
        this.precipitation = updateWeatherChunk.precipitation;
        this.windSpeed = updateWeatherChunk.windSpeed;
        this.windDirection = updateWeatherChunk.windDirection;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < updateWeatherChunk.posData.length; i++) {
            int key = updateWeatherChunk.posData[i];
            int value = updateWeatherChunk.weatherData[i];
            decode(key, pos);
            query(pos).data(value);
            level.setBlock(pos, level.getBlockState(pos), 3);
        }
    }
}
