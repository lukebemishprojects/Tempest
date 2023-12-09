package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherCheck.class)
public class WeatherCheckMixin {
    @ModifyExpressionValue(
        method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isRaining()Z"
        )
    )
    private boolean tempest$isRaining(boolean original, LootContext context) {
        if (!original) {
            var pos = context.getParamOrNull(LootContextParams.ORIGIN);
            if (pos != null) {
                var level = context.getLevel();
                var blockPos = new BlockPos((int) Math.round(pos.x), (int) Math.round(pos.y), (int) Math.round(pos.z));
                var data = Services.PLATFORM.getChunkData(level.getChunkAt(blockPos));
                var status = data.getWeatherStatus(blockPos);
                if (status != null) {
                    return true;
                }
            }
        }
        return original;
    }
}
