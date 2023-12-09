package dev.lukebemish.tempest.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @ModifyExpressionValue(
        method = {
            "getSkyDarken(F)F",
            "getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            "getCloudColor(F)Lnet/minecraft/world/phys/Vec3;"
        },
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/ClientLevel.getRainLevel(F)F"
        )
    )
    private float tempest$modifyRainLevel(float rainLevel) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos cameraBlockPos = new BlockPos(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y), Mth.floor(cameraPos.z));
        //noinspection DataFlowIssue
        var chunk = ((ClientLevel) (Object) this).getChunkAt(cameraBlockPos);
        var data = Services.PLATFORM.getChunkData(chunk);
        var status = data.getWeatherStatus(cameraBlockPos);
        if (status != null && status.intensity > rainLevel) {
            return status.intensity;
        } else {
            return rainLevel;
        }
    }
}
