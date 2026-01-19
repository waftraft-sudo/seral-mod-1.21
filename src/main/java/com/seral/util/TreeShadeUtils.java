package com.seral.util;

import java.util.Collections;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class TreeShadeUtils {

    private TreeShadeUtils() {}

    // 四角錐状に分布するランダムなオフセットを生成するヘルパーメソッド
    // y方向のオフセットの最小値と最大値を調整することで、分布の形状を変えられる
    public static Vec3 generatePyramidOffset(RandomSource random, int minY, int maxY) {
        double y = minY + (maxY - minY) * random.nextDouble();
        double horizontalRange = Math.abs(y);
        double x = (random.nextDouble() * 2 - 1) * horizontalRange;
        double z = (random.nextDouble() * 2 - 1) * horizontalRange;
        return new Vec3(x, y, z);
    }

    // 四角錐状に分布するランダムなオフセットを生成するヘルパーメソッド
    // y方向のオフセットの最小値と最大値を調整することで、分布の形状を変えられる
    // generatePyramidOffsetと同じ内容だが、外側のみをチェックする
    public static Vec3 generatePyramidOffsetOutline(RandomSource random, int minY, int maxY) {
        double y = minY + (maxY - minY) * random.nextDouble();
        double x = (random.nextInt(2) * 2 - 1) * y;
        double z = (random.nextInt(2) * 2 - 1) * y;
        return new Vec3(x, y, z);
    }

    // 上方向の四角錘状の範囲に特定のブロックが存在するかをチェックする
    // condition は、チェック対象の位置が条件を満たすかどうかを判定する関数
    public static boolean checkAboveBlocks(ServerLevel level, BlockPos pos, int yStart, int yEnd, int checks, java.util.function.Predicate<BlockPos> condition) {
        for (int i = 0; i < checks; i++) {
            Vec3 offset = generatePyramidOffset(level.getRandom(), yStart, yEnd);
            BlockPos checkPos = pos.offset((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
            if (!condition.test(checkPos)) {
                return false;
            }
        }
        return true;
    }

    // 上方向の四角錘状の範囲に特定のブロックが存在するかをチェックする
    // condition は、チェック対象の位置が条件を満たすかどうかを判定する関数
    // 外側のみをチェックする
    public static boolean checkAboveBlocksOutline(ServerLevel level, BlockPos pos, int yStart, int yEnd, int checks, java.util.function.Predicate<BlockPos> condition) {
        for (int i = 0; i < checks; i++) {
            Vec3 offset = generatePyramidOffsetOutline(level.getRandom(), yStart, yEnd);
            BlockPos checkPos = pos.offset((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
            if (!condition.test(checkPos)) {
                return false;
            }
        }
        return true;
    }

    public static void placeLeafLitterRandomAmount(Level level, BlockPos pos) {
        // 可能な枚数からランダムに1つ選ぶ
        var possibleValues = BlockStateProperties.SEGMENT_AMOUNT.getPossibleValues();
        int amount = possibleValues.get(level.random.nextInt(possibleValues.size()));
        tryPlaceLeafLitter(level, pos, amount);
    }

    public static boolean tryPlaceLeafLitter(Level level, BlockPos pos, int amount) {        

        // System.out.println("Placing leaf litter at " + pos + " in temperature " + BiomeUtils.getRawTemperature((ServerLevel) level, pos));
        // System.out.println("/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());

        BlockState posState = level.getBlockState(pos);
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(level.random);
        if (posState.is(Blocks.LEAF_LITTER)) {
            dir = posState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            int maxLeafLitterAmount = Collections.max(BlockStateProperties.SEGMENT_AMOUNT.getPossibleValues());
            int existingAmount = posState.getValue(BlockStateProperties.SEGMENT_AMOUNT);
            if (maxLeafLitterAmount <= existingAmount) {
                return false; // 既に最大量がある場合は置けない
            }
            amount += existingAmount;
            amount = Math.min(amount, maxLeafLitterAmount);
        }
        BlockState litterState = Blocks.LEAF_LITTER.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, dir)
            .setValue(BlockStateProperties.SEGMENT_AMOUNT, amount);
        level.setBlock(pos, litterState, 3);
        return true;
    }

    // 陰樹として扱う苗木のセットを返す関数
    public static final boolean isShadeSapling(Block block) {
        return Set.of(
            Blocks.JUNGLE_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.PALE_OAK_SAPLING
        ).contains(block);
    }
}
