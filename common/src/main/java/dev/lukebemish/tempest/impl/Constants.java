package dev.lukebemish.tempest.impl;

import net.minecraft.resources.ResourceLocation;

public final class Constants {
    public static final String MOD_ID = "tempest";

    private static final ResourceLocation BASE = new ResourceLocation(MOD_ID, MOD_ID);

    public static ResourceLocation id(String path) {
        return BASE.withPath(path);
    }

    private Constants() {}
}
