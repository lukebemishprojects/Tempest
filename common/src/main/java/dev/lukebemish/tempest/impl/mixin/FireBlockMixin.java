package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @ModifyExpressionValue(
        method = "tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isRaining()Z"
        ),
        expect = 2
    )
    private boolean tempest$isRaining(boolean original, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!original) {
            var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null) {
                return true;
            }
        }
        return original;
    }
}
