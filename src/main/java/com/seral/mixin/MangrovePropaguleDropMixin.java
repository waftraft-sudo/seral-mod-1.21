package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seral.util.BiomeUtils;
import com.seral.util.SaplingDropUtil;

@Mixin(MangrovePropaguleBlock.class)
public class MangrovePropaguleDropMixin {

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        
        // AGEが4（最大） かつ HANGING（ぶら下がっている）状態かチェック
        if (state.getValue(MangrovePropaguleBlock.AGE) == 4 && state.getValue(MangrovePropaguleBlock.HANGING)) {

            // 湿度（0：乾燥～4：湿潤）
            int vegetationIndex = BiomeUtils.getVegetationIndex(BiomeUtils.getRawVegetation(level, pos));
            
            // 確率で苗木を出す
            if (random.nextFloat() < 0.01f * (vegetationIndex + 1)) {
                SaplingDropUtil.popSapling(level, pos, new ItemStack(Items.MANGROVE_PROPAGULE));

                // 苗木を出したら年齢を0に戻す
                level.setBlock(pos, state.setValue(MangrovePropaguleBlock.AGE, 0), 3);
            }
        }
    }
}