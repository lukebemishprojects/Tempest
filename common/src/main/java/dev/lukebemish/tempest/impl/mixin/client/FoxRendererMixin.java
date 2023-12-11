package dev.lukebemish.tempest.impl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.client.FoxMutableVariant;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoxRenderer.class)
public class FoxRendererMixin {
    @ModifyExpressionValue(
        method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Fox;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Fox;getVariant()Lnet/minecraft/world/entity/animal/Fox$Type;"
        )
    )
    private Fox.Type tempest$modifyFoxVariant(Fox.Type original, Fox entity) {
        var altVariant = ((FoxMutableVariant) entity).tempest$getVariant();
        if (altVariant != null) {
            return altVariant;
        }
        return original;
    }
}
