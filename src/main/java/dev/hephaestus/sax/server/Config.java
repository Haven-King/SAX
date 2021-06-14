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
import java.util.LinkedHashMap;
import java.util.Map;

// Simple config for now
public class Config {
	private static final Path OPTIONS = FabricLoader.getInstance().getConfigDir().resolve("sax").resolve("options.json");
	private static final Path BLOCKS = FabricLoader.getInstance().getConfigDir().resolve("sax").resolve("blocks.json");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final Map<Block, Block> HIDDEN = new LinkedHashMap<>();
	public static byte CHUNK_RADIUS = 4;
	public static boolean LENIENT = false;
	public static int TICK_RATE = 10;

	static {
		HIDDEN.put(Blocks.COAL_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.IRON_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.COPPER_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.GOLD_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.DIAMOND_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.EMERALD_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.REDSTONE_ORE, Blocks.STONE);
		HIDDEN.put(Blocks.LAPIS_ORE, Blocks.STONE);

		HIDDEN.put(Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE);
		HIDDEN.put(Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE);

		HIDDEN.put(Blocks.NETHER_GOLD_ORE, Blocks.NETHERRACK);
		HIDDEN.put(Blocks.NETHER_QUARTZ_ORE, Blocks.NETHERRACK);
		HIDDEN.put(Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK);

		HIDDEN.put(Blocks.MOSSY_COBBLESTONE, Blocks.STONE);
		HIDDEN.put(Blocks.SPAWNER, Blocks.CAVE_AIR);
	}

	public static void load() {
		loadOptions();
		loadBlocks();
	}

	private static void loadOptions() {
		try {
			if (Files.exists(Config.OPTIONS)) {
				JsonObject options = JsonHelper.deserialize(Files.newBufferedReader(Config.OPTIONS));

				if (options.has("chunk_radius")) {
					CHUNK_RADIUS = options.get("chunk_radius").getAsByte();
				}

				if (options.has("lenient")) {
					LENIENT = options.get("lenient").getAsBoolean();
				}

				if (options.has("tick_rate")) {
					TICK_RATE = options.get("tick_rate").getAsInt();
				}
			}

			save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try {
			if (!Files.exists(Config.OPTIONS.getParent())) {
				Files.createDirectories(Config.OPTIONS.getParent());
			}

			JsonObject options = new JsonObject();

			options.addProperty("chunk_radius", CHUNK_RADIUS);
			options.addProperty("lenient", LENIENT);
			options.addProperty("tick_rate", TICK_RATE);

			Writer writer = Files.newBufferedWriter(Config.OPTIONS);
			writer.write(GSON.toJson(options));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadBlocks() {
		try {
			if (!Files.exists(Config.BLOCKS)) {
				Files.createDirectories(Config.BLOCKS.getParent());

				JsonObject blocks = new JsonObject();

				for (Map.Entry<Block, Block> entry : HIDDEN.entrySet()) {
					blocks.addProperty(
							Registry.BLOCK.getId(entry.getKey()).toString(),
							Registry.BLOCK.getId(entry.getValue()).toString()
					);
				}

				Writer writer = Files.newBufferedWriter(Config.BLOCKS);
				writer.write(GSON.toJson(blocks));
				writer.close();
			} else {
				HIDDEN.clear();

				for (Map.Entry<String, JsonElement> element : JsonHelper.deserialize(Files.newBufferedReader(Config.BLOCKS)).entrySet()) {
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
