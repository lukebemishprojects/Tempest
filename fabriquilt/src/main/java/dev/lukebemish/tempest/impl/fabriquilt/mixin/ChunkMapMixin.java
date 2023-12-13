package dev.lukebemish.tempest.impl.fabriquilt.mixin;

import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.fabriquilt.ModNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(
        method = "playerLoadedChunk(Lnet/minecraft/server/level/ServerPlayer;Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
        at = @At("RETURN")
    )
    private void tempest$onChunkWatch(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> packetCache, LevelChunk chunk, CallbackInfo ci) {
        var data = Services.PLATFORM.getChunkData(chunk);
        var packet = data.full();
        if (packet != null) {
            ServerPlayNetworking.send(
                player,
                new ModNetworking.UpdateWeatherChunkPacket(packet)
            );
        }
    }
}
