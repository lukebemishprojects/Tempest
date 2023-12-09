package dev.lukebemish.tempest.impl.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public interface DispatchRenderChunkAccessor {
    @Invoker(value = "beginLayer")
    void tempest$beginLayer(BufferBuilder builder);
}
