package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // 0. プレイヤー建築保護
        if (state.getValue(BlockStateProperties.PERSISTENT)) {
            return;
        }

        // 1. 確率判定 (例: 200回に1回)
        if (random.nextInt(200) != 0) {
            return;
        }

        // 2. 明るさチェック
        if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos.above()) > 8) {
            return;
        }

        // 3. 幹の探索開始 (真下)
        BlockPos startLogPos = null;
        BlockPos.MutableBlockPos cursor = pos.mutable().move(Direction.DOWN);
        for (int i = 0; i < 10; i++) {
            BlockState s = level.getBlockState(cursor);
            if (s.is(BlockTags.LOGS)) {
                startLogPos = cursor.immutable();
                break;
            }
            if (!s.is(BlockTags.LEAVES)) {
                return;
            } // 障害物
            cursor.move(Direction.DOWN);
        }
        if (startLogPos == null) {
            return;
        }

        // 4. 幅優先探索 (BFS) で木全体を特定
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logsToRemove = new ArrayList<>();
        
        BlockPos lowestPos = startLogPos;
        BlockState lowestLogState = level.getBlockState(startLogPos); // 木の種類を特定するために保存

        queue.add(startLogPos);
        visited.add(startLogPos);
        int maxLogs = 150;

        while (!queue.isEmpty() && logsToRemove.size() < maxLogs) {
            BlockPos current = queue.poll();
            logsToRemove.add(current);

            // 一番下の座標を更新
            if (current.getY() < lowestPos.getY()) {
                lowestPos = current;
                lowestLogState = level.getBlockState(current);
            }

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        BlockPos neighbor = current.offset(x, y, z);
                        if (!level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            continue;
                        }
                        if (visited.contains(neighbor)) {
                            continue;
                        }
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // 5. 原木の消去
        for (BlockPos p : logsToRemove) {
            level.removeBlock(p, false); 
        }

        // 6. バニラの「倒木」Featureを配置
        // 木の種類（ブロックID）から、対応する Feature ID を決定
        Identifier featureId = getFallenTreeFeatureId(lowestLogState);
        
        if (featureId != null) {
            // レジストリからFeatureを取得して配置
            var registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            var featureKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, featureId);

            registry.get(featureKey).ifPresent(holder -> {
                ConfiguredFeature<?, ?> feature = holder.value();
                // 配置実行 (place)
                // ※倒木Featureは通常「地表」に置くものなので、lowestPos（一番下の原木があった場所）に生成
                feature.place(level, level.getChunkSource().getGenerator(), random, lowestPos);
            });
        }
        // ...existing code...
        Identifier featureId = getFallenTreeFeatureId(lowestLogState);
        
        if (featureId != null) {
            var registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            var featureOptional = registry.get(featureId);
            if (featureOptional.isPresent()) {
                var feature = featureOptional.get();
                final BlockPos targetPos = lowestPos;           // make effectively final
                final BlockState targetLogState = lowestLogState;
                feature.place(level, level.getChunkSource().getGenerator(), random, targetPos);
            }
        }
        // ...existing code...
    }

    // ブロックの種類から「倒木Feature」のIDを返すヘルパーメソッド
    private Identifier getFallenTreeFeatureId(BlockState logState) {
        String path = "";
        
        // バニラの倒木IDは "fallen_樹種" という規則になっている
        if (logState.is(Blocks.OAK_LOG)) path = "fallen_oak";
        else if (logState.is(Blocks.BIRCH_LOG)) path = "fallen_birch";
        else if (logState.is(Blocks.SPRUCE_LOG)) path = "fallen_spruce";
        else if (logState.is(Blocks.JUNGLE_LOG)) path = "fallen_jungle";
        // アカシアやダークオークなどはバニラに「倒木Feature」がない場合があるため
        // 必要なら自作JSON ("lithoverdant:fallen_acacia"等) を指定するか、nullを返して何もしない
        
        if (!path.isEmpty()) {
            return Identifier.tryParse("minecraft:" + path);
        }
        return null;
    }
}