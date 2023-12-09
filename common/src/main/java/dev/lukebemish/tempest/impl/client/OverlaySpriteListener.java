package dev.lukebemish.tempest.impl.client;

import dev.lukebemish.tempest.impl.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class OverlaySpriteListener implements PreparableReloadListener {
    private static TextureAtlasSprite BLACK_ICE_1;
    private static TextureAtlasSprite BLACK_ICE_2;
    private static TextureAtlasSprite BLACK_ICE_3;

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        BLACK_ICE_1 = null;
        BLACK_ICE_2 = null;
        BLACK_ICE_3 = null;
        return CompletableFuture.supplyAsync(() -> null);
    }

    private static TextureAtlasSprite getBlackIce(int i) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(Constants.id("block/black_ice_"+i));
    }

    public static TextureAtlasSprite getBlackIce1() {
        TextureAtlasSprite blackIce = BLACK_ICE_1;
        if (blackIce != null) {
            return blackIce;
        }
        blackIce = getBlackIce(1);
        BLACK_ICE_1 = blackIce;
        return blackIce;
    }

    public static TextureAtlasSprite getBlackIce2() {
        TextureAtlasSprite blackIce = BLACK_ICE_2;
        if (blackIce != null) {
            return blackIce;
        }
        blackIce = getBlackIce(2);
        BLACK_ICE_2 = blackIce;
        return blackIce;
    }

    public static TextureAtlasSprite getBlackIce3() {
        TextureAtlasSprite blackIce = BLACK_ICE_3;
        if (blackIce != null) {
            return blackIce;
        }
        //blackIce = getBlackIce(3);
        blackIce = getBlackIce(3);
        BLACK_ICE_3 = blackIce;
        return blackIce;
    }
}
