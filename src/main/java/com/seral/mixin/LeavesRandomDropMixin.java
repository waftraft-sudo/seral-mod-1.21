package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.seral.util.BiomeUtils;

import java.util.List;

@Mixin(LeavesBlock.class)
public class LeavesRandomDropMixin {

    // 既存の onRandomTick の上に追加
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void onIsRandomlyTicking(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // プレイヤーが置いた葉っぱ（PERSISTENT）以外は、常にランダムティックを有効（true）にする
        if (!state.getValue(LeavesBlock.PERSISTENT)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {

        // 湿度（0：乾燥～4：湿潤）
        int vegetationIndex = BiomeUtils.getVegetationIndex(BiomeUtils.getRawVegetation(level, pos));

        // 確率で「ドロップ判定」を行う
        if (!state.getValue(LeavesBlock.PERSISTENT) 
            && random.nextFloat() < 0.0001f * (vegetationIndex + 1)) { // 多湿なほど葉っぱから苗木が出やすい
            
            // 1. ルートテーブルを引くための「状況（Context）」を作成
            // 「素手（ItemStack.EMPTY）で、その場所にあるブロックを壊した」という状況をシミュレート
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

            // 2. ドロップリストを取得（ここでバニラの確率計算が走る）
            List<ItemStack> drops = state.getDrops(builder);

            // 3. ドロップ品の中から「苗木」だけを探して落とす
            for (ItemStack stack : drops) {
                if (stack.is(ItemTags.SAPLINGS)) {
                    
                    // 苗木が見つかったらスポーンさせる
                    ItemEntity itemEntity = new ItemEntity(level, 
                        pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5, 
                        stack.copy()
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
        }
    }
}