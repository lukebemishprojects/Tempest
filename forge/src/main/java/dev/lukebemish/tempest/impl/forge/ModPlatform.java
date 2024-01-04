package dev.lukebemish.tempest.impl.forge;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@AutoService(Services.Platform.class)
public final class ModPlatform implements Services.Platform {
    public static final Capability<WeatherChunkData> WEATHER_CHUNK_DATA = CapabilityManager.get(new CapabilityToken<>(){});
    public static final ResourceLocation WEATHER_CHUNK_DATA_LOCATION = Constants.id("weather_status");

    private static final Map<Pair<String, ResourceKey<? extends Registry<?>>>, DeferredRegister<?>> REGISTRIES = new HashMap<>();

    @Override
    public WeatherChunkData getChunkData(LevelChunk chunk) {
        var existing = ((FastChunkLookup) chunk).tempest$getChunkData();
        if (existing != null) {
            return existing;
        } else {
            var data = chunk.getCapability(WEATHER_CHUNK_DATA).orElseThrow(() -> new NullPointerException("Failed to get weather chunk data"));
            ((FastChunkLookup) chunk).tempest$setChunkData(data);
            return data;
        }
    }

    @Override
    public <S, T extends S> Supplier<T> register(Supplier<T> supplier, ResourceLocation location, Registry<S> registry) {
        //noinspection unchecked
        DeferredRegister<S> register = (DeferredRegister<S>) REGISTRIES.computeIfAbsent(Pair.of(location.getNamespace(), registry.key()), k -> DeferredRegister.create(registry.key(), location.getNamespace()));
        return register.register(location.getPath(), supplier);
    }
}
