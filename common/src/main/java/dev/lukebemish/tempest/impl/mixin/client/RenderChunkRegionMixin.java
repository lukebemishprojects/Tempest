package dev.lukebemish.tempest.impl.mixin.client;

import dev.lukebemish.tempest.impl.client.LevelChunkHolder;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin implements LevelChunkHolder {

    @Shadow
    @Final
    protected Level level;

    @Override
    public Level tempest$level() {
        return level;
    }
}
