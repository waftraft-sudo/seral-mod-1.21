package com.seral.util;

import java.util.Collections;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
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
        Set<Block> shadeSaplings = Set.of(
            Blocks.JUNGLE_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.PALE_OAK_SAPLING
        );
        return shadeSaplings.contains(block);
    }

    // 指定範囲内の明るさの平均を取る
    public static double getAreaAverageLight(ServerLevel level, BlockPos pos, int radius) {

        // 特定の範囲の明るさの平均値をとる
        double sum = 0.0f;
        int count = 0;
        for (int i = -radius; i < radius + 1; i++) {
            for (int j = -radius; j < radius + 1; j++) {
                BlockPos lightPos = pos.offset(i, 0, j);
                int brightness = level.getBrightness(LightLayer.SKY, lightPos);
                if (brightness == 0) {
                    continue;
                }
                sum += level.getBrightness(LightLayer.SKY, lightPos);
                count += 1;
            }
        }
        double averageLight = sum / count;
        // System.out.println("Brightness: " + averageBrightness);

        return averageLight;
    }

    // ほかの木によって日陰になっているかどうかの判定を行うヘルパーメソッド
    public static boolean isInShade(ServerLevel level, BlockPos pos, int radius) {

        DebugUtils.level = level;

        // 自分の葉
        BlockPos thisLeafPos = pos.above();
        BlockState thisLeafState = level.getBlockState(thisLeafPos);
        Block thisLeaf = thisLeafState.getBlock();

        if (!thisLeafState.is(BlockTags.LEAVES)) {
            return true; // 何らかの理由でログの真上のブロックが葉ではないならば日陰とみなす
        }

        // 自分の葉から上に進み、最初の自分と同じ葉以外のブロックを探す
        BlockPos abovePos = thisLeafPos;
        for (int i = 0; i < 4; i++) { // 最初の葉の4つ上の葉まで自分の葉とみなす
            abovePos = thisLeafPos.above(i + 1);
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.getBlock() != thisLeaf) {
                break;
            }
        }

        double averageBrightness = getAreaAverageLight(level, abovePos, radius);
        // System.out.println("Brightness: " + averageBrightness);

        return averageBrightness < 14.75f;
    }
}
