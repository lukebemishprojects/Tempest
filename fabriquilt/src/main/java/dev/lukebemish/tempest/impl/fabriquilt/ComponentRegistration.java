package dev.lukebemish.tempest.impl.fabriquilt;

import dev.lukebemish.tempest.impl.Constants;
import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentInitializer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;

public class ComponentRegistration implements ChunkComponentInitializer {
    public static final ComponentKey<WeatherDataComponent> WEATHER_CHUNK_DATA =
        ComponentRegistry.getOrCreate(Constants.id("weather_chunk_data"), WeatherDataComponent.class);
    @Override
    public void registerChunkComponentFactories(ChunkComponentFactoryRegistry registry) {
        registry.register(WEATHER_CHUNK_DATA, WeatherDataComponent::new);
    }
}
