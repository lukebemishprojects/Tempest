package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeehiveBlockEntity.class)
public class BeehiveBlockEntityMixin {
    @ModifyExpressionValue(
        method = "releaseOccupant(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeData;Ljava/util/List;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeReleaseStatus;Lnet/minecraft/core/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isRaining()Z"
        )
    )
    private static boolean tempest$isRaining(
        boolean original,
        @Local(argsOnly = true) Level level,
        @Local(argsOnly = true, ordinal = 0) BlockPos pos
    ) {
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
