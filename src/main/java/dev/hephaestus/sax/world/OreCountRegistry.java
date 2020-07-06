package dev.hephaestus.sax.world;

import dev.hephaestus.fiblib.FibLib;
import dev.hephaestus.sax.block.HideOresFromSoftBans;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class OreCountRegistry {
    private static final HashMap<RegistryKey<DimensionType>, HashMap<Block, Integer>> ORE_PER_CHUNK = new HashMap<>();
    private static final HashMap<RegistryKey<DimensionType>, HashMap<Block, Block>> ORE_TO_SETTING = new HashMap<>();
    private static final HashSet<Block> TRACKED_BLOCKS = new HashSet<>();

    /**
     * Tracks the maximum number of ores that generate per chunk in the given dimension.
     *
     * @param dimension is the dimension the ore is generated. This is specified because some mods/servers may have a given
     *                  ore block occur across multiple dimensions.
     * @param setting is the block that surrounds the ore. In the Overworld, this is stone, and in the Nether this is netherrack.
     *                Can be specified for custom dimensions.
     * @param ore is the block that we'd like to track the count of.
     * @param count is the maximum number of the given ore that can generate in each chunk in the given dimension.
     */
    public static void put(RegistryKey<DimensionType> dimension, Block setting, Block ore, int count) {
        if (!(TRACKED_BLOCKS.contains(ore))) {
            FibLib.Blocks.register(new HideOresFromSoftBans(ore, setting));

        }

        ORE_PER_CHUNK.computeIfAbsent(dimension, x -> new HashMap<>()).put(ore, count);
        ORE_TO_SETTING.computeIfAbsent(dimension, x -> new HashMap<>()).put(ore, setting);
        TRACKED_BLOCKS.add(ore);
    }

    public static int get(RegistryKey<DimensionType> dimension, Block ore) {
        return ORE_PER_CHUNK.containsKey(dimension) ? ORE_PER_CHUNK.get(dimension).getOrDefault(ore, 0) : 0;
    }

    public static Block getSetting(RegistryKey<DimensionType> dimension, Block ore) {
        return ORE_TO_SETTING.containsKey(dimension) ? ORE_TO_SETTING.get(dimension).getOrDefault(ore, Blocks.STONE) : Blocks.STONE;
    }

    public static Collection<Block> tracked() {
        return TRACKED_BLOCKS;
    }

    public static boolean isTracked(Block block) {
        return TRACKED_BLOCKS.contains(block);
    }
}
