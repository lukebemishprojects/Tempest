package dev.lukebemish.tempest.impl.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoisyWeatherMap implements WeatherMapData.WeatherMap {
    public static final Codec<Provider> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("rate").forGetter(Provider::rate),
        ResourceKey.codec(Registries.NOISE).fieldOf("parameters").forGetter(Provider::parameters),
        Codec.INT.fieldOf("offset").forGetter(Provider::offset)
    ).apply(i, Provider::new));
    public static final ResourceLocation ID = Constants.id("noisy");

    public record Provider(int rate, ResourceKey<NormalNoise.NoiseParameters> parameters, int offset) implements WeatherMapData.WeatherMap.Provider {

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public WeatherMapData.WeatherMap build(ServerLevel level) {
            NormalNoise.NoiseParameters resolvedParameters = level.registryAccess().registryOrThrow(Registries.NOISE).get(parameters());
            long seed = level.getSeed() + offset;
            return new NoisyWeatherMap(seed, resolvedParameters, rate());
        }
    }

    private final NormalNoise noise;
    private final int rate;

    public NoisyWeatherMap(long seed, NormalNoise.NoiseParameters parameters, int rate) {
        this.rate = rate;
        noise = NormalNoise.create(new LegacyRandomSource(seed), parameters);
    }

    public double query(int x, int z, int gameTime) {
        int relativeTime = gameTime / rate;
        return Mth.clamp(noise.getValue(x, relativeTime, z), -1, 1);
    }

    public static void register() {
        WeatherMapData.WeatherMap.Provider.PROVIDERS.put(ID, CODEC);
    }
}
