package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    LivingEntityMixin(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"
            )
        )
    )
    private void tempest$wrapDeltaMovement(LivingEntity livingEntity, double dx, double dy, double dz, Operation<Void> operation) {
        //noinspection DataFlowIssue
        if (!((LivingEntity) (Object) this).shouldDiscardFriction()) {
            if (this.onGround()) {
                var pos = this.getBlockPosBelowThatAffectsMyMovement();
                var weatherData = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos)).query(pos);
                int blackIce = weatherData.blackIce();
                if (blackIce != 0) {
                    var unscaledDx = dx / 0.91f;
                    var unscaledDz = dz / 0.91f;

                    var degree = 1 - (blackIce / 15f);
                    degree = degree * degree;

                    var newDx = degree * dx + (1 - degree) * unscaledDx;
                    var newDz = degree * dz + (1 - degree) * unscaledDz;
                    operation.call(livingEntity, newDx, dy, newDz);
                    return;
                }
            }
        }
        operation.call(livingEntity, dx, dy, dz);
    }
}
