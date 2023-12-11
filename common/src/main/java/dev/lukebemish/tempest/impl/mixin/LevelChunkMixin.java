package dev.lukebemish.tempest.impl.mixin;

import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements FastChunkLookup {
    @Unique
    private WeatherChunkData tempest$chunkData;

    @Override
    public WeatherChunkData tempest$getChunkData() {
        return tempest$chunkData;
    }

    @Override
    public void tempest$setChunkData(WeatherChunkData data) {
        tempest$chunkData = data;
    }
}
