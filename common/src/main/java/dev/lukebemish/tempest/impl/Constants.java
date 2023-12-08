package dev.lukebemish.tempest.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lukebemish.tempest.impl.data.NoisyWeatherMap;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Constants {
    public static final String MOD_ID = "tempest";
    public static final Gson GSON = new GsonBuilder().setLenient().create();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final ResourceLocation BASE = new ResourceLocation(MOD_ID, MOD_ID);

    public static ResourceLocation id(String path) {
        return BASE.withPath(path);
    }

    private Constants() {}

    public static void bootstrap() {
        NoisyWeatherMap.register();
    }
}
