package dev.lukebemish.tempest.impl.data;

import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public enum WeatherCategory {
    SNOW(new ResourceLocation("textures/environment/snow.png"), false, 1.0f),
    RAIN(new ResourceLocation("textures/environment/rain.png"), true, 0.2f),
    SLEET(Constants.id("textures/environment/sleet.png"), true, 0.5f),
    HAIL(Constants.id("textures/environment/hail.png"), true, 0.5f);

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

        public WeatherStatus(WeatherCategory category, float intensity, float windX, float windZ) {
            this.category = category;
            float i = Mth.sqrt(intensity);
            this.intensity = 1 - (0.7f * (1 - i));
            this.speed = Mth.sqrt(windX * windX + windZ * windZ);
            this.swirl = (1 - speed * speed) * this.category.swirlMult;
            this.windX = windX / this.speed;
            this.windZ = windZ / this.speed;
        }
    }
}
