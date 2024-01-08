package dev.lukebemish.tempest.impl.compat.snowrealmagic;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import snownee.snow.Hooks;

@AutoService(Services.CompatProvider.class)
public class SnowRealMagicCompat implements Services.CompatProvider {
    public interface SnowRealMagicPlatform {
        SnowRealMagicPlatform INSTANCE = Services.load(SnowRealMagicPlatform.class);

        boolean isVariant(Block snowVariant);

        BlockState decreaseLayer(Block snowVariant, BlockState original, Level level, BlockPos pos, boolean flag);
        int layers(BlockState original, Level level, BlockPos pos);
    }

    @Override
    public Services.@Nullable Melter melter() {
        return (level, pos, original) -> {
            Block snowVariant = original.getBlock();
            if (SnowRealMagicPlatform.INSTANCE.isVariant(snowVariant)) {
                try {
                    // TODO: fix melting further stuff down
                    BlockState newState = SnowRealMagicPlatform.INSTANCE.decreaseLayer(snowVariant, original, level, pos, false);
                    if (newState != original) {
                        level.setBlockAndUpdate(pos, newState);
                        return true;
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        };
    }

    @Override
    public Services.@Nullable Snower snower() {
        return (level, pos, original) -> {
            var mutablePos = pos.mutable();
            for (int i = 0; i < 16; i++) {
                mutablePos.move(Direction.DOWN);
                var newState = level.getBlockState(mutablePos);
                if (
                    !(
                        (SnowRealMagicPlatform.INSTANCE.isVariant(newState.getBlock()) && SnowRealMagicPlatform.INSTANCE.layers(newState, level, mutablePos) < 8) ||
                        Hooks.canContainState(newState)
                    ) && Hooks.canSnowSurvive(newState, level, mutablePos.above())) {
                    var finalPos = mutablePos.above();
                    return Hooks.placeLayersOn(level, finalPos, 1, false, new DirectionalPlaceContext(level, finalPos, Direction.UP, ItemStack.EMPTY, Direction.DOWN), false, true);
                }
            }
            return false;
        };
    }

    @Override
    public boolean shouldLoad() {
        return Services.PLATFORM.modLoaded("snowrealmagic");
    }
}
