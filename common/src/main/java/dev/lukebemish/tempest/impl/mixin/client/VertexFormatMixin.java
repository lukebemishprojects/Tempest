package dev.lukebemish.tempest.impl.mixin.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.lukebemish.tempest.impl.client.VertexFormatWrapper;
import it.unimi.dsi.fastutil.ints.IntList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatWrapper {
    @Shadow
    @Final
    private IntList offsets;

    @Override
    public int tempest$getOffset(int index) {
        return this.offsets.getInt(index);
    }
}
