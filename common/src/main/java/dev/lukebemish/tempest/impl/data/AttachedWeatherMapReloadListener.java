package dev.lukebemish.tempest.impl.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.util.*;

public class AttachedWeatherMapReloadListener extends SimplePreparableReloadListener<Map<ResourceKey<Level>, WeatherMapData>> {
    public static final String DIRECTORY = Constants.MOD_ID + "/noise_maps";
    private final RegistryAccess registryAccess;

    public AttachedWeatherMapReloadListener(RegistryAccess access) {
        super();
        this.registryAccess = access;
    }

    @Override
    protected @NotNull Map<ResourceKey<Level>, WeatherMapData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        Map<ResourceKey<Level>, WeatherMapData> map = new HashMap<>();
        FileToIdConverter fileToIdConverter = FileToIdConverter.json(DIRECTORY);
        resourceManager.listPacks().forEach(packResources -> {
            for (var namespace : packResources.getNamespaces(PackType.SERVER_DATA)) {
                packResources.listResources(PackType.SERVER_DATA, namespace, DIRECTORY, (path, ioSupplier) -> {
                    if (!path.getPath().endsWith(".json")) {
                        return;
                    }
                    ResourceLocation id = fileToIdConverter.fileToId(path);
                    try (var reader = new InputStreamReader(ioSupplier.get())) {
                        JsonElement element = GsonHelper.fromJson(Constants.GSON, reader, JsonElement.class);
                        var result = WeatherMapData.CODEC.parse(ops, element);
                        if (result.result().isEmpty()) {
                            Constants.LOGGER.error("Failed to decode noise map {}: {}", id, result.error().orElseThrow().message());
                        } else {
                            var weatherMap = result.result().get();
                            map.put(weatherMap.level(), weatherMap);
                        }
                    } catch (Exception e) {
                        Constants.LOGGER.error("Failed to open noise map {}", id, e);
                    }
                });
            }
        });
        return map;
    }

    public static final Map<ResourceKey<Level>, WeatherMapData> WEATHER_MAPS = new IdentityHashMap<>();

    @Override
    protected void apply(Map<ResourceKey<Level>, WeatherMapData> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        WEATHER_MAPS.clear();
        WEATHER_MAPS.putAll(object);
        Constants.LOGGER.info("Loaded weather noise maps for {} levels", object.size());
    }

    public static void applyToServer(MinecraftServer server) {
        for (var entry : WEATHER_MAPS.entrySet()) {
            var level = server.getLevel(entry.getKey());
            if (level == null) {
                Constants.LOGGER.error("Failed to apply noise map for non-existent level {}", entry.getKey());
                continue;
            }
            var container = (WeatherContainer) level;
            WeatherMapData.Built data = WeatherMapData.Built.build(entry.getValue(), level);
            container.tempest$weatherMap(data);
        }
    }
}
