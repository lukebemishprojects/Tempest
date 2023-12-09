package dev.lukebemish.tempest.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lukebemish.tempest.impl.Constants;
import dev.lukebemish.tempest.impl.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    @Inject(
        method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tempest$frozenUpBlockPreventUsage(Level level, Player player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        var data = Services.PLATFORM.getChunkData(level.getChunkAt(result.getBlockPos()));
        if (data.query(result.getBlockPos()).frozenUp()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
        method = "neighborChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tempest$frozenUpBlockPreventUpdates(Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        //noinspection DataFlowIssue
        var state = ((BlockBehaviour.BlockStateBase) (Object) this);
        if (state.is(Constants.FREEZES_UP)) {
            if (state.canSurvive(level, pos)) {
                var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
                if (data.query(pos).frozenUp()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(
        method = "entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tempest$frozenUpBlockPreventEntityUpdates(Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        //noinspection DataFlowIssue
        var state = ((BlockBehaviour.BlockStateBase) (Object) this);
        if (state.is(Constants.FREEZES_UP)) {
            if (state.canSurvive(level, pos)) {
                var data = Services.PLATFORM.getChunkData(level.getChunkAt(pos));
                if (data.query(pos).frozenUp()) {
                    ci.cancel();
                }
            }
        }
    }

    @ModifyReturnValue(
        method = "getTicker(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;",
        at = @At("RETURN")
    )
    private <T extends BlockEntity> BlockEntityTicker<T> tempest$checkedTicker(BlockEntityTicker<T> original, Level level, BlockEntityType<T> blockEntityType) {
        if (original == null) {
            return null;
        }
        //noinspection DataFlowIssue
        var state = ((BlockBehaviour.BlockStateBase) (Object) this);
        if (state.is(Constants.FREEZES_UP)) {
            return (level1, pos, state1, blockEntity) -> {
                var data = Services.PLATFORM.getChunkData(level1.getChunkAt(pos));
                if (data.query(pos).frozenUp()) {
                    return;
                }
                original.tick(level1, pos, state1, blockEntity);
            };
        }
        return original;
    }
}
