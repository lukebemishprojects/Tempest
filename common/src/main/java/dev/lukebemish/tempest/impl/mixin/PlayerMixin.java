package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(
        method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isStayingOnGroundSurface()Z"
        )
    )
    private boolean tempest$maybeDontBacKOffEdge(Player self, Operation<Boolean> operation) {
        if (this.onGround()) {
            var pos = this.getBlockPosBelowThatAffectsMyMovement();
            var weatherData = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos)).query(pos);
            int blackIce = weatherData.blackIce();
            if (blackIce >= 4) {
                return false;
            }
        }
        return operation.call(self);
    }
}
