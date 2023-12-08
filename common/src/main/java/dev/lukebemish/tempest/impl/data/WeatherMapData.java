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
    WeatherMap.Provider windSpeed,
    WeatherMap.Provider windDirection
) {
    public static final Codec<WeatherMapData> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(WeatherMapData::level),
        WeatherMap.Provider.CODEC.fieldOf("precipitation").forGetter(WeatherMapData::precipitation),
        WeatherMap.Provider.CODEC.fieldOf("temperature").forGetter(WeatherMapData::temperature),
        WeatherMap.Provider.CODEC.fieldOf("wind_speed").forGetter(WeatherMapData::windSpeed),
        WeatherMap.Provider.CODEC.fieldOf("wind_direction").forGetter(WeatherMapData::windDirection)
    ).apply(i, WeatherMapData::new));

    public interface WeatherMap {
        double query(int x, int z, int gameTime);

        interface Provider {
            Map<ResourceLocation, Codec<? extends Provider>> PROVIDERS = new HashMap<>();
            Codec<Provider> CODEC = ResourceLocation.CODEC.dispatch(Provider::id, PROVIDERS::get);

            ResourceLocation id();
            WeatherMap build(ServerLevel level);
        }
    }
}
