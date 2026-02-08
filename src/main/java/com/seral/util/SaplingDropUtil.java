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
            pos.getX(), pos.getY() - 0.5f, pos.getZ(), 
            itemStack
        );
        
        // ランダムな方向に飛ばす
        // random.nextGaussian() を使うと、自然なバラつきになる
        double dx = random.nextGaussian() * 0.5f + (random.nextBoolean() ? 1.0f : -1.0f) * 1.5; // X方向の速度
        double dy = 0.01f;                        // Y方向（少し上に跳ねさせる）
        double dz = random.nextGaussian() * 0.5f + (random.nextBoolean() ? 1.0f : -1.0f) * 1.5; // Z方向の速度

        itemEntity.setDeltaMovement(dx, dy, dz);
        level.addFreshEntity(itemEntity);

    }
}