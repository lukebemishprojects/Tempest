package dev.lukebemish.tempest.impl.fabriquilt.mixin.client;

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
import dev.lukebemish.tempest.impl.client.QuadHelper;
import dev.lukebemish.tempest.impl.mixin.client.DispatchRenderChunkAccessor;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
            target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;)V"
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
        Operation<Void> operation,
        float x, float y, float z, ChunkBufferBuilderPack buffers,
        @Share("set") LocalRef<Set<RenderType>> setRef,
        @Share("region") LocalRef<RenderChunkRegion> regionRef
    ) {
        operation.call(blockRenderDispatcher, state, pos, blockAndTintGetter, poseStack, vertexConsumer, checkSides, random);
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

            boolean frozenUp = data.query(pos).frozenUp();
            if (frozenUp) {
                sprite = OverlaySpriteListener.getBlackIce3();
            }

            var bakedModel = blockRenderDispatcher.getBlockModel(state);

            BufferBuilder translucentBuilder = buffers.builder(RenderType.translucent());
            if (setRef.get().add(RenderType.translucent())) {
                ((DispatchRenderChunkAccessor) renderChunk).tempest$beginLayer(translucentBuilder);
            }
            Function<Direction, List<BakedQuad>> quadProvider = dir -> bakedModel.getQuads(state, dir, random);
            QuadHelper.renderOverlayQuads(state, pos, poseStack.last().pose(), quadProvider, frozenUp, level, sprite, translucentBuilder);
        }
    }
}
