package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.data.WeatherCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.Fox$SeekShelterGoal")
public abstract class FoxSeekShelterMixin extends FleeSunGoal {
    public FoxSeekShelterMixin(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @ModifyExpressionValue(
        method = "canUse()Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"
        )
    )
    private boolean tempest$modifyWhenToSeekShelter(boolean original) {
        if (!original) {
            var pos = this.mob.blockPosition();
            //noinspection resource
            var data = Services.PLATFORM.getChunkData(this.mob.level().getChunkAt(pos));
            var status = data.getWeatherStatus(pos);
            if (status != null && (status.thunder > 0 || status.category == WeatherCategory.HAIL)) {
                // when it's hailing or thundering, seek shelter
                return true;
            }
        }
        return original;
    }
}
