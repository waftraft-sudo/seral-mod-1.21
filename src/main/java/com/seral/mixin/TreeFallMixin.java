package com.seral.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
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
public class TreeFallMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {

        // ベースの確率
        if (random.nextFloat() < 0.5f) {
            return;
        }

        DebugUtils.level = level;

        // プレイヤー建築保護
        if (state.getValue(BlockStateProperties.PERSISTENT)) {
            return;
        }

        // 湿度（0：乾燥～4：湿潤）
        int vegetationIndex = BiomeUtils.getVegetationIndex(BiomeUtils.getRawVegetation(level, pos));
        // 湿度が高いほど倒れにくい
        if (1.0f / (2 * vegetationIndex + 1.0f) < random.nextFloat()) {
            return;
        }

        // 幹の真上
        BlockState belowState = level.getBlockState(pos.below());
        if (!belowState.is(BlockTags.LOGS)) {
            return;
        }

        BlockPos startLogPos = pos.below();
        TreeStructure tree = identifyWholeTree(level, startLogPos);
        int treeLogCount = tree.logPositions.size();

        // 頂点にrandomTickが起こった時だけ
        if (startLogPos.getY() != tree.highestLogPos.getY()) {
            return;
        }

        // ハチの巣を持っていない
        if (tree.hasBeehive) {
            return;
        }

        // 頂点が2x2の木は1ブロックの木と比べて4倍の確率で倒木判定が起きるため4分の1にする
        if (tree.has2x2Peak) {
            if (1.0f / 4.0f < random.nextFloat()) {
                return;
            }
        }

        // 頂点のログとその上の葉っぱの種類が異なるなら倒す
        Block startLeavesBlock = level.getBlockState(pos).getBlock();
        Block startLogBlock = level.getBlockState(startLogPos).getBlock();
        if (startLeavesBlock != LeafUtils.getLeaves(startLogBlock)) {
            if (!(startLeavesBlock == Blocks.OAK_LEAVES && startLogBlock == Blocks.JUNGLE_LOG)) { // jungle bushの組み合わせなら許容
                DebugUtils.p(startLogPos, 50, "Knocking down wrong head tree at:\n" + "/tp @s " + startLogPos.getX() + " "  + startLogPos.getY() + " "  + startLogPos.getZ());
                knockDownTree(level, tree);
                return;
            }
        }

        // ランダムで倒す
        if (random.nextFloat() < 0.05f) {
            DebugUtils.p(startLogPos, 100, "Randomly knocking down tree at:\n" + "/tp @s " + startLogPos.getX() + " "  + startLogPos.getY() + " "  + startLogPos.getZ());
            knockDownTree(level, tree);
            if (random.nextFloat() < 0.1f && 4 < treeLogCount) {
                placeFallenTree(level, tree.lowestLogPos, startLogBlock);
            }
            return;
        }

        // 日陰判定
        if (TreeShadeUtils.isInShade(level, tree.highestLogPos, 4)) {
            Block saplingBlock = LeafUtils.getSapling(level.getBlockState(pos).getBlock());
            if (!TreeShadeUtils.isShadeSapling(saplingBlock) || random.nextFloat() < 0.01) { // 陰樹は日陰でも倒れにくい
                DebugUtils.p(startLogPos, 100, "Knocking down tree due to shade at:\n" + "/tp @s " + startLogPos.getX() + " "  + startLogPos.getY() + " "  + startLogPos.getZ());
                knockDownTree(level, tree);
                if (random.nextFloat() < 0.1f && 4 < treeLogCount) {
                    placeFallenTree(level, tree.lowestLogPos, startLogBlock);
                }
                return;
            }
        }

    }

    // 倒木を置く
    private static void placeFallenTree(ServerLevel level, BlockPos stumpPos, Block logBlock) {
        RandomSource random = level.getRandom();
        // 木の種類（ブロックID）から、対応する Feature ID を決定
        Identifier featureId = TreeStructure.getFallenTreeFeatureId(logBlock);
        if (featureId == null) {
            // System.out.println("featureId is null");
            // System.out.println("/tp @s " + stumpPos.getX() + " "  + stumpPos.getY() + " "  + stumpPos.getZ());
            return;
        }
        var registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var featureOptional = registry.get(featureId);
        if (!featureOptional.isPresent()) {
            System.out.println("No feature found for id: " + featureId.toString());
            System.out.println("/tp @s " + stumpPos.getX() + " "  + stumpPos.getY() + " "  + stumpPos.getZ());
            return;
        }
        var holder = featureOptional.get();
        var feature = holder.value();
        // System.out.println("Placing fallen tree with feature id: " + featureId.toString() + "\n"
        //     + "/tp @s " + stumpPos.getX() + " "  + stumpPos.getY() + " "  + stumpPos.getZ()
        // );
        feature.place(level, level.getChunkSource().getGenerator(), random, stumpPos);
    }

    // 木を倒す
    private static void knockDownTree(ServerLevel level, TreeStructure tree) {

        DebugUtils.level = level;

        // 木が地面に接していないのであれば処理を中止
        if (!level.getBlockState(tree.lowestLogPos.below()).is(BlockTags.DIRT)) {
            System.out.println("Aborting fallen tree removal because no dirt found below logs with lowest log at: " + tree.lowestLogPos);
            return;
        }

        DebugUtils.p(tree.lowestLogPos, 10, "Removing " + tree.logPositions.size() + " logs and placing fallen tree at\n"
            + "/tp @s " + tree.lowestLogPos.getX() + " " + tree.lowestLogPos.getY() + " " + tree.lowestLogPos.getZ());

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
        boolean hasBeehive = false;

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
                        BlockState neighborState = level.getBlockState(neighbor);

                        if (neighborState.is(BlockTags.BEEHIVES)) {
                            hasBeehive = true;
                            continue;
                        }

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

        BlockPos finalHighestLogPos = highestPos;
        boolean has2x2Peak = logsToRemove.stream().filter(logPos -> 
            !finalHighestLogPos.equals(logPos) && finalHighestLogPos.getY() == logPos.getY()
        ).findAny().isPresent();

        TreeStructure tree = new TreeStructure();
        tree.logPositions = logsToRemove;
        tree.lowestLogPos = lowestPos;
        tree.highestLogPos = highestPos;
        tree.has2x2Peak = has2x2Peak;
        tree.hasBeehive = hasBeehive;
        return tree;
    }
}