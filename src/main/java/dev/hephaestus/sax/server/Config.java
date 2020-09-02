package dev.hephaestus.sax.server;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashMap;

// Dummy config for now
public class Config {
	public static final HashMap<Block, Block> HIDDEN = new HashMap<>();

	static {
		HIDDEN.put(Blocks.DIAMOND_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.IRON_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.GOLD_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.COAL_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.REDSTONE_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.LAPIS_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.MOSSY_COBBLESTONE, Blocks.STONE);
		HIDDEN.put(Blocks.SPAWNER, Blocks.CAVE_AIR);
	}
}
