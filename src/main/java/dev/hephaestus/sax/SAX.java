package dev.hephaestus.sax;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.hephaestus.fiblib.api.BlockFib;
import dev.hephaestus.fiblib.api.BlockFibRegistry;
import dev.hephaestus.sax.server.Config;
import dev.hephaestus.sax.util.FastCaster;
import dev.hephaestus.sax.util.ObfuscatedWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class SAX implements ModInitializer {
    public static final String MODID = "sax";
    public static final String MOD_NAME = "SAX";
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    public static Identifier id(String... path) {
        return new Identifier(MODID, String.join(".", path));
    }

    @Override
    public void onInitialize() {
        Config.load();

        for (Map.Entry<Block, Block> entry : Config.HIDDEN.entrySet()) {
            BlockFibRegistry.register(new BlockFib.Builder(entry.getKey(), entry.getValue())
                    .withCondition(player -> !player.isCreative())
                    .lenient()
                    .build()
            );
        }

        ServerChunkEvents.CHUNK_LOAD.register(FastCaster::load);
        ServerChunkEvents.CHUNK_UNLOAD.register(FastCaster::unload);

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(CommandManager.literal("sax")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("lenient")
                                .then(RequiredArgumentBuilder.<ServerCommandSource, Boolean>argument("lenient", BoolArgumentType.bool())
                                        .executes(SAX::lenient))
                                .executes(SAX::getLenient)
                        )
                        .then(CommandManager.literal("tickRate")
                                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("tickRate", IntegerArgumentType.integer(1))
                                        .executes(SAX::tickRate))
                                .executes(SAX::getTickrate)
                        )
                        .then(CommandManager.literal("chunkRadius")
                                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("chunkRadius", IntegerArgumentType.integer(1, Byte.MAX_VALUE))
                                        .executes(SAX::chunkRadius))
                                .executes(SAX::getChunkRadius)
                        )
        ));
    }

    private static int lenient(CommandContext<ServerCommandSource> context) {
        Config.LENIENT = context.getArgument("lenient", Boolean.class);
        Config.save();

        return 0;
    }

    private static int getLenient(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Lenient: " + Config.LENIENT), false);

        return 0;
    }

    private static int tickRate(CommandContext<ServerCommandSource> context) {
        Config.TICK_RATE = context.getArgument("tickRate", Integer.class);
        Config.save();

        return 0;
    }

    private static int getTickrate(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Tick Rate: " + Config.TICK_RATE), false);

        return 0;
    }

    private static int chunkRadius(CommandContext<ServerCommandSource> context) {
        Config.CHUNK_RADIUS = (byte) (context.getArgument("chunkRadius", Integer.class) & 0xFF);
        Config.save();

        for (ServerWorld world : context.getSource().getMinecraftServer().getWorlds()) {
            ((ObfuscatedWorld) world).reset();
        }

        return 0;
    }

    private static int getChunkRadius(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Chunk Radius: " + Config.CHUNK_RADIUS), false);

        return 0;
    }
}
