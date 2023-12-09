package dev.lukebemish.tempest.impl.mixin;

import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(
        method = "isRainingAt(Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tempest$isRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        //noinspection DataFlowIssue
        var level = (Level) (Object) this;
        var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
        var status = data.getWeatherStatus(pos);
        if (status != null && (status.category == WeatherCategory.RAIN || status.category == WeatherCategory.SLEET)) {
            cir.setReturnValue(true);
        }
    }
}
