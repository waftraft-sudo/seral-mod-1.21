package com.seral.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class LeafUtils {
    // 葉っぱと苗木の対応表を作成
    private static final Map<Block, Block> LEAF_TO_SAPLING = new HashMap<>();

    static {
        LEAF_TO_SAPLING.put(Blocks.OAK_LEAVES, Blocks.OAK_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.CHERRY_LEAVES, Blocks.CHERRY_SAPLING);
        LEAF_TO_SAPLING.put(Blocks.MANGROVE_LEAVES, Blocks.MANGROVE_PROPAGULE);
        LEAF_TO_SAPLING.put(Blocks.AZALEA_LEAVES, Blocks.AZALEA);
        LEAF_TO_SAPLING.put(Blocks.FLOWERING_AZALEA_LEAVES, Blocks.FLOWERING_AZALEA);
        // 必要なら自分のModのブロックもここに追加
    }

    public static Block getSapling(Block leafBlock) {
        // 対応表にあればそれを返す。なければ空気(null扱いの代わり)などを返す
        return LEAF_TO_SAPLING.getOrDefault(leafBlock, Blocks.AIR);
    }
}