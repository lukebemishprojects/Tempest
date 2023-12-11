package dev.lukebemish.tempest.impl;

import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;

public interface FastChunkLookup {
    WeatherChunkData tempest$getChunkData();
    void tempest$setChunkData(WeatherChunkData data);
}
