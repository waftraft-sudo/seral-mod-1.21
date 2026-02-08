package com.seral.util;

import net.minecraft.core.BlockPos;
import java.util.Set;

public class TreeStructure {
    public Set<BlockPos> logPositions;
    public BlockPos lowestLogPos;
    public BlockPos highestLogPos;
    public boolean has2x2Peak;
    public boolean hasBeehive;

    public TreeStructure() {}
}
