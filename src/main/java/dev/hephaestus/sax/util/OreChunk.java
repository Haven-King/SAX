package dev.hephaestus.sax.util;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public interface OreChunk {
    void sax_init();
    ListView<BlockPos> sax_getObfuscatedBlocks();
    Block getBlock(BlockPos pos);
}
