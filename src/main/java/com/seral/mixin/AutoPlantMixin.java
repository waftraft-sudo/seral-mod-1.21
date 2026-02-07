package com.seral.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seral.util.BiomeUtils;
import com.seral.util.DebugUtils;
import com.seral.util.SaplingWitherUtil;
import com.seral.util.TreeShadeUtils;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.Level;

@Mixin(ItemEntity.class)
public abstract class AutoPlantMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {


        // Mixinの対象である ItemEntity 自身を取得
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getItem();

        // サーバーサイドでのみ処理（クライアント側での二重設置を防ぐ）
        Level itemLevel = itemEntity.level();
        if (itemLevel.isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) itemLevel;

        RandomSource random = level.random;
        
        DebugUtils.level = level;

        BlockPos originalPos = itemEntity.blockPosition();

        // アイテムが「ブロック」
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        // アイテムが「苗木」
        if (!(stack.is(ItemTags.SAPLINGS))) {
            return;
        }
        
        // 5分経過
        if (itemEntity.getAge() < 6000 - 1) { // 5分ちょうどだと設置されないことがある
            return;
        }
        
        // 地面にある、またはマングローブの苗木で水中にある
        if (!(itemEntity.onGround() || stack.is(Items.MANGROVE_PROPAGULE) && itemEntity.isInWater())) {
            return;
        }

        // マングローブの苗木なら水の底まで移動
        if (stack.is(Items.MANGROVE_PROPAGULE)) {
            int distance = calcDistanceToWaterBottom(level, originalPos, 10);
            if (0 < distance && distance < 5) {
                itemEntity.setPos(itemEntity.getX(), itemEntity.getY() - distance, itemEntity.getZ());
            }
        }

        BlockPos pos = itemEntity.blockPosition();
        BlockState posState = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());
        BlockState saplingState = blockItem.getBlock().defaultBlockState();
        FluidState fluidState = level.getFluidState(pos);
        Block block = blockItem.getBlock();

        // 設置場所が置き換え不可能なら終了
        if (!posState.canBeReplaced()) {
            return;
        }

        // 下がしっかりしたブロックであることを確認。フェンスや屋根の上に落ち葉が落ちないように
        if (!belowState.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return;
        }

        // 陰樹はPotzol以外では植わりにくい
        if (TreeShadeUtils.isShadeSapling(block) && random.nextFloat() < 0.2f) {
            if (!belowState.is(Blocks.PODZOL)) {
                SaplingWitherUtil.witherItemSapling(level, originalPos);
                return;
            }
        }

        // Podzol以外では湿度に応じて確率で枯れる
        // 湿度（0：乾燥～4：湿潤）{
        if (!belowState.is(Blocks.PODZOL)) {
            int vegetationIndex = BiomeUtils.getVegetationIndex(BiomeUtils.getRawVegetation(level, pos));
            if (0.1 * (vegetationIndex + 1) < random.nextFloat()) { // 湿度が低いほど枯れやすい
                SaplingWitherUtil.witherItemSapling(level, pos);
                return;
            }
        }
        
        // 空が見えて下が植えられるブロックなら苗木を置く
        if (saplingState.canSurvive(level, pos) 
            && level.canSeeSky(originalPos.above())) { // 空が見えるか確認するのは水底に沈む前の位置から
            // 苗木を設置
            if (saplingState.hasProperty(BlockStateProperties.WATERLOGGED) && fluidState.is(FluidTags.WATER)) {
                // waterlog可能で水中ならwaterlog状態で植える
                saplingState = saplingState.setValue(BlockStateProperties.WATERLOGGED, true);
            }
            level.setBlock(pos, saplingState, 3);
        } else {
            // 水中でないなら落ち葉を置く
            if (fluidState.is(FluidTags.WATER) || !TreeShadeUtils.tryPlaceLeafLitter(level, pos, 1)) {
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

    // 水の底までの距離を計算する
    private int calcDistanceToWaterBottom(Level level, BlockPos pos, int limit) {
        BlockPos searchPos = pos.mutable();
        int distance = 0;
        while (distance < limit
            && level.isInsideBuildHeight(searchPos.getY()) 
            && level.getFluidState(searchPos.below(distance + 1)).is(FluidTags.WATER)) {
            distance += 1;
        }
        return distance;
    }

}


