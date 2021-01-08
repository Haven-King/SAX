package dev.hephaestus.sax.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface OreChunk {
    void sax_init();
    List<BlockPos> sax_getObfuscatedBlocks();
    Block sax_getBlock(BlockPos pos);
    BlockState getBlockState(BlockPos pos);
}
