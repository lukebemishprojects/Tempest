package dev.lukebemish.tempest.impl.fabriquilt.compat.snowrealmagic;

import com.google.auto.service.AutoService;
import dev.lukebemish.tempest.impl.compat.snowrealmagic.SnowRealMagicCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import snownee.snow.block.SnowVariant;

@AutoService(SnowRealMagicCompat.SnowRealMagicPlatform.class)
public class SnowRealMagicPlatformImpl implements SnowRealMagicCompat.SnowRealMagicPlatform {
    @Override
    public boolean isVariant(Block snowVariant) {
        return snowVariant instanceof SnowVariant;
    }

    @Override
    public BlockState decreaseLayer(Block snowVariant, BlockState original, Level level, BlockPos pos, boolean flag) {
        return ((SnowVariant) snowVariant).decreaseLayer(original, level, pos, flag);
    }

    @Override
    public int layers(BlockState original, Level level, BlockPos pos) {
        return ((SnowVariant) original.getBlock()).layers(original, level, pos);
    }
}
