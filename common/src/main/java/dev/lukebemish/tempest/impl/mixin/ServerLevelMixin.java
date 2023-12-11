package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.WeatherContainer;
import dev.lukebemish.tempest.impl.data.WeatherMapData;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements WeatherContainer {
    @Unique
    private WeatherMapData.Built weatherMap;

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Override
    public void tempest$weatherMap(WeatherMapData.@Nullable Built weatherMap) {
        this.weatherMap = weatherMap;
    }

    @Inject(
        method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At("HEAD")
    )
    private void tempest$tickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (weatherMap != null && this.random.nextInt(8) == 0) {
            var chunkData = Services.PLATFORM.getChunkData(chunk);
            //noinspection DataFlowIssue
            chunkData.tick((ServerLevel) (Object) this, weatherMap);
        }
    }

    @ModifyExpressionValue(
        method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isThundering()Z"
        )
    )
    private boolean tempest$modifyIsThundering(boolean original, LevelChunk chunk, int randomTickSpeed) {
        if (!original) {
            var data = Services.PLATFORM.getChunkData(chunk);
            var status = data.getWeatherStatus(chunk.getPos().getBlockAt(8, 64, 8));
            if (status != null && status.thunder > 0) {
                return true;
            }
        }
        return original;
    }
}
