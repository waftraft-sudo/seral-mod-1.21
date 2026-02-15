package com.seral.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.Set;

public class TreeStructure {
    public Set<BlockPos> logPositions;
    public BlockPos lowestLogPos;
    public BlockPos highestLogPos;
    public boolean has2x2Peak;
    public boolean hasBeehive;
    
    private static final Map<Block, String> FALLEN_TREE_MAP = Map.of(
        Blocks.OAK_LOG, "fallen_oak_tree",
        Blocks.BIRCH_LOG, "fallen_birch_tree",
        Blocks.SPRUCE_LOG, "fallen_spruce_tree",
        Blocks.JUNGLE_LOG, "fallen_jungle_tree"
    );

    public TreeStructure() {}
    

    // ブロックの種類から「倒木Feature」のIDを返すヘルパーメソッド
    static public Identifier getFallenTreeFeatureId(Block log) {
        String path = "";
        
        path = FALLEN_TREE_MAP.getOrDefault(log, "");
        
        if (!path.isEmpty()) {
            return Identifier.tryParse("minecraft:" + path);
        }
        return null;
    }
}
