package dev.hephaestus.sax.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Simple config for now
public class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final HashMap<Block, Block> HIDDEN = new HashMap<>();

	public static byte SEARCH_RADIUS = 30;
	public static byte CHUNK_RADIUS = 4;

	static {
		HIDDEN.put(Blocks.DIAMOND_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.IRON_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.GOLD_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.COAL_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.REDSTONE_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.LAPIS_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.MOSSY_COBBLESTONE, Blocks.STONE);
		HIDDEN.put(Blocks.SPAWNER, Blocks.CAVE_AIR);
		HIDDEN.put(Blocks.NETHER_GOLD_ORE, Blocks.NETHERRACK);
		HIDDEN.put(Blocks.NETHER_QUARTZ_ORE, Blocks.NETHERRACK);
		HIDDEN.put(Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK);
	}

	public static void load() {
		Path configDir = FabricLoader.getInstance().getConfigDir().normalize().resolve("sax");
		loadOptions(configDir, configDir.resolve("options.json"));
		loadBlocks(configDir, configDir.resolve("blocks.json"));
	}

	private static void loadOptions(Path dir, Path file) {
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(dir);

				JsonObject options = new JsonObject();

				options.addProperty("search_radius", SEARCH_RADIUS);

				Writer writer = Files.newBufferedWriter(file);
				writer.write(GSON.toJson(options));
				writer.close();
			} else {
				JsonObject options = JsonHelper.deserialize(Files.newBufferedReader(file));
				SEARCH_RADIUS = options.get("search_radius").getAsByte();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void loadBlocks(Path dir, Path file) {
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(dir);

				JsonObject blocks = new JsonObject();

				for (Map.Entry<Block, Block> entry : HIDDEN.entrySet()) {
					blocks.addProperty(
							Registry.BLOCK.getId(entry.getKey()).toString(),
							Registry.BLOCK.getId(entry.getValue()).toString()
					);
				}

				Writer writer = Files.newBufferedWriter(file);
				writer.write(GSON.toJson(blocks));
				writer.close();
			} else {
				HIDDEN.clear();

				for (Map.Entry<String, JsonElement> element : JsonHelper.deserialize(Files.newBufferedReader(file)).entrySet()) {
					HIDDEN.put(
							Registry.BLOCK.get(new Identifier(element.getKey())),
							Registry.BLOCK.get(new Identifier(element.getValue().getAsString()))
					);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
