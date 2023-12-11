package dev.lukebemish.tempest.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyExpressionValue(
        method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/ClientLevel.getRainLevel(F)F"
        )
    )
    private static float tempest$modifyRainLevel(float rainLevel, Camera activeRenderInfo, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos cameraBlockPos = new BlockPos(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y), Mth.floor(cameraPos.z));
        var chunk = level.getChunkAt(cameraBlockPos);
        var data = Services.PLATFORM.getChunkData(chunk);
        var status = data.getWeatherStatus(cameraBlockPos);
        if (status != null && status.intensity > rainLevel) {
            return status.intensity;
        } else {
            return rainLevel;
        }
    }

    @ModifyExpressionValue(
        method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/ClientLevel.getThunderLevel(F)F"
        )
    )
    private static float tempest$modifyThunderevel(float rainLevel, Camera activeRenderInfo, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos cameraBlockPos = new BlockPos(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y), Mth.floor(cameraPos.z));
        var chunk = level.getChunkAt(cameraBlockPos);
        var data = Services.PLATFORM.getChunkData(chunk);
        var status = data.getWeatherStatus(cameraBlockPos);
        if (status != null && status.thunder > rainLevel) {
            return status.thunder;
        } else {
            return rainLevel;
        }
    }
}
