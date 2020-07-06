package dev.hephaestus.sax.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.hephaestus.sax.SAX;
import dev.hephaestus.sax.component.BlockAudit;
import dev.hephaestus.sax.component.NaturalBreakRecord;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public class AuditCommand {
    public static void register(CommandDispatcher<ServerCommandSource> commandDispatcher, boolean b) {
        commandDispatcher.register(CommandManager.literal(SAX.MODID)
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("audit").then(CommandManager.argument("players", EntityArgumentType.players()).executes(AuditCommand::auditPlayers))
                .then(CommandManager.literal("chunk").executes(AuditCommand::auditChunk)))
        );
    }

    private static int auditChunk(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        Entity source = serverCommandSourceCommandContext.getSource().getEntity();

        if (source instanceof ServerPlayerEntity && source.getServer() != null) {
            BlockAudit.of(((ServerPlayerEntity) source).getServerWorld().getChunk(source.getBlockPos())).audit(
                    source.getServer()
            );
        }

        return 0;
    }

    private static int auditPlayers(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) throws CommandSyntaxException {
        Entity source = serverCommandSourceCommandContext.getSource().getEntity();

        if (source instanceof ServerPlayerEntity && source.getServer() != null) {
            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(serverCommandSourceCommandContext, "players");
            BlockAudit audit = BlockAudit.of(((ServerPlayerEntity) source).getServerWorld().getChunk(source.getBlockPos()));

            for (ServerPlayerEntity playerEntity : targets) {
                NaturalBreakRecord breakRecord = NaturalBreakRecord.of(playerEntity);
                breakRecord.audit();
                audit.audit(playerEntity);
            }
        }

        return 0;
    }
}
