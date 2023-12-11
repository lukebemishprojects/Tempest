package dev.lukebemish.tempest.impl.forge;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

@AutoService(Services.Platform.class)
public final class ModPlatform implements Services.Platform {
    public static final Capability<WeatherChunkData> WEATHER_CHUNK_DATA = CapabilityManager.get(new CapabilityToken<>(){});
    public static final ResourceLocation WEATHER_CHUNK_DATA_LOCATION = Constants.id("weather_status");

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
}
