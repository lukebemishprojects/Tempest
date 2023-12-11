package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.client.FoxMutableVariant;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public abstract class FoxMixin extends LivingEntity implements FoxMutableVariant {
    protected FoxMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Fox.Type tempest$getVariant() {
        return tempest$variant;
    }

    @Unique
    private Fox.Type tempest$variant;

    @Inject(
        method = "aiStep()V",
        at = @At("HEAD")
    )
    private void tempest$modifyFoxVariant(CallbackInfo ci) {
        //noinspection resource
        if (this.level().isClientSide) {
            var pos = this.blockPosition();
            //noinspection resource
            var data = Services.PLATFORM.getChunkData(this.level().getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null && status.category == WeatherCategory.SNOW) {
                tempest$variant = Fox.Type.SNOW;
            } else {
                tempest$variant = null;
            }
        }
    }

    @ModifyExpressionValue(
        method = "tick()V",
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
