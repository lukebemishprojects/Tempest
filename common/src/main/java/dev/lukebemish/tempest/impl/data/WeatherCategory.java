package dev.lukebemish.tempest.impl.data;

import net.minecraft.resources.ResourceLocation;

public enum WeatherCategory {
    SNOW(new ResourceLocation("textures/environment/snow.png"), false, 1.0f),
    RAIN(new ResourceLocation("textures/environment/rain.png"), true, 0.2f);

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
            this.intensity = intensity;
            this.swirl = (1 - windSpeed * windSpeed) * this.category.swirlMult;
            this.windX = (float) Math.cos(windDirection / (2 * Math.PI));
            this.windZ = (float) Math.sin(windDirection / (2 * Math.PI));
            this.speed = windSpeed;
        }
    }
}
