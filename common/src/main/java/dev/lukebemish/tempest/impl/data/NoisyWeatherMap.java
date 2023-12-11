package dev.lukebemish.tempest.impl.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;

public class NoisyWeatherMap implements WeatherMapData.WeatherMap {
    public static final Codec<Provider> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("rate").forGetter(Provider::rate),
        DensityFunction.DIRECT_CODEC.fieldOf("density_function").forGetter(Provider::densityFunction),
        Codec.INT.fieldOf("offset").forGetter(Provider::offset)
    ).apply(i, Provider::new));
    public static final ResourceLocation ID = Constants.id("noisy");

    public record Provider(int rate, DensityFunction densityFunction, int offset) implements WeatherMapData.WeatherMap.Provider {

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public WeatherMapData.WeatherMap build(ServerLevel level) {
            long seed = level.getSeed() + offset;
            var visitedDensity = densityFunction().mapAll(new DensityFunction.Visitor() {
                @Override
                public @NotNull DensityFunction apply(DensityFunction densityFunction) {
                    return densityFunction;
                }

                @Override
                public DensityFunction.@NotNull NoiseHolder visitNoise(DensityFunction.NoiseHolder noiseHolder) {
                    var params = noiseHolder.noiseData().value();
                    var noise = NormalNoise.create(new LegacyRandomSource(seed), params);
                    return new DensityFunction.NoiseHolder(noiseHolder.noiseData(), noise);
                }
            });
            return new NoisyWeatherMap(visitedDensity, rate());
        }
    }

    private final DensityFunction densityFunction;
    private final int rate;

    public NoisyWeatherMap(DensityFunction densityFunction, int rate) {
        this.rate = rate;
        this.densityFunction = densityFunction;
    }

    @Override
    public float query(int x, int z, long gameTime) {
        long relativeTime = gameTime / rate;
        double computed = densityFunction.compute(new DensityFunction.FunctionContext() {
            @Override
            public int blockX() {
                return x;
            }

            @Override
            public int blockY() {
                return (int) (relativeTime);
            }

            @Override
            public int blockZ() {
                return z;
            }
        });
        return Mth.clamp((float) computed, -1, 1);
    }

    public static void register() {
        WeatherMapData.WeatherMap.Provider.PROVIDERS.put(ID, CODEC);
    }
}
