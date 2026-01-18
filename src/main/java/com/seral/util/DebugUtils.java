package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class DebugUtils {

    private DebugUtils() {}

    public static ServerLevel level;

    // プレイヤーが近くにいるときだけ標準出力（デバッグ用）
    public static void p(BlockPos pos, double range, String message) {
        boolean isPlayerNearby = !level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            new net.minecraft.world.phys.AABB(
                pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                pos.getX() + range, pos.getY() + range, pos.getZ() + range
            )
        ).isEmpty();
        if (isPlayerNearby) {
            System.out.println(message);
        }
    }
}