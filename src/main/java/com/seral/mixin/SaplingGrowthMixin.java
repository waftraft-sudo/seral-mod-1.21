package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seral.util.DebugUtils;
import com.seral.util.TreeShadeUtils;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@Mixin(SaplingBlock.class)
public class SaplingGrowthMixin {

    @Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
    private void onAdvanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {

        DebugUtils.level = level;

        FluidState fluidState = level.getFluidState(pos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        BlockPos waterSurfacePos = pos.above(calcDistanceToWaterSurface(level, pos));

        // 空が見えない場合は枯らす。水中の場合は水面から水が見えない場合は枯らす
        if (!isInWater && !level.canSeeSky(pos.above()) || isInWater && !level.canSeeSky(waterSurfacePos.above())) {
            if (!TreeShadeUtils.isShadeSapling(state.getBlock())) { // 陰樹は空が見えなくても枯れにくい
                level.removeBlock(pos, false);
                if (!isInWater) { // 水中なら落ち葉を置かない
                    TreeShadeUtils.placeLeafLitterRandomAmount(level, pos);
                }
                ci.cancel();
                return;
            }
        }

        // 周りに木がある場合は枯らす
        boolean hasNearbyTrees = false;
        int checkRange = TreeShadeUtils.isShadeSapling(state.getBlock()) ? 1 : 12; // 陰樹なら近くの木しかチェックしない
        for (int i = 0; i < 8; i++) {
            Vec3 offset = TreeShadeUtils.generatePyramidOffsetOutline(level.getRandom(), 1, checkRange);
            // System.out.println(offset);
            BlockPos checkPos = pos.offset((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
            if (level.getBlockState(checkPos).is(BlockTags.LEAVES)) {
                hasNearbyTrees = true;
                break;
            }
        }
        if (hasNearbyTrees) {
            DebugUtils.p(pos, 5, "Sapling getting removed due to nearby trees at " + pos);
            level.removeBlock(pos, false);
            TreeShadeUtils.placeLeafLitterRandomAmount(level, pos);
            ci.cancel();
            return;
        }

        if (TreeShadeUtils.isShadeSapling(state.getBlock())) {
            if (tryPlantAnother(level, pos)) {
                ci.cancel();
                return;
            }
            if (random.nextFloat() < 0.5f) {
                return; // 陰樹は成長しにくい
            }
        }

        // // System.out.println("Sapling at " + pos + " has sky access with sky light level: " + skyLight);

        // // 3. 巨木（2x2）の判定を邪魔しないように、今の場所が1本の時だけ実行
        // // (簡易的な判定：隣が苗木でなければ1本とみなす)
        // if (!isLargeSaplingSetup(level, pos, state)) {
            
        //     // 明るさに応じて設定を選択
        //     ResourceKey<ConfiguredFeature<?, ?>> selectedKey = (skyLight > 13) ? TALL_OAK : SHORT_OAK;
        //     if (skyLight == 15 && random.nextFloat() < 0.1F) {
        //         selectedKey = FANCY_OAK;
        //     }

        //     // レジストリから設定を取得して木を生成
        //     level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
        //         .get(selectedKey)
        //         .ifPresent(feature -> {
        //             // 苗木を消して木を置く
        //             level.removeBlock(pos, false);
        //             if (feature.value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
        //                 ci.cancel(); // バニラの成長処理を中止
        //             }
        //         });
        // }
    }

    // 周囲に苗木が多かったらもう一つ苗木を増やす
    private boolean tryPlantAnother(ServerLevel level, BlockPos pos) {
        Block saplingBlock = level.getBlockState(pos).getBlock();
        RandomSource random = level.getRandom();
        
        int[] fitnesses = new int[4];
        int k = 0;
        int maxFitness = 0;
        for (int i = 0; i >= -1; --i) {
            for (int j = 0; j >= -1; --j) {
                int fitness = getTwoByTwoFitness(level, pos.offset(i, 0, j), saplingBlock);
                if (fitness == 4) {
                    // すでに2x2が完成しているなら終了
                    return false;
                }
                maxFitness = (maxFitness < fitness) ? fitness : maxFitness;
                fitnesses[k] = fitness;
                k++;
            }
        }
        if (maxFitness == 0) {
            if (random.nextFloat() < 0.1f) {
                level.removeBlock(pos, false);
                TreeShadeUtils.placeLeafLitterRandomAmount(level, pos);
                // 2x2になれないなら消える
            }
            return false;
        }
        if (maxFitness == 1) {
            return false;
        }
        k = 0;
        for (int i = 0; i >= -1; --i) {
            for (int j = 0; j >= -1; --j) {
                int fitness = fitnesses[k];
                if (fitness < maxFitness) {
                    k++;
                    continue;
                }

                // System.out.println("Auto-planting additional sapling from " + pos);
                // System.out.println("/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                plantAnother(level, pos.offset(i, 0, j), saplingBlock);
                return true;
            }
        }
        return false;
    }

    private int getTwoByTwoFitness(Level level, BlockPos pos, Block block) {
        int fitness = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos checkPos = pos.offset(dx, 0, dz);

                if (level.getBlockState(checkPos).getBlock() == block) {
                    fitness++;
                    continue;
                }

                // 設置場所が置き換え不可能なら終了
                if (!(level.getBlockState(checkPos).canBeReplaced())) {
                    return 0;
                }
                
                // 下が土系のブロックでないなら終了
                if (!level.getBlockState(checkPos.below()).is(BlockTags.DIRT)) {
                    return 0;
                }
            }
        }
        return fitness;
    }

    // 2x2になるように苗木を一つ増やす
    private void plantAnother(Level level, BlockPos pos, Block block) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos anotherPos = pos.offset(dx, 0, dz);
                if (level.getBlockState(anotherPos).getBlock() == block) {
                    continue;
                }
                level.setBlock(anotherPos, block.defaultBlockState(), 3);
                return;
            }
        }
    }

    // 水中から水面までの距離を計算する
    private int calcDistanceToWaterSurface(Level level, BlockPos pos) {
        BlockPos searchPos = pos.mutable();
        int distance = 0;
        while (distance < 100 
            && level.isInsideBuildHeight(searchPos.getY()) 
            && level.getFluidState(searchPos.above(distance + 1)).is(FluidTags.WATER)) {
            distance += 1;
        }
        return distance;
    }
}