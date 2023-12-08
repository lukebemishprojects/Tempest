package dev.lukebemish.tempest.impl.forge.mixin;

import dev.lukebemish.tempest.impl.data.world.LevelIdMap;
import dev.lukebemish.tempest.impl.forge.ModNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.NetworkDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(
        method = "placeNewPlayer",
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraftforge/network/NetworkHooks;sendMCRegistryPackets(Lnet/minecraft/network/Connection;Ljava/lang/String;)V")
    )
    private void tempest$syncNetwork(Connection netManager, ServerPlayer player, CallbackInfo ci) {
        //noinspection DataFlowIssue
        MinecraftServer server = ((PlayerList) (Object) this).getServer();
        ModNetworking.INSTANCE.sendTo(
            LevelIdMap.send(server.registryAccess()),
            netManager,
            NetworkDirection.PLAY_TO_CLIENT
        );
    }
}
