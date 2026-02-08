package com.seral.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class LeafUtils {
    // 葉っぱと苗木の対応表
    private static final Map<Block, Block> LEAF_TO_SAPLING = new HashMap<>();

    // 木と葉っぱの対応表
    private static final Map<Block, Block> LOG_TO_LEAVES = new HashMap<>();

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
        LEAF_TO_SAPLING.put(Blocks.PALE_OAK_LEAVES, Blocks.PALE_OAK_SAPLING);
        LOG_TO_LEAVES.put(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        LOG_TO_LEAVES.put(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
        LOG_TO_LEAVES.put(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
        LOG_TO_LEAVES.put(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
        LOG_TO_LEAVES.put(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);
        LOG_TO_LEAVES.put(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES);
        LOG_TO_LEAVES.put(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES);
        LOG_TO_LEAVES.put(Blocks.MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE);
        LOG_TO_LEAVES.put(Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES);
    }

    public static Block getSapling(Block leafBlock) {
        // 対応表にあればそれを返す。なければ空気(null扱いの代わり)などを返す
        return LEAF_TO_SAPLING.getOrDefault(leafBlock, Blocks.AIR);
    }

    public static Block getLeaves(Block logBlock) {
        // 対応表にあればそれを返す。なければ空気(null扱いの代わり)などを返す
        return LOG_TO_LEAVES.getOrDefault(logBlock, Blocks.AIR);
    }
}