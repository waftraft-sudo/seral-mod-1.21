package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        // 2. スカイライトを取得
        int skyLight = level.getBrightness(LightLayer.SKY, pos);

        // 3. 巨木（2x2）の判定を邪魔しないように、今の場所が1本の時だけ実行
        // (簡易的な判定：隣が苗木でなければ1本とみなす)
        if (!isLargeSaplingSetup(level, pos, state)) {
            
            // 明るさに応じて設定を選択
            ResourceKey<ConfiguredFeature<?, ?>> selectedKey = (skyLight > 12) ? TALL_OAK : SHORT_OAK;
            if (skyLight == 15 && random.nextFloat() < 0.1F) {
                selectedKey = FANCY_OAK;
            }

            System.out.println("Generating " + selectedKey + " type oak.");
            // レジストリから設定を取得して木を生成
            level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .get(selectedKey)
                .ifPresent(feature -> {
                    // 苗木を消して木を置く
                    level.removeBlock(pos, false);
                    if (feature.value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
                        ci.cancel(); // バニラの成長処理を中止
                    }
                });
        }
    }

    // 2x2の巨木構成かどうかを判定する補助メソッド
    private boolean isLargeSaplingSetup(ServerLevel level, BlockPos pos, BlockState state) {
        // ここに2x2の判定ロジックを入れる（今回は1本を優先するため簡略化）
        return false; 
    }
}