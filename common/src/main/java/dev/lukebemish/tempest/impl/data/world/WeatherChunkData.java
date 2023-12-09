package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import dev.lukebemish.tempest.impl.data.WeatherMapData;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeatherChunkData {
    final Int2ObjectMap<WeatherData.Concrete> data = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    private final LevelChunk chunk;

    private final Int2IntMap updateQueue = new Int2IntOpenHashMap();
    private boolean dirty = false;

    private float[] precipitation = new float[] {-0.5f, -0.5f, -0.5f, -0.5f};
    private float[] temperature = new float[] {0.5f, 0.5f, 0.5f, 0.5f};
    private float[] windSpeed = new float[4];
    private float[] windDirection = new float[4];

    private static final int[] XS = new int[] {0, 0, 16, 16};
    private static final int[] ZS = new int[] {0, 16, 0, 16};

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

    public @Nullable UpdateWeatherChunk full() {
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
        CompoundTag stats = new CompoundTag();
        for (int i = 0; i < 4; i++) {
            stats.putFloat("precipitation" + i, precipitation[i]);
            stats.putFloat("temperature" + i, temperature[i]);
            stats.putFloat("windSpeed" + i, windSpeed[i]);
            stats.putFloat("windDirection" + i, windDirection[i]);
        }
        tag.put("stats", stats);
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
        if (tag.contains("stats", Tag.TAG_COMPOUND)) {
            CompoundTag stats = tag.getCompound("stats");
            for (int i = 0; i < 4; i++) {
                precipitation[i] = stats.getFloat("precipitation" + i);
                temperature[i] = stats.getFloat("temperature" + i);
                windSpeed[i] = stats.getFloat("windSpeed" + i);
                windDirection[i] = stats.getFloat("windDirection" + i);
            }
        }
    }

    public float temperature(BlockPos pos) {
        return relative(pos, temperature);
    }

    public float precipitation(BlockPos pos) {
        return relative(pos, precipitation);
    }

    public float windSpeed(BlockPos pos) {
        return relative(pos, windSpeed);
    }

    public float windDirection(BlockPos pos) {
        return relative(pos, windDirection);
    }

    private static float relative(BlockPos pos, float[] corners) {
        float x = (pos.getX() & 0xF) / 15f;
        float z = (pos.getZ() & 0xF) / 15f;
        float x1 = 1 - x;
        float z1 = 1 - z;
        return corners[0] * x1 * z1 + corners[1] * x1 * z + corners[2] * x * z1 + corners[3] * x * z;
    }

    public void tick(ServerLevel level, WeatherMapData.Built weatherMap) {
        int x = chunk.getPos().getMinBlockX();
        int z = chunk.getPos().getMinBlockZ();

        if (level.random.nextInt(16) == 0) {
            long gameTime = chunk.getLevel().getGameTime();
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 4; i++) {
                mutablePos.setX(x+XS[i]);
                mutablePos.setZ(z+ZS[i]);
                var surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutablePos);
                var biome = chunk.getLevel().getBiome(surface).value();

                temperature[i] = weatherMap.temperature().query(x+XS[i], z+ZS[i], gameTime);

                if (!biome.warmEnoughToRain(surface)) {
                    temperature[i] -= 0.85f;
                } else if (biome.getBaseTemperature() > 1.5f) {
                    temperature[i] += 0.7f;
                }

                precipitation[i] = weatherMap.precipitation().query(x+XS[i], z+ZS[i], gameTime);

                if (!biome.hasPrecipitation()) {
                    precipitation[i] -= 0.6f;
                }

                windSpeed[i] = (weatherMap.windSpeed().query(x+XS[i], z+ZS[i], gameTime) + 1) / 2;
                windDirection[i] = weatherMap.windDirection().query(x+XS[i], z+ZS[i], gameTime);
            }
            this.dirty = true;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int key : new IntArrayList(data.keySet())) {
            var val = this.data.get(key);
            if (val != null) {
                decode(key, pos);
                BlockState state = level.getBlockState(pos);
                if (val.frozenUp() && !state.is(Constants.FREEZES_UP)) {
                    val.frozenUp(false);
                }
                //noinspection deprecation
                if (!state.blocksMotion()) {
                    if (!val.frozenUp() || !state.is(Constants.FREEZES_UP)) {
                        data.remove(key);
                        update(key, 0);
                    }
                }
                if (meltsAt(level, pos)) {
                    int blackIce = val.blackIce();
                    blackIce = Math.max(0, blackIce - 2);
                    val.levelBlackIce(level, pos, blackIce);
                }
                boolean belowSturdyUp = state.isFaceSturdy(level, pos, Direction.UP);
                pos.setY(pos.getY() + 1);
                state = level.getBlockState(pos);
                boolean aboveSturdyDown = state.isFaceSturdy(level, pos, Direction.DOWN);
                if (belowSturdyUp && aboveSturdyDown) {
                    data.remove(key);
                    update(key, 0);
                }
            }
        }

        if (meltAndFreeze(level, x, z)) {
            boolean tryAgain = meltAndFreeze(level, x, z);
            if (tryAgain && level.random.nextBoolean()) {
                meltAndFreeze(level, x, z);
            }
        }

        this.update();
    }

    private boolean meltAndFreeze(ServerLevel level, int x, int z) {
        BlockPos waterySurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level.getBlockRandomPos(x, 0, z, 15)).below();
        float temp = temperature(waterySurface);
        if (temp < 0f) {
            // add new black ice or ice
            return tryFreezeBlock(level, waterySurface);
        }

        if (temp > 0) {
            // melt black ice or ice
            tryMeltBlock(level, waterySurface);
            return temp > 0.5f;
        }
        return false;
    }

    private boolean tryFreezeBlock(ServerLevel level, BlockPos toFreeze) {
        float precip = precipitation(toFreeze);
        float temp = temperature(toFreeze);
        if (shouldFreeze(level, toFreeze)) {
            var freezeState = level.getBlockState(toFreeze);
            if (freezeState.getBlock() instanceof LiquidBlock) {
                if (isFreezableWater(level, toFreeze)) {
                    level.setBlockAndUpdate(toFreeze, Blocks.ICE.defaultBlockState());
                    var data = query(toFreeze);
                    int current = data.blackIce();
                    if (current < 15) {
                        data.levelBlackIce(level, toFreeze, current + 3);
                    }
                }
                return temp < -0.5f;
            } else {
                if (isSleeting(temp, precip)) {
                    var data = query(toFreeze);
                    int current = data.blackIce();
                    if (current < 15) {
                        data.levelBlackIce(level, toFreeze, current + 2);
                        var abovePos = toFreeze.above();
                        BlockState aboveState;
                        while ((aboveState = level.getBlockState(abovePos)).is(Constants.FREEZES_UP)) {
                            if (aboveState.is(Constants.FREEZES_UP)) {
                                var aboveData = query(abovePos);
                                int aboveCurrent = aboveData.blackIce();
                                if (aboveCurrent < 15) {
                                    aboveData.levelBlackIce(level, abovePos, aboveCurrent + 2);
                                }
                            }
                            abovePos = abovePos.above();
                        }
                    }
                    return precip > 0.5f;
                } else if (isSnowing(temp, precip)) {
                    BlockPos toSnow = toFreeze.above();
                    var state = level.getBlockState(toSnow);
                    if (state.getBlock() == Blocks.SNOW) {
                        int levels = state.getValue(SnowLayerBlock.LAYERS);
                        BlockState newState;
                        if (levels < 7) {
                            newState = state.setValue(SnowLayerBlock.LAYERS, levels + 1);
                        } else {
                            if (level.random.nextFloat() < 0.75f) {
                                newState = Blocks.SNOW_BLOCK.defaultBlockState();
                            } else {
                                newState = Blocks.POWDER_SNOW.defaultBlockState();
                            }
                        }
                        level.setBlockAndUpdate(toSnow, newState);
                        Block.pushEntitiesUp(state, newState, level, toSnow);
                    } else if (state.canBeReplaced() && Blocks.SNOW.defaultBlockState().canSurvive(level, toSnow)) {
                        level.setBlockAndUpdate(toSnow, Blocks.SNOW.defaultBlockState());
                    }
                    return precip > 0.5f;
                }
            }
        }
        return precip > 0.29f && temp < -0.29;
    }

    private static boolean isSnowing(float temp, float precip) {
        return precip > 0f && ((temp < -0.6) || (temp < -0.1 && precip > 0.7f));
    }

    private static boolean isSleeting(float temp, float precip) {
        return precip > 0f && (temp < 0 && !isSnowing(temp, precip));
    }

    private static boolean isRaining(float temp, float precip) {
        return precip > 0f && !isSleeting(temp, precip) && !isSnowing(temp, precip);
    }

    private void tryMeltBlock(ServerLevel level, BlockPos toMelt) {
        if (shouldMelt(level, toMelt)) {
            var state = level.getBlockState(toMelt);
            if (state.getBlock() == Blocks.ICE) {
                level.setBlockAndUpdate(toMelt, Blocks.WATER.defaultBlockState());
                var data = query(toMelt);
                int current = data.blackIce();
                if (current > 0) {
                    data.levelBlackIce(level, toMelt, 0);
                }
            } else {
                var data = query(toMelt);
                int current = data.blackIce();
                if (current > 0) {
                    data.levelBlackIce(level, toMelt, Math.max(0, current - 2));
                    var abovePos = toMelt.above();
                    BlockState aboveState;
                    while ((aboveState = level.getBlockState(abovePos)).is(Constants.FREEZES_UP)) {
                        if (aboveState.is(Constants.FREEZES_UP)) {
                            var aboveData = query(abovePos);
                            int aboveCurrent = aboveData.blackIce();
                            if (aboveCurrent > 0) {
                                aboveData.levelBlackIce(level, abovePos, Math.max(0, aboveCurrent - 2));
                            }
                        }
                        abovePos = abovePos.above();
                    }
                }
            }

            var above = toMelt.above();
            var stateAbove = level.getBlockState(above);
            if (stateAbove.getBlock() == Blocks.SNOW) {
                int levels = stateAbove.getValue(SnowLayerBlock.LAYERS);
                if (levels > 1) {
                    var newState = stateAbove.setValue(SnowLayerBlock.LAYERS, levels - 1);
                    level.setBlockAndUpdate(above, newState);
                    Block.pushEntitiesUp(stateAbove, newState, level, above);
                } else {
                    level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                }
            } else if (stateAbove.getBlock() == Blocks.AIR && (state.getBlock() == Blocks.SNOW_BLOCK || state.getBlock() == Blocks.POWDER_SNOW)) {
                level.setBlockAndUpdate(above, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7));
            }
        }
    }

    private boolean meltsAt(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        mutable.set(pos);
        for (int i = -1; i <= 1; i++) {
            mutable.setX(pos.getX() + i);
            if (level.getBrightness(LightLayer.BLOCK, mutable) > 11) return true;
        }
        mutable.setX(pos.getX());
        for (int i = -1; i <= 1; i++) {
            mutable.setY(pos.getY() + i);
            if (level.getBrightness(LightLayer.BLOCK, mutable) > 11) return true;
        }
        mutable.setY(pos.getY());
        for (int i = -1; i <= 1; i++) {
            mutable.setZ(pos.getZ() + i);
            if (level.getBrightness(LightLayer.BLOCK, mutable) > 11) return true;
        }
        return level.getBrightness(LightLayer.BLOCK, pos) > 11;
    }

    private boolean shouldFreeze(ServerLevel level, BlockPos toFreeze) {
        return toFreeze.getY() >= level.getMinBuildHeight() && toFreeze.getY() < level.getMaxBuildHeight() && level.getBrightness(LightLayer.BLOCK, toFreeze) < 10;
    }

    private boolean shouldMelt(ServerLevel level, BlockPos toFreeze) {
        return toFreeze.getY() >= level.getMinBuildHeight() && toFreeze.getY() < level.getMaxBuildHeight();
    }

    private boolean isFreezableWater(ServerLevel level, BlockPos toFreeze) {
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

    public @Nullable WeatherCategory.WeatherStatus getWeatherStatus(BlockPos pos) {
        float precip = precipitation(pos);
        if (precipitation(pos) > 0f) {
            WeatherCategory category;
            float temp = temperature(pos);
            if (isSnowing(temp, precip)) {
                category = WeatherCategory.SNOW;
            } else if (isSleeting(temp, precip)) {
                category = WeatherCategory.SLEET;
            } else {
                category = WeatherCategory.RAIN;
            }
            return new WeatherCategory.WeatherStatus(category, precipitation(pos), windSpeed(pos), windDirection(pos));
        }
        return null;
    }
}
