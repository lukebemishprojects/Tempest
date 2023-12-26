package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                //noinspection resource
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

    @SuppressWarnings("resource")
    @Inject(
        method = "baseTick()V",
        at = @At("HEAD")
    )
    private void tempest$baseTick(CallbackInfo ci) {
        if (this.isAlive()) {
            var pos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
            if (level().canSeeSky(pos)){
                var weatherData = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos));
                var status = weatherData.getWeatherStatusWindAware(pos);
                if (!this.level().isClientSide()) {
                    if ((this.tickCount & 8) == 0 && status != null && status.category == WeatherCategory.HAIL) {
                        var source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Constants.HAIL_DAMAGE_TYPE));
                        if (this.getType().is(Constants.DAMAGED_BY_HAIL)) {
                            this.hurt(source, status.intensity / 3);
                        } else if (!this.getType().is(Constants.IMMUNE_TO_HAIL)) {
                            this.hurt(source, 0);
                        }
                    }
                }
                if (status != null && status.speed > 0.75) {
                    //noinspection ConstantValue
                    if (!((Object) this instanceof Player player) || (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying))) {
                        if (!this.onGround() || status.speed > 1) {
                            var cDelta = this.getDeltaMovement();
                            var windDelta = new Vec3(status.windX, 0, status.windZ);
                            double inDirection = cDelta.dot(windDelta);
                            if (inDirection < status.speed) {
                                double mult = (status.speed * 0.25 - inDirection) * 0.1;
                                this.setDeltaMovement(cDelta.add(windDelta.scale(mult)));
                            }
                        }
                    }
                }
            }
        }
    }
}
