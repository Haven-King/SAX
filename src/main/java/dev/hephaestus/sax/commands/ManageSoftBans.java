package dev.hephaestus.sax.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.hephaestus.sax.SAX;
import dev.hephaestus.sax.world.level.SoftBanManager;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.UserCache;
import net.minecraft.world.WorldProperties;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class ManageSoftBans {
    public static void register(CommandDispatcher<ServerCommandSource> commandDispatcher, boolean b) {
        commandDispatcher.register(CommandManager.literal(SAX.MODID)
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("unban").then(CommandManager.argument("players", EntityArgumentType.players()).executes(ManageSoftBans::unbanPlayers)))
                .then(CommandManager.literal("ban").then(CommandManager.argument("players", EntityArgumentType.players()).executes(ManageSoftBans::banPlayers)))
                .then(CommandManager.literal("list").executes(ManageSoftBans::listBannedPlayers))
        );
    }

    private static int listBannedPlayers(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        UserCache userCache = serverCommandSourceCommandContext.getSource().getMinecraftServer().getUserCache();

        serverCommandSourceCommandContext.getSource().sendFeedback(
                new LiteralText("Softbanned Players:"), false
        );

        for (Map.Entry<UUID, Date> ban : SoftBanManager.of((WorldProperties) serverCommandSourceCommandContext.getSource().getMinecraftServer().getSaveProperties()).getBans().entrySet()) {
            GameProfile profile = userCache.getByUuid(ban.getKey());
            String player = profile == null ? ban.getKey().toString() : profile.getName();
            serverCommandSourceCommandContext.getSource().sendFeedback(
                    new LiteralText(String.format("  %s - Banned on %s", player, ban.getValue().toString())),
                    false
            );
        }

        return 0;
    }

    private static int banPlayers(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(serverCommandSourceCommandContext, "players");
        SoftBanManager softBanManager = SoftBanManager.of((WorldProperties) serverCommandSourceCommandContext.getSource().getMinecraftServer().getSaveProperties());

        for (ServerPlayerEntity playerEntity : targets) {
            if (!softBanManager.isBanned(playerEntity)) {
                softBanManager.ban(playerEntity);
            }
        }

        return 0;
    }

    private static int unbanPlayers(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(serverCommandSourceCommandContext, "players");
        SoftBanManager softBanManager = SoftBanManager.of((WorldProperties) serverCommandSourceCommandContext.getSource().getMinecraftServer().getSaveProperties());

        for (ServerPlayerEntity playerEntity : targets) {
            if (softBanManager.isBanned(playerEntity)) {
                softBanManager.unBan(playerEntity);
            }
        }

        return 0;
    }
}
