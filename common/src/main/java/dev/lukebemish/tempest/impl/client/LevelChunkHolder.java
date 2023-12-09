package dev.lukebemish.tempest.impl.client;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public interface LevelChunkHolder {
    Level tempest$level();

    static LevelChunk tempest$chunkAt(RenderChunkRegion region, BlockPos pos) {
        var provider = (LevelChunkHolder) region;
        return provider.tempest$level().getChunkAt(pos);
    }
}
