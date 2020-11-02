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

	public static void load() {
		Path configDir = FabricLoader.getInstance().getConfigDir().normalize().resolve("sax");
		Path configFile = configDir.resolve("blocks.json");

		try {
			if (!Files.exists(configFile)) {
				Files.createDirectories(configDir);

				JsonObject jsonObject = new JsonObject();

				for (Map.Entry<Block, Block> entry : HIDDEN.entrySet()) {
					jsonObject.addProperty(
						Registry.BLOCK.getId(entry.getKey()).toString(),
						Registry.BLOCK.getId(entry.getValue()).toString()
					);
				}

				Writer writer = Files.newBufferedWriter(configFile);
				writer.write(GSON.toJson(jsonObject));
				writer.close();
			} else {
				HIDDEN.clear();

				for (Map.Entry<String, JsonElement> element : JsonHelper.deserialize(Files.newBufferedReader(configFile)).entrySet()) {
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
