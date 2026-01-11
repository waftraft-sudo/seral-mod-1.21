package com.seral.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(ItemEntity.class)
public abstract class AutoPlantMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Mixinの対象である ItemEntity 自身を取得
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getItem();
        Level level = itemEntity.level();

        // 1. サーバーサイドでのみ処理（クライアント側での二重設置を防ぐ）
        // 2. アイテムが「ブロック」かつ「苗木」であることを確認
        if (!level.isClientSide() && stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SaplingBlock) {
                
                // 地面にあり、かつ5分経過しているか
                if (itemEntity.onGround() && itemEntity.tickCount >= 6000) {
                    BlockPos pos = itemEntity.blockPosition();
                    
                    // 設置場所が置き換え可能で、かつ下が土系のブロックか確認
                    if (level.getBlockState(pos).canBeReplaced() && 
                        level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
                        
                        // 2. 明るさチェック
                        if (!level.canSeeSky(pos.above())) {
                            return;
                        }
                        // 3. 苗木を設置
                        level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
                        
                        // アイテムを消費
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            itemEntity.discard();
                        }
                        // デスポーンタイマーをリセットする
                        ((ItemEntityAccessor) itemEntity).setAge(0);
                        itemEntity.setPickUpDelay(0);
                    }
                }
            }
        }
    }
}