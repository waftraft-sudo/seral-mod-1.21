package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.seral.util.LeafUtils;
import com.seral.util.TreeShadeUtils;
import com.seral.util.TreeStructure;
import com.seral.util.BiomeUtils;
import com.seral.util.DebugUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {

        // 確立を低くする
        if (random.nextInt(100) + 1 <= 95) {
            return;
        }

        // プレイヤー建築保護
        if (state.getValue(BlockStateProperties.PERSISTENT)) {
            return;
        }

        // 幹の探索開始 (真下)
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

        TreeStructure tree = identifyWholeTree(level, startLogPos);

        // 湿度に応じて確立で枯れる
        // 湿度（0：乾燥～4：湿潤）
        int vegetationIndex = BiomeUtils.getVegetationIndex(BiomeUtils.getRawVegetation(level, pos));

        // ランダムで倒す
        if (1.0f / tree.logPositions.size() / (vegetationIndex + 1) < random.nextFloat()) {// 大きいほど、湿度が高いほど倒れにくい
            return;
        }

        if (random.nextFloat() < 0.1f) {
            System.out.println("Randomly knocking down tree at " + startLogPos);
            knockDownTree(level, tree);
            return;
        }

        // 日陰判定
        if (isInShade(level, tree.highestLogPos, 4)) {
            Block saplingBlock = LeafUtils.getSapling(level.getBlockState(pos).getBlock());
            if (!TreeShadeUtils.isShadeSapling(saplingBlock) || random.nextFloat() < 0.01) { // 陰樹は日陰でも倒れにくい
                System.out.println("Knocking down tree at " + startLogPos + " due to shade.");
                knockDownTree(level, tree);
                return;
            }
        }

        // // バニラの「倒木」Featureを配置
        // // 木の種類（ブロックID）から、対応する Feature ID を決定
        // Identifier featureId = getFallenTreeFeatureId(lowestLogState);
        
        // if (featureId != null) {
        //     var registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        //     var featureOptional = registry.get(featureId);
        //     if (featureOptional.isPresent()) {
        //         var holder = featureOptional.get();
        //         ConfiguredFeature<?, ?> feature = holder.value();
        //         final BlockPos targetPos = lowestPos;           // make effectively final
        //         feature.place(level, level.getChunkSource().getGenerator(), random, targetPos);
        //     }
        // }
    }

    // // ブロックの種類から「倒木Feature」のIDを返すヘルパーメソッド
    // private Identifier getFallenTreeFeatureId(BlockState logState) {
    //     String path = "";
        
    //     // バニラの倒木IDは "fallen_樹種" という規則になっている
    //     if (logState.is(Blocks.OAK_LOG)) path = "fallen_oak";
    //     else if (logState.is(Blocks.BIRCH_LOG)) path = "fallen_birch";
    //     else if (logState.is(Blocks.SPRUCE_LOG)) path = "fallen_spruce";
    //     else if (logState.is(Blocks.JUNGLE_LOG)) path = "fallen_jungle";
    //     // アカシアやダークオークなどはバニラに「倒木Feature」がない場合があるため
    //     // 必要なら自作JSON ("lithoverdant:fallen_acacia"等) を指定するか、nullを返して何もしない
        
    //     if (!path.isEmpty()) {
    //         return Identifier.tryParse("minecraft:" + path);
    //     }
    //     return null;
    // }

    // ほかの木によって日陰になっているかどうかの判定を行うヘルパーメソッド
    // 上向きピラミッドの指定範囲内に葉ブロックが存在するかをチェックする
    private static boolean isInShade(ServerLevel level, BlockPos pos, int radius) {

        DebugUtils.level = level;

        // 自分の葉
        BlockPos thisLeafPos = pos.above();
        BlockState thisLeafState = level.getBlockState(thisLeafPos);
        Block thisLeaf = thisLeafState.getBlock();

        if (!thisLeafState.is(BlockTags.LEAVES)) {
            return true; // 何らかの理由でログの真上のブロックが葉ではないならば日陰とみなす
        }

        // 自分の葉から上に進み、最初の自分と同じ葉以外のブロックを探す
        BlockPos abovePos = thisLeafPos;
        for (int i = 0; i < 4; i++) { // 最初の葉の4つ上の葉まで自分の葉とみなす
            abovePos = thisLeafPos.above(i + 1);
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.getBlock() != thisLeaf) {
                break;
            }
        }

        // 特定の範囲の明るさの平均値をとる
        double sum = 0.0f;
        int count = 0;
        for (int i = -radius; i < radius + 1; i++) {
            for (int j = -radius; j < radius + 1; j++) {
                BlockPos lightPos = abovePos.offset(i, 0, j);
                int brightness = level.getBrightness(LightLayer.SKY, lightPos);
                if (brightness == 0) {
                    continue;
                }
                sum += level.getBrightness(LightLayer.SKY, lightPos);
                count += 1;
            }
        }
        double averageBrightness = sum / count;
        // System.out.println("Brightness: " + averageBrightness);

        return averageBrightness < 14.5f;
    }

    // 木を倒す
    private static void knockDownTree(ServerLevel level, TreeStructure tree) {

        // 木が地面に接していないのであれば処理を中止
        if (!level.getBlockState(tree.lowestLogPos.below()).is(BlockTags.DIRT)) {
            System.out.println("Aborting fallen tree removal because no dirt found below logs with lowest log at: " + tree.lowestLogPos);
            return;
        }

        System.out.println("Removing " + tree.logPositions.size() + " logs and placing fallen tree at: " + tree.lowestLogPos);
        System.out.println("/tp @s " + tree.lowestLogPos.getX() + " " + tree.lowestLogPos.getY() + " " + tree.lowestLogPos.getZ());

        // 原木の消去
        for (BlockPos p : tree.logPositions) {
            level.removeBlock(p, false); 
        }
    }


    // 幅優先探索 (BFS) で木全体を特定
    private static TreeStructure identifyWholeTree(ServerLevel level, BlockPos startLogPos) {        
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> logsToRemove = new HashSet<>();
        
        BlockPos lowestPos = startLogPos;
        BlockPos highestPos = startLogPos;

        queue.add(startLogPos);
        visited.add(startLogPos);
        int maxLogs = 500;

        while (!queue.isEmpty() && logsToRemove.size() < maxLogs) {
            BlockPos current = queue.poll();
            logsToRemove.add(current);

            // 一番下の座標を更新
            if (current.getY() < lowestPos.getY()) {
                lowestPos = current;
            }

            // 一番上の座標を更新
            if (current.getY() > highestPos.getY()) {
                highestPos = current;
            }

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        BlockPos neighbor = current.offset(x, y, z);
                        var neighborState = level.getBlockState(neighbor);

                        if (!neighborState.is(BlockTags.LOGS) 
                            && !neighborState.is(Blocks.MANGROVE_ROOTS)
                            && !neighborState.is(Blocks.MOSS_CARPET)) {
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

        TreeStructure tree = new TreeStructure();
        tree.logPositions = logsToRemove;
        tree.lowestLogPos = lowestPos;
        tree.highestLogPos = highestPos;
        return tree;
    }
}