package dev.lukebemish.tempest.impl.data;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class WeatherMap {
    private final NormalNoise noise;
    private final int rate;

    public WeatherMap(long seed, NormalNoise.NoiseParameters parameters, int rate) {
        this.rate = rate;
        noise = NormalNoise.create(new LegacyRandomSource(seed), parameters);
    }

    public double query(int x, int z, int gameTime) {
        int relativeTime = gameTime / rate;
        return noise.getValue(x, relativeTime, z);
    }
}
