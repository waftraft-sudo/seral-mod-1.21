package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public class SaplingWitherUtil {
    public static void witherSapling(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        level.removeBlock(pos, false);
        if (!isInWater) { // 水中なら落ち葉を置かない
            TreeShadeUtils.placeLeafLitterRandomAmount(level, pos);
        }
    }
    public static void witherItemSapling(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        if (!isInWater) { // 水中なら落ち葉を置かない
            TreeShadeUtils.tryPlaceLeafLitter(level, pos, 1);
        }
    }

}