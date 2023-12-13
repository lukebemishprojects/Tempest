package dev.lukebemish.tempest.impl.fabriquilt;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.FastChunkLookup;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.WeatherMapData;
import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import dev.lukebemish.tempest.impl.data.world.WeatherData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

@AutoService(Services.Platform.class)
public final class ModPlatform implements Services.Platform {

    @Override
    public WeatherChunkData getChunkData(LevelChunk chunk) {
        var existing = ((FastChunkLookup) chunk).tempest$getChunkData();
        if (existing != null) {
            return existing;
        } else if (chunk instanceof EmptyLevelChunk emptyChunk) {
            return new EmptyData(emptyChunk);
        } else {
            var data = ComponentRegistration.WEATHER_CHUNK_DATA.get(chunk).data;
            ((FastChunkLookup) chunk).tempest$setChunkData(data);
            return data;
        }
    }

    private static final class EmptyData extends WeatherChunkData {
        public EmptyData(EmptyLevelChunk chunk) {
            super(chunk);
        }

        @Override
        protected void update(int pos, int data) {}

        @Override
        public void update() {}

        @Override
        public List<BlockPos> icedInSection(SectionPos pos) {
            return List.of();
        }

        @Override
        public WeatherData query(BlockPos pos) {
            return WeatherData.Empty.INSTANCE;
        }

        @Override
        public void tick(ServerLevel level, WeatherMapData.Built weatherMap) {}
    }
}
