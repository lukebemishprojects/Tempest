package dev.lukebemish.tempest.impl.fabriquilt;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.world.level.chunk.LevelChunk;

@AutoService(Services.Platform.class)
public final class ModPlatform implements Services.Platform {
    @Override
    public WeatherChunkData getChunkData(LevelChunk chunk) {
        // TODO: implement
        return null;
    }
}
