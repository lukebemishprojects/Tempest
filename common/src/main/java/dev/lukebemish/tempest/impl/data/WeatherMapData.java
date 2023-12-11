package dev.lukebemish.tempest.impl.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public record WeatherMapData(
    ResourceKey<Level> level,
    WeatherMap.Provider precipitation,
    WeatherMap.Provider temperature,
    WeatherMap.Provider windX,
    WeatherMap.Provider windZ,
    WeatherMap.Provider thunder
) {
    public static final Codec<WeatherMapData> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(WeatherMapData::level),
        WeatherMap.Provider.CODEC.fieldOf("precipitation").forGetter(WeatherMapData::precipitation),
        WeatherMap.Provider.CODEC.fieldOf("temperature").forGetter(WeatherMapData::temperature),
        WeatherMap.Provider.CODEC.fieldOf("wind_x").forGetter(WeatherMapData::windX),
        WeatherMap.Provider.CODEC.fieldOf("wind_z").forGetter(WeatherMapData::windZ),
        WeatherMap.Provider.CODEC.fieldOf("thunder").forGetter(WeatherMapData::thunder)
    ).apply(i, WeatherMapData::new));

    public interface WeatherMap {
        float query(int x, int z, long gameTime);

        interface Provider {
            Map<ResourceLocation, Codec<? extends Provider>> PROVIDERS = new HashMap<>();
            Codec<Provider> CODEC = ResourceLocation.CODEC.dispatch(Provider::id, PROVIDERS::get);

            ResourceLocation id();
            WeatherMap build(ServerLevel level);
        }
    }

    public record Built(
        WeatherMap precipitation,
        WeatherMap temperature,
        WeatherMap windX,
        WeatherMap windZ,
        WeatherMap thunder
    ) {
        static Built build(WeatherMapData data, ServerLevel level) {
            return new Built(
                data.precipitation().build(level),
                data.temperature().build(level),
                data.windX().build(level),
                data.windZ().build(level),
                data.thunder().build(level)
            );
        }
    }
}
