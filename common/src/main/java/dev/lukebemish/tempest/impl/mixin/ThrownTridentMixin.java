package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends Entity {
    public ThrownTridentMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyExpressionValue(
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"
        )
    )
    private boolean tempest$modifyIsThundering(boolean original) {
        if (!original) {
            var pos = this.blockPosition();
            //noinspection resource
            var data = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null && status.thunder > 0) {
                return true;
            }
        }
        return original;
    }
}
