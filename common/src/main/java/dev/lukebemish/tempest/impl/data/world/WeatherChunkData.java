package dev.lukebemish.tempest.impl.data.world;

import dev.lukebemish.tempest.api.WeatherStatus;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import dev.lukebemish.tempest.impl.data.WeatherMapData;
import dev.lukebemish.tempest.impl.util.QuasiRandomChunkVisitor;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WeatherChunkData {
    final Int2ObjectMap<WeatherData.Concrete> data = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    private final LevelChunk chunk;

    private final Int2IntMap updateQueue = new Int2IntOpenHashMap();
    private boolean networkingDirty = false;

    private float[] precipitation = new float[] {-0.5f, -0.5f, -0.5f, -0.5f};
    private float[] temperature = new float[] {0.5f, 0.5f, 0.5f, 0.5f};
    private float[] windX = new float[4];
    private float[] windZ = new float[4];
    private float[] thunder = new float[4];

    private static final int[] XS = new int[] {0, 0, 16, 16};
    private static final int[] ZS = new int[] {0, 16, 0, 16};

    private boolean initialized;
    private int visitIndex = -1;
    private final List<BlockPos> windCheckPositions = new ArrayList<>();

    private final @Nullable Runnable setDirtyCallback;

    public WeatherChunkData(LevelChunk chunk, @Nullable Runnable setDirtyCallback) {
        this.chunk = chunk;
        this.setDirtyCallback = setDirtyCallback;
    }

    public WeatherChunkData(LevelChunk chunk) {
        this(chunk, null);
    }

    protected void update(int pos, int data) {
        synchronized (updateQueue) {
            updateQueue.put(pos, data);
            markDirty();
        }
    }

    private void markDirty() {
        networkingDirty = true;
        if (setDirtyCallback != null) {
            setDirtyCallback.run();
        }
    }

    public List<BlockPos> icedInSection(SectionPos pos) {
        List<BlockPos> iced = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (var key : data.keySet()) {
            decode(key, mutable);
            var data = query(mutable);
            if (data.blackIce() >= 1 && mutable.getY() >= pos.minBlockY() && mutable.getY() <= pos.maxBlockY()) {
                iced.add(mutable.immutable());
            }
        }
        return iced;
    }

    public void update() {
        if (this.networkingDirty) {
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
                this.networkingDirty = false;
            }
            var packet = new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData, precipitation, temperature, windX, windZ, thunder);
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
        return new UpdateWeatherChunk(LevelIdMap.CURRENT.id(chunk.getLevel().dimension()), chunk.getPos(), posData, weatherData, precipitation, temperature, windX, windZ, thunder);
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
            stats.putFloat("windX" + i, windX[i]);
            stats.putFloat("windZ" + i, windZ[i]);
            stats.putFloat("thunder" + i, thunder[i]);
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
                windX[i] = stats.getFloat("windX" + i);
                windZ[i] = stats.getFloat("windZ" + i);
                thunder[i] = stats.getFloat("thunder" + i);
            }
        }
    }

    public float temperature(BlockPos pos) {
        return relative(pos, temperature);
    }

    public float precipitation(BlockPos pos) {
        return relative(pos, precipitation);
    }

    public float windX(BlockPos pos) {
        return relative(pos, windX);
    }

    public float windZ(BlockPos pos) {
        return relative(pos, windZ);
    }

    public float thunder(BlockPos pos) {
        return relative(pos, thunder);
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

        if (!initialized || level.random.nextInt(8) == 0) {
            initialized = true;
            long gameTime = chunk.getLevel().getGameTime();
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 4; i++) {
                mutablePos.setX(x+XS[i]);
                mutablePos.setZ(z+ZS[i]);
                var surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutablePos);
                var biome = chunk.getLevel().getBiome(surface).value();

                float temp = weatherMap.temperature().query(x+XS[i], z+ZS[i], gameTime);

                if (!biome.warmEnoughToRain(surface)) {
                    temp -= 0.95f;
                } else if (biome.getBaseTemperature() > 1.5f) {
                    temp += 0.7f;
                }

                temperature[i] = Mth.clamp(temp, -1, 1);

                float precip = weatherMap.precipitation().query(x+XS[i], z+ZS[i], gameTime);

                if (!biome.hasPrecipitation()) {
                    precipitation[i] -= 0.6f;
                }

                precipitation[i] = Mth.clamp(precip, -1, 1);

                windX[i] = weatherMap.windX().query(x+XS[i], z+ZS[i], gameTime);
                windZ[i] = weatherMap.windZ().query(x+XS[i], z+ZS[i], gameTime);
                thunder[i] = Mth.clamp(weatherMap.thunder().query(x+XS[i], z+ZS[i], gameTime), -1, 1);
            }
            recalculateWindCheckPositions(level);
            this.markDirty();
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

        if (meltAndFreeze(level)) {
            boolean tryAgain = meltAndFreeze(level);
            if (tryAgain && level.random.nextBoolean()) {
                meltAndFreeze(level);
            }
        }

        this.update();
    }

    private void recalculateWindCheckPositions(Level level) {
        var centerPos = chunk.getPos().getBlockAt(8, 0, 8);
        float windX = windX(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerPos));
        float windZ = windZ(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerPos));
        float speed = Mth.sqrt(windX * windX + windZ * windZ);
        float angle = Mth.clamp(speed, 0, 1.25F)/ 1.25F;
        float singleX = -(float) Math.cos(angle) * windX / speed;
        float singleY = (float) Math.sin(angle);
        float singleZ = -(float) Math.cos(angle) * windZ / speed;
        windCheckPositions.clear();
        float xOff = 0;
        float yOff = 0;
        float zOff = 0;
        for (int i = 0; i < 12; i++) {
            xOff += singleX;
            yOff += singleY;
            zOff += singleZ;
            windCheckPositions.add(new BlockPos(Math.round(xOff), Math.round(yOff), Math.round(zOff)));
        }
    }

    private boolean meltAndFreeze(ServerLevel level) {
        BlockPos waterySurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QuasiRandomChunkVisitor.INSTANCE.inChunk(chunk, visitIndex, i -> this.visitIndex = i)).below();
        float temp = temperature(waterySurface);
        float precip = precipitation(waterySurface);
        float thunder = thunder(waterySurface);

        boolean repeat = false;

        if (isHailing(temp, precip, thunder)) {
            if (level.random.nextFloat() < 0.4 * precip) {
                BlockPos hailSurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, waterySurface).below();
                if (canSeeWind(hailSurface)) {
                    if (tryHailBreak(level, hailSurface.above())) {
                        tryHailBreak(level, hailSurface);
                    }
                }
            }
            repeat = level.random.nextFloat() < precip;
        }

        if (temp < 0f) {
            BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            above.set(waterySurface);
            above.setY(above.getY() + 1);
            if (!canSeeWindIgnoreLeavesOrSnow(above)) {
                above.setY(above.getY() + 1);
                if (level.random.nextBoolean() || !canSeeWindIgnoreLeavesOrSnow(above)) {
                    if (level.random.nextBoolean() || !canSeeWindIgnoreLeavesOrSnow(above)) {
                        return false;
                    }
                }
            }
            // add new black ice or ice
            boolean frozen = tryFreezeBlock(level, waterySurface);
            repeat = frozen || (repeat && level.random.nextBoolean());
        }

        if (temp > 0) {
            // melt black ice or ice
            tryMeltBlock(level, waterySurface);
            boolean melted = level.random.nextFloat() < temp;
            repeat = repeat || (melted && level.random.nextBoolean());
        }

        return repeat;
    }

    private boolean tryFreezeBlock(ServerLevel level, BlockPos toFreeze) {
        float precip = this.precipitation(toFreeze);
        float temp = this.temperature(toFreeze);
        float thunder = this.thunder(toFreeze);
        if (validBlock(level, toFreeze)) {
            if (shouldFreeze(level, toFreeze)) {
                var freezeState = level.getBlockState(toFreeze);
                if (freezeState.getBlock() instanceof LiquidBlock) {
                    if (isFreezableWater(level, toFreeze)) {
                        level.setBlockAndUpdate(toFreeze, Blocks.ICE.defaultBlockState());
                        var data = this.query(toFreeze);
                        int current = data.blackIce();
                        if (current < 15) {
                            data.levelBlackIce(level, toFreeze, current + 3);
                        }
                    }
                    return level.random.nextFloat() < -temp;
                } else {
                    if (isSleeting(temp, precip, thunder)) {
                        var data = this.query(toFreeze);
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
                        return level.random.nextFloat() < precip;
                    } else if (isSnowing(temp, precip, thunder)) {
                        BlockPos toSnow = toFreeze.above();
                        var state = level.getBlockState(toSnow);
                        if (state.getBlock() == Blocks.SNOW) {
                            int levels = state.getValue(SnowLayerBlock.LAYERS);
                            BlockState newState;
                            if (levels < 6) {
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
                            if (hasSpaceForSnow(level, toSnow)) {
                                level.setBlockAndUpdate(toSnow, Blocks.SNOW.defaultBlockState());
                            }
                        } else if (state.getBlock() == Blocks.POWDER_SNOW) {
                            BlockPos aboveSnow = toSnow.above();
                            BlockState upState = level.getBlockState(aboveSnow);
                            level.setBlockAndUpdate(toSnow, Blocks.SNOW_BLOCK.defaultBlockState());
                            if (upState.canBeReplaced() && Blocks.SNOW.defaultBlockState().canSurvive(level, aboveSnow)) {
                                if (hasSpaceForSnow(level, aboveSnow)) {
                                    level.setBlockAndUpdate(aboveSnow, Blocks.SNOW.defaultBlockState());
                                }
                            }
                        }
                        return level.random.nextFloat() < precip;
                    }
                }
            }
        }
        return level.random.nextFloat() < (level.random.nextBoolean() ? -temp : precip);
    }

    public boolean canSeeWindIgnoreLeaves(BlockPos pos) {
        var mutablePos = new BlockPos.MutableBlockPos();
        for (BlockPos check : windCheckPositions) {
            mutablePos.setWithOffset(pos, check);
            if (chunk.getLevel().isLoaded(mutablePos)) {
                BlockState blockState = chunk.getLevel().getBlockState(mutablePos);
                if (isMotionBlocking(blockState) && !(blockState.getBlock() instanceof LeavesBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canSeeWindIgnoreLeavesOrSnow(BlockPos pos) {
        var mutablePos = new BlockPos.MutableBlockPos();
        for (BlockPos check : windCheckPositions) {
            mutablePos.setWithOffset(pos, check);
            if (chunk.getLevel().isLoaded(mutablePos)) {
                BlockState blockState = chunk.getLevel().getBlockState(mutablePos);
                if (isMotionBlocking(blockState) && !(blockState.getBlock() instanceof LeavesBlock) && !(blockState.is(Constants.SNOW_PASSTHROUGH))) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canSeeWind(BlockPos pos) {
        var mutablePos = new BlockPos.MutableBlockPos();
        for (BlockPos check : windCheckPositions) {
            mutablePos.setWithOffset(pos, check);
            if (chunk.getLevel().isLoaded(mutablePos)) {
                BlockState blockState = chunk.getLevel().getBlockState(mutablePos);
                if (isMotionBlocking(blockState)) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static boolean isMotionBlocking(BlockState blockState) {
        return (blockState.blocksMotion() || !blockState.getFluidState().isEmpty()) && !(blockState.getBlock() == Blocks.SNOW);
    }

    private static boolean hasSpaceForSnow(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            pos = pos.below();
            if (!validBlock(level, pos)) {
                return true;
            }
            var block = level.getBlockState(pos).getBlock();
            if (block != Blocks.SNOW_BLOCK && block != Blocks.POWDER_SNOW) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryHailBreak(ServerLevel level, BlockPos toFreeze) {
        var hailEffectState = level.getBlockState(toFreeze);
        if (hailEffectState.is(Constants.BREAKS_WITH_HAIL) && !hailEffectState.is(Constants.SAFE_WITH_HAIL)) {
            level.destroyBlock(toFreeze, true);
            return false;
        }
        return true;
    }

    private static boolean isHailing(float temp, float precip, float thunder) {
        return temp < 0.5f && ((precip > 0.75f && temp > -0.5f) || thunder > 0.9f);
    }

    private static boolean isSnowing(float temp, float precip, float thunder) {
        return precip > 0f && !isHailing(temp, precip, thunder) && ((temp < -0.6) || (temp < -0.1 && precip > 0.7f));
    }

    private static boolean isSleeting(float temp, float precip, float thunder) {
        return precip > 0f && temp < 0 && !isSnowing(temp, precip, thunder) && !isHailing(temp, precip, thunder);
    }

    private static boolean isRaining(float temp, float precip, float thunder) {
        return precip > 0f && !isSleeting(temp, precip, thunder) && !isSnowing(temp, precip, thunder) && !isHailing(temp, precip, thunder);
    }

    public interface WeatherStatusAssembler {
        WeatherStatus assemble(WeatherStatus.Kind kind, float intensity, float temperature, boolean thunder, Vec2 wind);
    }

    public WeatherStatus makeApiStatus(WeatherStatusAssembler assembler, BlockPos pos) {
        float temp = temperature(pos);
        float precip = precipitation(pos);
        float thunder = thunder(pos);
        float windX = windX(pos);
        float windZ = windZ(pos);

        WeatherStatus.Kind kind;
        if (isSnowing(temp, precip, thunder)) {
            kind = WeatherStatus.Kind.SNOW;
        } else if (isSleeting(temp, precip, thunder)) {
            kind = WeatherStatus.Kind.SLEET;
        } else if (isHailing(temp, precip, thunder)) {
            kind = WeatherStatus.Kind.HAIL;
        } else if (isRaining(temp, precip, thunder)) {
            kind = WeatherStatus.Kind.RAIN;
        } else {
            kind = WeatherStatus.Kind.CLEAR;
        }

        return assembler.assemble(kind, Mth.sqrt(Mth.clamp(precip, 0, 1)), temp, thunder > 0f, new Vec2(windX, windZ));
    }

    private void tryMeltBlock(ServerLevel level, BlockPos toMelt) {
        if (validBlock(level, toMelt)) {
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
                } else {
                    level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
                }
            } else if (state.getBlock() == Blocks.SNOW_BLOCK || state.getBlock() == Blocks.POWDER_SNOW) {
                level.setBlockAndUpdate(toMelt, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7));
            } else if (stateAbove.getBlock() == Blocks.POWDER_SNOW) {
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
        return level.getBrightness(LightLayer.BLOCK, toFreeze) < 10;
    }

    private static boolean validBlock(ServerLevel level, BlockPos toFreeze) {
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

    void update(UpdateWeatherChunk updateWeatherChunk, Consumer<BlockPos> posUpdater) {
        boolean recalcChecks = this.windX != updateWeatherChunk.windX || this.windZ != updateWeatherChunk.windZ;

        this.temperature = updateWeatherChunk.temperature;
        this.precipitation = updateWeatherChunk.precipitation;
        this.windX = updateWeatherChunk.windX;
        this.windZ = updateWeatherChunk.windZ;
        this.thunder = updateWeatherChunk.thunder;

        if (recalcChecks) {
            recalculateWindCheckPositions(chunk.getLevel());
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < updateWeatherChunk.posData.length; i++) {
            int key = updateWeatherChunk.posData[i];
            int value = updateWeatherChunk.weatherData[i];
            decode(key, pos);
            query(pos).data(value);
            posUpdater.accept(pos);
        }
    }

    public @Nullable WeatherCategory.WeatherStatus getWeatherStatusWindAware(BlockPos pos) {
        var status = getWeatherStatus(pos);
        if (canSeeWind(pos)) {
            return status;
        }
        return null;
    }

    public @Nullable WeatherCategory.WeatherStatus getWeatherStatus(BlockPos pos) {
        float precip = precipitation(pos);
        if (precipitation(pos) > 0f) {
            WeatherCategory category;
            float temp = temperature(pos);
            float thunder = this.thunder(pos);
            if (isSnowing(temp, precip, thunder)) {
                category = WeatherCategory.SNOW;
            } else if (isSleeting(temp, precip, thunder)) {
                category = WeatherCategory.SLEET;
            } else if (isHailing(temp, precip, thunder)) {
                category = WeatherCategory.HAIL;
            } else {
                category = WeatherCategory.RAIN;
            }
            return new WeatherCategory.WeatherStatus(category, precipitation(pos), windX(pos), windZ(pos), thunder(pos));
        }
        return null;
    }
}
