package dev.lukebemish.tempest.impl;

import dev.lukebemish.tempest.impl.data.WeatherChunkData;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ServiceLoader;

public class Services {
    public static final Platform PLATFORM = load(Platform.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    public interface Platform {
        WeatherChunkData getChunkData(LevelChunk chunk);
    }
}
