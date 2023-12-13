package dev.lukebemish.tempest.impl.fabriquilt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    LivingEntityMixin(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyExpressionValue(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getFriction()F"
        )
    )
    private float tempest$modifyFriction(float original) {
        if (this.onGround()) {
            var pos = this.getBlockPosBelowThatAffectsMyMovement();
            //noinspection resource
            var weatherData = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos)).query(pos);
            int blackIce = weatherData.blackIce();
            if (blackIce != 0) {
                var degree = 1 - (blackIce / 15f);
                degree = degree * degree;

                return degree * original + (1 - degree);
            }
        }
        return original;
    }
}
