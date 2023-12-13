package dev.lukebemish.tempest.impl.fabriquilt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lukebemish.tempest.impl.data.AttachedWeatherMapReloadListener;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @ModifyExpressionValue(
        method = "loadResources(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess$Frozen;Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/commands/Commands$CommandSelection;ILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/ReloadableServerResources;listeners()Ljava/util/List;"
        )
    )
    private static List<PreparableReloadListener> modifyListeners(
        List<PreparableReloadListener> original,
        ResourceManager resourceManager,
        RegistryAccess.Frozen registryAccess,
        FeatureFlagSet enabledFeatures,
        Commands.CommandSelection commandSelection,
        int functionCompilationLevel,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        var list = new ArrayList<>(original);
        list.add(new AttachedWeatherMapReloadListener(registryAccess));
        return list;
    }
}
