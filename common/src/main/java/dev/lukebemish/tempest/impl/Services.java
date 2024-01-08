package dev.lukebemish.tempest.impl;

import dev.lukebemish.tempest.impl.data.world.WeatherChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public final class Services {
    private Services() {}

    public static final Platform PLATFORM = load(Platform.class);

    private static final List<Snower> SNOWERS;
    private static final List<Melter> MELTERS;

    public static boolean snow(ServerLevel level, BlockPos pos, BlockState original) {
        for (var snower : SNOWERS) {
            if (snower.snow(level, pos, original)) {
                return true;
            }
        }
        return false;
    }

    public static boolean melt(ServerLevel level, BlockPos pos, BlockState original) {
        for (var melter : MELTERS) {
            if (melter.melt(level, pos, original)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    static {
        var melters = new ArrayList<Melter>();
        var snowers = new ArrayList<Snower>();
        for (var provider : ServiceLoader.load(CompatProvider.class)) {
            if (provider.shouldLoad()) {
                var compat = provider.compat();
                melters.add(new Melter() {
                    boolean valid = true;

                    @Override
                    public boolean melt(ServerLevel level, BlockPos pos, BlockState original) {
                        if (valid) {
                            try {
                                return compat.melt(level, pos, original);
                            } catch (Throwable t) {
                                valid = false;
                                Constants.LOGGER.error("Failed to melt block at {} with provider {}", pos, provider.getClass().getName(), t);
                            }
                        }
                        return false;
                    }
                });
                snowers.add(new Snower() {
                    boolean valid = true;

                    @Override
                    public boolean snow(ServerLevel level, BlockPos pos, BlockState original) {
                        if (valid) {
                            try {
                                return compat.snow(level, pos, original);
                            } catch (Throwable t) {
                                valid = false;
                                Constants.LOGGER.error("Failed to snow block at {} with provider {}", pos, provider.getClass().getName(), t);
                            }
                        }
                        return false;
                    }
                });
            }
        }
        MELTERS = List.copyOf(melters);
        SNOWERS = List.copyOf(snowers);
    }

    public interface Platform {
        WeatherChunkData getChunkData(LevelChunk chunk);
        <S, T extends S> Supplier<T> register(Supplier<T> supplier, ResourceLocation location, Registry<S> registry);

        boolean modLoaded(String modId);
    }

    @FunctionalInterface
    private interface Melter {
        boolean melt(ServerLevel level, BlockPos pos, BlockState original);
    }

    @FunctionalInterface
    private interface Snower {
        boolean snow(ServerLevel level, BlockPos pos, BlockState original);
    }

    public interface Compat {
        boolean melt(ServerLevel level, BlockPos pos, BlockState original);
        boolean snow(ServerLevel level, BlockPos pos, BlockState original);
    }

    public interface CompatProvider {
        Compat compat();

        boolean shouldLoad();
    }
}
