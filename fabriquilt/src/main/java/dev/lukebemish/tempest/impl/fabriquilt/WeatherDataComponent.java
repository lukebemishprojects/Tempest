package dev.lukebemish.tempest.impl.fabriquilt;

import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

public class WeatherDataComponent implements Component {
    public final WeatherChunkData data;

    public WeatherDataComponent(ChunkAccess chunk) {
        if (chunk instanceof LevelChunk levelChunk) {
            FastChunkLookup lookup = (FastChunkLookup) chunk;
            var data = lookup.tempest$getChunkData();
            if (data == null) {
                data = new WeatherChunkData(levelChunk, () -> {
                    chunk.setUnsaved(true);
                });
                lookup.tempest$setChunkData(data);
            }
            this.data = data;
        } else {
            this.data = null;
        }
    }

    @Override
    public void readFromNbt(@NotNull CompoundTag tag) {
        if (data == null) return;
        data.load(tag);
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        if (data == null) return;
        data.save(tag);
    }
}
