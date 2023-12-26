package dev.lukebemish.tempest.impl.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

public class QuasiRandomChunkVisitor {
    public static final QuasiRandomChunkVisitor INSTANCE = new QuasiRandomChunkVisitor(9875331409874325L, 8);
    private final int[] xs;
    private final int[] zs;
    public final int size;

    public QuasiRandomChunkVisitor(long seed, int iterations) {
        List<Pair<Integer, Integer>> positions = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            List<Pair<Integer, Integer>> temp = new ArrayList<>();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    temp.add(Pair.of(x, z));
                }
            }
            Collections.shuffle(temp, new Random(seed + i));
            positions.addAll(temp);
        }
        xs = new int[positions.size()];
        zs = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            xs[i] = positions.get(i).getFirst();
            zs[i] = positions.get(i).getSecond();
        }
        size = positions.size();
    }

    public BlockPos inChunk(LevelChunk chunk, int index, IntConsumer consumer) {
        if (index < 0 || index >= size) {
            index = chunk.getLevel().getRandom().nextInt(size);
        }
        int x = xs[index];
        int z = zs[index];
        consumer.accept((index + 1) % size);
        return chunk.getPos().getBlockAt(x, 0, z);
    }
}
