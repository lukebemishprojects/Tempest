package dev.lukebemish.tempest.api;

import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;

public class WeatherStatus {
    private final Kind kind;
    private final float intensity;
    private final float temperature;
    private final boolean thunder;
    private final Vec2 wind;

    private WeatherStatus(Kind kind, float intensity, float temperature, boolean thunder, Vec2 wind) {
        this.kind = kind;
        this.intensity = intensity;
        this.temperature = temperature;
        this.thunder = thunder;
        this.wind = wind;
    }

    /**
     * {@return the category of weather present}
     */
    public Kind kind() {
        return kind;
    }

    /**
     * {@return how much precipitation is falling, modified by the biome, from 0 (nothing) to 1}
     */
    public float intensity() {
        return intensity;
    }

    /**
     * {@return the relative temperature of the current weather, modified by the biome, from -1 (cold) to 1 (hot)}
     */
    public float temperature() {
        return temperature;
    }

    /**
     * {@return whether or not there is thunder}
     */
    public boolean thunder() {
        return thunder;
    }

    /**
     * {@return the wind vector, providing both wind direction and speed}
     */
    public Vec2 wind() {
        return wind;
    }

    /**
     * Queries the current weather at a given position in a level. If the level does not have weather associated with it,
     * this will still return a status - that status will simply be {@link Kind#CLEAR} with no precipitation or wind.
     * @param level the level to query
     * @param position the position to query at
     * @return the weather status at the given position
     */
    public WeatherStatus atPosition(Level level, BlockPos position) {
        LevelChunk chunk = level.getChunkAt(position);
        var data = Services.PLATFORM.getChunkData(chunk);
        return data.makeApiStatus(WeatherStatus::new, position);
    }

    public enum Kind {
        /**
         * No precipitation.
         */
        CLEAR,
        /**
         * Rain, but temperatures are above freezing.
         */
        RAIN,
        /**
         * Produced with high precipitation and relatively cold temperatures, especially if it is thundering; damages glass and crops.
         */
        HAIL,
        /**
         * Produced with cold temperatures; coats the ground in snow.
         */
        SNOW,
        /**
         * Freezing rain; produced at medium-cold temperatures. Coats the ground in ice and freezes up redstone components.
         */
        SLEET
    }
}
