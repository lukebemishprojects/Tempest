package dev.lukebemish.tempest.impl.forge.compat.embeddium;

import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import dev.lukebemish.tempest.impl.client.QuadHelper;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.embeddedt.embeddium.api.ChunkMeshEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public class EmbeddiumCompat {
    public static void addCompat(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(EmbeddiumCompat::chunkMeshListener);
    }

    private static void chunkMeshListener(ChunkMeshEvent event) {
        var level = event.getWorld();
        var sectionPos = event.getSectionOrigin();
        var chunkPos = sectionPos.chunk();
        var data = Services.PLATFORM.getChunkData(level.getChunk(chunkPos.x, chunkPos.z));
        var icedPositions = data.icedInSection(sectionPos);
        if (icedPositions.isEmpty()) return;
        IntList blackIces = new IntArrayList(icedPositions.size());
        BooleanList frozenUps = new BooleanArrayList(icedPositions.size());
        for (var pos : icedPositions) {
            var atPos = data.query(pos);
            blackIces.add(atPos.blackIce());
            frozenUps.add(atPos.frozenUp());
        }
        event.addMeshAppender(context -> {
            RandomSource random = RandomSource.create();
            for (int i = 0; i < Math.min(icedPositions.size(), 1); i++) {
                var pos = icedPositions.get(i);
                var blackIce = blackIces.getInt(i);
                var frozenUp = frozenUps.getBoolean(i);
                TextureAtlasSprite sprite;
                if (blackIce < 1) {
                    continue;
                } else if (blackIce < 6) {
                    sprite = OverlaySpriteListener.getBlackIce1();
                } else if (blackIce < 11) {
                    sprite = OverlaySpriteListener.getBlackIce2();
                } else {
                    sprite = OverlaySpriteListener.getBlackIce3();
                }
                if (frozenUp) {
                    sprite = OverlaySpriteListener.getBlackIce3();
                }
                var state = context.blockRenderView().getBlockState(pos);
                var bakedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                var modelData = bakedModel.getModelData(context.blockRenderView(), pos, state, ModelData.EMPTY);
                random.setSeed(state.getSeed(pos));
                Function<Direction, List<BakedQuad>> quadProvider = dir -> bakedModel.getQuads(state, dir, random, modelData, null);
                var vertexConsumer = context.vertexConsumerProvider().apply(RenderType.translucent());
                var pose = new Matrix4f();
                pose.translation(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
                QuadHelper.renderOverlayQuads(state, pos, pose, quadProvider, frozenUp, context.blockRenderView(), sprite, vertexConsumer);
            }
        });
    }
}
