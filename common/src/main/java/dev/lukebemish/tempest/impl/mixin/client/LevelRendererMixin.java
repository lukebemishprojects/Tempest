package dev.lukebemish.tempest.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.client.FancyPrecipitationRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Unique
    FancyPrecipitationRenderer tempest$precipitationRenderer;

    @Shadow
    @Final
    private float[] rainSizeX;
    @Shadow
    @Final
    private float[] rainSizeZ;

    @Shadow
    private int ticks;

    @Inject(
        method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;Lnet/minecraft/client/renderer/RenderBuffers;)V",
        at = @At("RETURN")
    )
    private void tempest$initPrecipitationRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.tempest$precipitationRenderer = new FancyPrecipitationRenderer(this.rainSizeX, this.rainSizeZ);
    }

    @Inject(
        method = "renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
        at = @At("HEAD")
    )
    private void tempest$renderSnowAndRain(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        this.tempest$precipitationRenderer.renderWeather(lightTexture, partialTick, camX, camY, camZ, this.ticks);
    }

    @Inject(
        method = "tickRain(Lnet/minecraft/client/Camera;)V",
        at = @At("HEAD")
    )
    private void tempest$tickRain(Camera camera, CallbackInfo ci) {
        this.tempest$precipitationRenderer.tickWeather(camera, this.ticks);
    }

    @ModifyExpressionValue(
        method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/ClientLevel.getRainLevel(F)F"
        )
    )
    private float tempest$modifyRainLevel(float rainLevel) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos cameraBlockPos = new BlockPos(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y), Mth.floor(cameraPos.z));
        //noinspection DataFlowIssue
        var chunk = Minecraft.getInstance().level.getChunkAt(cameraBlockPos);
        var data = Services.PLATFORM.getChunkData(chunk);
        var status = data.getWeatherStatus(cameraBlockPos);
        if (status != null && status.intensity > rainLevel) {
            return status.intensity;
        } else {
            return rainLevel;
        }
    }
}
