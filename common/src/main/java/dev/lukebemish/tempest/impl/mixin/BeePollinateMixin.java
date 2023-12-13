package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeePollinateGoal")
public class BeePollinateMixin {
    @Unique
    private Bee bee;

    @Inject(
        method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V",
        at = @At("RETURN")
    )
    private void tempest$init(Bee bee, CallbackInfo ci) {
        this.bee = bee;
    }

    @ModifyExpressionValue(
        method = {
            "canBeeUse()Z",
            "canBeeContinueToUse()Z"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isRaining()Z"
        )
    )
    private boolean tempest$isRaining(
        boolean original
    ) {
        if (!original) {
            var pos = this.bee.blockPosition();
            //noinspection resource
            var data = Services.PLATFORM.getChunkData(this.bee.level().getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null) {
                return true;
            }
        }
        return original;
    }
}
