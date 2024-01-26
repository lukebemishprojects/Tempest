package dev.lukebemish.tempest.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukebemish.tempest.impl.data.NoisyWeatherMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Constants {
    public static final String MOD_ID = "tempest";

    private static final ResourceLocation BASE = new ResourceLocation(MOD_ID, MOD_ID);
    public static final Gson GSON = new GsonBuilder().setLenient().create();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final TagKey<Block> FREEZES_UP = TagKey.create(Registries.BLOCK, id("freezes_up"));
    public static final TagKey<Block> BREAKS_WITH_HAIL = TagKey.create(Registries.BLOCK, id("breaks_with_hail"));
    public static final TagKey<Block> SAFE_WITH_HAIL = TagKey.create(Registries.BLOCK, id("safe_with_hail"));
    public static final TagKey<Block> SNOW_PASSTHROUGH = TagKey.create(Registries.BLOCK, id("snow_passthrough"));
    public static final TagKey<EntityType<?>> DAMAGED_BY_HAIL = TagKey.create(Registries.ENTITY_TYPE, id("damaged_by_hail"));
    public static final TagKey<EntityType<?>> IMMUNE_TO_HAIL = TagKey.create(Registries.ENTITY_TYPE, id("immune_to_hail"));

    public static final ResourceKey<DamageType> HAIL_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, id("hail"));

    public static final ResourceLocation TEMPEST_WEATHER_CHECK = id("tempest_weather_check");

    public static ResourceLocation id(String path) {
        return BASE.withPath(path);
    }

    private Constants() {}

    public static void bootstrap() {
        NoisyWeatherMap.register();

        Services.PLATFORM.register(() -> TempestWeatherCheck.TYPE, TEMPEST_WEATHER_CHECK, BuiltInRegistries.LOOT_CONDITION_TYPE);
    }
}
