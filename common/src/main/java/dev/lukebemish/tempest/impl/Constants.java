package dev.lukebemish.tempest.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukebemish.tempest.impl.data.NoisyWeatherMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

    public static ResourceLocation id(String path) {
        return BASE.withPath(path);
    }

    private Constants() {}

    public static void bootstrap() {
        NoisyWeatherMap.register();
    }
}
