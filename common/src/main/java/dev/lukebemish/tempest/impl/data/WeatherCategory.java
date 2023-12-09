package dev.lukebemish.tempest.impl.data;

import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public enum WeatherCategory {
    SNOW(new ResourceLocation("textures/environment/snow.png"), false, 1.0f),
    RAIN(new ResourceLocation("textures/environment/rain.png"), true, 0.2f),
    SLEET(Constants.id("textures/environment/sleet.png"), true, 0.5f);

    public final ResourceLocation location;
    public final boolean fastFalling;
    private final float swirlMult;

    WeatherCategory(ResourceLocation location, boolean fastFalling, float swirlMult) {
        this.location = location;
        this.fastFalling = fastFalling;
        this.swirlMult = swirlMult;
    }

    public static class WeatherStatus {
        public final WeatherCategory category;
        public final float intensity;
        public final float swirl;
        public final float windX;
        public final float windZ;
        public final float speed;

        public WeatherStatus(WeatherCategory category, float intensity, float windSpeed, float windDirection) {
            this.category = category;
            this.intensity = Mth.sqrt(intensity);
            this.swirl = (1 - windSpeed * windSpeed) * this.category.swirlMult;
            this.windX = (float) Math.cos(windDirection * Math.PI);
            this.windZ = (float) Math.sin(windDirection / Math.PI);
            this.speed = windSpeed;
        }
    }
}
