package dev.lukebemish.tempest.impl.forge.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.client.LevelChunkHolder;
import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import dev.lukebemish.tempest.impl.mixin.client.DispatchRenderChunkAccessor;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class RebuildTaskMixin {
    @Shadow
    @Nullable
    protected RenderChunkRegion region;

    @Unique
    private ChunkRenderDispatcher.RenderChunk renderChunk;

    @Inject(
        method = "<init>(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;DLnet/minecraft/client/renderer/chunk/RenderChunkRegion;Z)V",
        at = @At("RETURN")
    )
    private void tempest$init(ChunkRenderDispatcher.RenderChunk renderChunk, double distAtCreation, @Nullable RenderChunkRegion region, boolean isHighPriority, CallbackInfo ci) {
        this.renderChunk = renderChunk;
    }
    @Inject(
        method = "<init>(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;Lnet/minecraft/world/level/ChunkPos;DLnet/minecraft/client/renderer/chunk/RenderChunkRegion;Z)V",
        at = @At("RETURN")
    )
    private void tempest$init(ChunkRenderDispatcher.RenderChunk renderChunk, @Nullable ChunkPos pos, double d, @Nullable RenderChunkRegion arg, boolean bl, CallbackInfo ci) {
        this.renderChunk = renderChunk;
    }

    @ModifyVariable(
        method = "compile(FFFLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults;",
        at = @At("STORE")
    )
    private Set<RenderType> tempest$captureSet(Set<RenderType> set, @Share("set") LocalRef<Set<RenderType>> setRef) {
        setRef.set(set);
        return set;
    }

    @Inject(
        method = "compile(FFFLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults;",
        at = @At("HEAD")
    )
    private void tempest$clearSet(float x, float y, float z, ChunkBufferBuilderPack buffers, CallbackInfoReturnable<?> ci, @Share("region") LocalRef<RenderChunkRegion> regionRef) {
        regionRef.set(region);
    }

    @WrapOperation(
        method = "compile(FFFLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask$CompileResults;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"
        )
    )
    private void tempest$addBlackIceQuads(
        BlockRenderDispatcher blockRenderDispatcher,
        BlockState state,
        BlockPos pos,
        BlockAndTintGetter blockAndTintGetter,
        PoseStack poseStack,
        VertexConsumer vertexConsumer,
        boolean checkSides,
        RandomSource random,
        ModelData modelData,
        RenderType renderType,
        Operation<Void> operation,
        float x, float y, float z, ChunkBufferBuilderPack buffers,
        @Share("set") LocalRef<Set<RenderType>> setRef,
        @Share("region") LocalRef<RenderChunkRegion> regionRef
    ) {
        operation.call(blockRenderDispatcher, state, pos, blockAndTintGetter, poseStack, vertexConsumer, checkSides, random, modelData, renderType);
        var re = regionRef.get();
        if (re != null) {
            TextureAtlasSprite sprite;
            var level = ((LevelChunkHolder) re).tempest$level();
            var chunk = LevelChunkHolder.tempest$chunkAt(re, pos);
            var data = Services.PLATFORM.getChunkData(chunk);
            var blackIce = data.query(pos).blackIce();
            if (blackIce < 1) {
                return;
            } else if (blackIce < 6) {
                sprite = OverlaySpriteListener.getBlackIce1();
            } else if (blackIce < 11) {
                sprite = OverlaySpriteListener.getBlackIce2();
            } else {
                sprite = OverlaySpriteListener.getBlackIce3();
            }

            var bakedModel = blockRenderDispatcher.getBlockModel(state);

            //boolean flag = Minecraft.useAmbientOcclusion() && state.getLightEmission(blockAndTintGetter, pos) == 0 && bakedModel.useAmbientOcclusion(state, renderType);

            List<BakedQuad> quads = new ArrayList<>(bakedModel.getQuads(
                state,
                null,
                random,
                modelData,
                null
            ));
            quads.addAll(bakedModel.getQuads(
                state,
                Direction.UP,
                random,
                modelData,
                null
            ));
            BufferBuilder translucentBuilder = buffers.builder(RenderType.translucent());
            if (setRef.get().add(RenderType.translucent())) {
                ((DispatchRenderChunkAccessor) renderChunk).tempest$beginLayer(translucentBuilder);
            }
            var posUp = pos.above();
            for (var quad : quads) {
                if (quad.getDirection() == Direction.UP) {
                    int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
                    for (int vert = 0; vert < 4; vert++) {
                        float px = packedPos(vert, 0, vertexData);
                        float py = packedPos(vert, 1, vertexData) + 0.01f;
                        float pz = packedPos(vert, 2, vertexData);
                        Vector4f vector4f = poseStack.last().pose().transform(new Vector4f(px, py, pz, 1.0F));
                        int light = LevelRenderer.getLightColor(level, state, posUp);
                        float u = sprite.getU(px * 16);
                        float v = sprite.getV(pz * 16);
                        float nx = packedNormal(vert, 0, vertexData);
                        float ny = packedNormal(vert, 1, vertexData);
                        float nz = packedNormal(vert, 2, vertexData);
                        translucentBuilder.vertex(vector4f.x, vector4f.y, vector4f.z, 1, 1, 1, 1, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
                    }
                }
            }
        }
    }

    @Unique
    private static float packedPos(int vert, int idx, int[] vertexData) {
        int offset = vert * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
        return Float.intBitsToFloat(vertexData[offset + idx]);
    }

    @Unique
    private static float packedNormal(int vert, int idx, int[] vertexData) {
        int offset = vert * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
        int packedNormal = vertexData[offset];
        return ((byte) ((packedNormal >> (8 * idx)) & 0xFF)) / 127f;
    }

}
