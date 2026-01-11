package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seral.util.TreeShadeUtils;

@Mixin(SaplingBlock.class)
public class SaplingGrowthMixin {

    // JSONで定義した設定を読み込むための「キー」を定義
    private static final ResourceKey<ConfiguredFeature<?, ?>> SHORT_OAK = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath("seral", "short_oak"));
    
    private static final ResourceKey<ConfiguredFeature<?, ?>> TALL_OAK = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath("seral", "tall_oak"));
    
    private static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK = 
        ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.tryParse("minecraft:fancy_oak"));

    @Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
    private void onAdvanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
        
        // 1. オークの苗木であることを確認（他の苗木に影響を与えないため）
        if (!state.is(net.minecraft.world.level.block.Blocks.OAK_SAPLING)) return;

        if (!level.canSeeSky(pos.above())) {
            // System.out.println("Deleting sapling at " + pos + " because it cannot see the sky.");
            level.removeBlock(pos, false);
            ci.cancel();
            return;
        }

        // 2a. 周りに木がある場合は成長させない
        boolean hasNearbyTrees = false;
        for (int i = 0; i < 8; i++) {
            Vec3 offset = TreeShadeUtils.generatePyramidOffsetOutline(level.getRandom(), 3, 8);
            // System.out.println(offset);
            BlockPos checkPos = pos.offset((int)Math.round(offset.x), (int)Math.round(offset.y), (int)Math.round(offset.z));
            if (level.getBlockState(checkPos).is(BlockTags.LEAVES)) {
                hasNearbyTrees = true;
                break;
            }
        }
        if (hasNearbyTrees) {
            // System.out.println("Deleting sapling at " + pos + " due to nearby trees.");
            // System.out.println("/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            level.removeBlock(pos, false);
            ci.cancel();
            return;
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

    // 2x2の巨木構成かどうかを判定する補助メソッド
    private boolean isLargeSaplingSetup(ServerLevel level, BlockPos pos, BlockState state) {
        // ここに2x2の判定ロジックを入れる（今回は1本を優先するため簡略化）
        return false; 
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void onRandomTickCheckDark(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // SaplingBlock の randomTick で暗所判定を行い、十分に暗ければ消す
        // 既存の advanceTree が成長時のみの判定なので、暗い場所で残る問題に対処する
        // 対象は全苗木だが、必要なら特定の苗木種で絞る
        try {
            // スカイライトが低い場合に削除
            if (!level.canSeeSky(pos.above())) {
                level.removeBlock(pos, false);
            }
        } catch (Exception e) {
            // 念のため例外を無視してゲームを止めない
            e.printStackTrace();
        }
    }
}