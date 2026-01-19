package com.seral.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seral.util.TreeShadeUtils;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

@Mixin(ItemEntity.class)
public abstract class AutoPlantMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Mixinの対象である ItemEntity 自身を取得
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getItem();
        Level level = itemEntity.level();

        // サーバーサイドでのみ処理（クライアント側での二重設置を防ぐ）
        if (level.isClientSide()) {
            return;
        }

        // アイテムが「ブロック」
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        // アイテムが「苗木」
        if (!(blockItem.getBlock() instanceof SaplingBlock)) {
            return;
        }
        
        // 地面にある
        if (!itemEntity.onGround()) {
            return;
        }
        
        // 5分経過している
        if (itemEntity.getAge() < 6000 - 1) { // 5分ちょうどだと設置されないことがあるため-1
            return;
        }

        BlockPos pos = itemEntity.blockPosition();
        BlockState posState = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());

        // 設置場所が置き換え不可能なら終了
        if (!posState.canBeReplaced()) {
            return;
        }

        // 下がしっかりしたブロックであることを確認
        if (!belowState.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return;
        }
        
        // 空が見えて下が土系のブロックなら苗木を置く
        if (belowState.is(BlockTags.DIRT) && level.canSeeSky(pos.above())) {
            // 苗木を設置
            level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
        } else {
            // 落ち葉を置く
            if (!TreeShadeUtils.tryPlaceLeafLitter(level, pos, 1)) {
                return;
            }
        }

        // アイテムを消費
        consumeItem(itemEntity, stack);
    }

    // アイテムを消費
    private void consumeItem(ItemEntity itemEntity, ItemStack stack) {
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


