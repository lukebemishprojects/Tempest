package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Bee.class)
public abstract class BeeMixin extends Entity {
    public BeeMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyExpressionValue(
        method = {
            "wantsToEnterHive()Z",
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
            var pos = this.blockPosition();
            //noinspection resource
            var data = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null) {
                return true;
            }
        }
        return original;
    }
}
