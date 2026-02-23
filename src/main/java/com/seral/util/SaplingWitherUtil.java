package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SaplingWitherUtil {
    public static void witherSapling(Level level, BlockPos pos) {
        BlockState posState = level.getBlockState(pos);
        Block litterBlock = posState.getBlock() == Blocks.CHERRY_SAPLING ? Blocks.PINK_PETALS : Blocks.LEAF_LITTER;
        FluidState fluidState = level.getFluidState(pos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        level.removeBlock(pos, false);
        if (!isInWater) { // 水中なら落ち葉を置かない
            TreeShadeUtils.placeLitterRandomAmount(level, pos, litterBlock);
        }
    }
    public static void witherItemSapling(Level level, BlockPos pos, Block litterBlock) {
        FluidState fluidState = level.getFluidState(pos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        if (!isInWater) { // 水中なら落ち葉を置かない
            TreeShadeUtils.tryPlaceLeafLitter(level, pos, 1, litterBlock);
        }
    }

}