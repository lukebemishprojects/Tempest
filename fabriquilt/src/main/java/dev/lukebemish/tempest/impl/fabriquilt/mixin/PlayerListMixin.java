package dev.lukebemish.tempest.impl.fabriquilt.mixin;

import dev.lukebemish.tempest.impl.data.world.LevelIdMap;
import dev.lukebemish.tempest.impl.fabriquilt.ModNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(
        method = "placeNewPlayer",
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V")
    )
    private void tempest$syncNetwork(Connection netManager, ServerPlayer player, CallbackInfo ci) {
        //noinspection DataFlowIssue
        MinecraftServer server = ((PlayerList) (Object) this).getServer();
        ServerPlayNetworking.send(
            player,
            new ModNetworking.LevelIdMapPacket(LevelIdMap.send(server.registryAccess()))
        );
    }
}
