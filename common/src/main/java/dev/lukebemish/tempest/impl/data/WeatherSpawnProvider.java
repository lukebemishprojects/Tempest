package dev.lukebemish.tempest.impl.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.tempest.api.WeatherStatus;
import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record WeatherSpawnProvider(WeatherStatus.Kind kind, List<MobSpawnSettings.SpawnerData> spawners) {
    public static final Codec<WeatherSpawnProvider> CODEC = RecordCodecBuilder.create(i -> i.group(
        WeatherStatus.Kind.CODEC.comapFlatMap(kind -> kind == WeatherStatus.Kind.CLEAR ? DataResult.error(() -> "Weather kind 'clear' is not valid here") : DataResult.success(kind), Function.identity()).fieldOf("kind").forGetter(WeatherSpawnProvider::kind),
        MobSpawnSettings.SpawnerData.CODEC.listOf().fieldOf("spawners").forGetter(WeatherSpawnProvider::spawners)
    ).apply(i, WeatherSpawnProvider::new));

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> extendList(WeightedRandomList<MobSpawnSettings.SpawnerData> original, ServerLevel level, BlockPos pos, MobCategory mobCategory) {
        if (level.canSeeSky(pos) && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY()) {
            var kind = WeatherStatus.atPosition(level, pos).kind();
            if (kind == WeatherStatus.Kind.CLEAR) {
                return original;
            }
            var list = new ArrayList<>(original.unwrap());
            list.addAll(ReloadListener.PROVIDERS.getOrDefault(kind, Map.of()).getOrDefault(mobCategory, List.of()));
            return WeightedRandomList.create(list);
        }
        return original;
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        public static final String DIRECTORY = Constants.MOD_ID + "/spawn_providers";

        public ReloadListener() {
            super(Constants.GSON, DIRECTORY);
        }

        public static final Map<WeatherStatus.Kind, Map<MobCategory, List<MobSpawnSettings.SpawnerData>>> PROVIDERS = new EnumMap<>(WeatherStatus.Kind.class);

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
            PROVIDERS.clear();
            object.forEach((id, element) -> {
                var result = CODEC.parse(JsonOps.INSTANCE, element);
                if (result.result().isEmpty()) {
                    Constants.LOGGER.error("Failed to decode spawn provider {}: {}", id, result.error().orElseThrow().message());
                } else {
                    var provider = result.result().get();
                    var map = PROVIDERS.computeIfAbsent(provider.kind(), k -> new EnumMap<>(MobCategory.class));
                    provider.spawners().forEach(spawner -> {
                        var list = map.computeIfAbsent(spawner.type.getCategory(), k -> new ArrayList<>());
                        list.add(spawner);
                    });
                }
            });
            Constants.LOGGER.info("Loaded {} weather spawn providers", object.size());
        }
    }
}
