package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SaplingDropUtil {
    
    public static void popSapling(Level level, BlockPos pos, ItemStack itemStack) {
        RandomSource random = level.random;

        // 苗木が見つかったらスポーンさせる
        ItemEntity itemEntity = new ItemEntity(level, 
            pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, 
            itemStack
        );
        
        // ランダムな方向に飛ばす
        // random.nextGaussian() を使うと、自然なバラつきになる
        double dx = random.nextGaussian() * 1.0; // X方向の速度
        double dy = 0.01;                        // Y方向（少し上に跳ねさせる）
        double dz = random.nextGaussian() * 1.0; // Z方向の速度

        itemEntity.setDeltaMovement(dx, dy, dz);
        level.addFreshEntity(itemEntity);

    }
}