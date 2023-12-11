package dev.lukebemish.tempest.impl.forge.compat.embeddium;

import dev.lukebemish.tempest.impl.Services;
import dev.lukebemish.tempest.impl.client.OverlaySpriteListener;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

public class EmbeddiumCompat {
    public static void addCompat(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(EmbeddiumCompat::chunkMeshListener);
    }

    private static void chunkMeshListener(ChunkMeshEvent event) {
        var level = event.getWorld();
        var sectionPos = event.getSectionOrigin();
        var chunkPos = sectionPos.chunk();
        var data = Services.PLATFORM.getChunkData(level.getChunk(chunkPos.x, chunkPos.z));
        var icedPositions = data.icedInSection(sectionPos);
        if (icedPositions.isEmpty()) return;
        IntList blackIces = new IntArrayList(icedPositions.size());
        BooleanList frozenUps = new BooleanArrayList(icedPositions.size());
        for (var pos : icedPositions) {
            var atPos = data.query(pos);
            blackIces.add(atPos.blackIce());
            frozenUps.add(atPos.frozenUp());
        }
        event.addMeshAppender(context -> {
            for (int i = 0; i < icedPositions.size(); i++) {
                var pos = icedPositions.get(i);
                var blackIce = blackIces.getInt(i);
                var frozenUp = frozenUps.getBoolean(i);
                TextureAtlasSprite sprite;
                if (blackIce < 1) {
                    continue;
                } else if (blackIce < 6) {
                    sprite = OverlaySpriteListener.getBlackIce1();
                } else if (blackIce < 11) {
                    sprite = OverlaySpriteListener.getBlackIce2();
                } else {
                    sprite = OverlaySpriteListener.getBlackIce3();
                }
                if (frozenUp) {
                    sprite = OverlaySpriteListener.getBlackIce3();
                }
                var state = level.getBlockState(pos);
            }
        });
    }
}
