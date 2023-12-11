package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightningRodBlock.class)
public class LightningRodBlockMixin {
    @ModifyExpressionValue(
        method = "animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"
        )
    )
    private boolean tempest$modifyIsThundering(boolean original, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!original) {
            var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null && status.thunder > 0) {
                return true;
            }
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "onProjectileHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"
        )
    )
    private boolean tempest$modifyIsThundering(boolean original, Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!original) {
            var pos = hit.getBlockPos();
            var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null && status.thunder > 0) {
                return true;
            }
        }
        return original;
    }
}
