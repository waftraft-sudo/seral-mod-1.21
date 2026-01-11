package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
}
