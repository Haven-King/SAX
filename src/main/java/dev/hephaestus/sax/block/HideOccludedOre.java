package dev.hephaestus.sax.block;

import dev.hephaestus.fiblib.blocks.BlockFib;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;

public class HideOccludedOre extends BlockFib {
    public HideOccludedOre(Block input, Block output) {
        super(input, output);
    }

    @Override
    protected boolean condition(ServerPlayerEntity serverPlayerEntity) {
        return !serverPlayerEntity.isCreative();
    }

    @Override
    public boolean isSoft() {
        return true;
    }
}
