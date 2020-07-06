package dev.hephaestus.sax;

import dev.hephaestus.fiblib.FibLib;
import dev.hephaestus.sax.block.HideOresFromSoftBans;
import dev.hephaestus.sax.commands.AuditCommand;
import dev.hephaestus.sax.commands.ManageSoftBans;
import dev.hephaestus.sax.component.BlockAudit;
import dev.hephaestus.sax.component.NaturalBreakRecord;
import dev.hephaestus.sax.world.level.SoftBanManager;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.ChunkComponentCallback;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.event.LevelComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SAX implements ModInitializer {
    public static final String MODID = "sax";
    public static final String MOD_NAME = "SAX";
    public static final Logger LOG = LogManager.getLogger(MOD_NAME);

    public static Identifier id(String... path) {
        return new Identifier(MODID, String.join(".", path));
    }

    public static final ComponentType<SoftBanManager> SOFT_BAN_MANAGER =
            ComponentRegistry.INSTANCE.registerIfAbsent(id("component", "soft_bans"), SoftBanManager.class);

    public static final ComponentType<BlockAudit> BLOCK_AUDIT =
            ComponentRegistry.INSTANCE.registerIfAbsent(id("component", "block_audit"), BlockAudit.class);

    public static final ComponentType<NaturalBreakRecord> NATURAL_BREAKS =
            ComponentRegistry.INSTANCE.registerIfAbsent(id("component", "natural_breaks"), NaturalBreakRecord.class);

    @Override
    public void onInitialize() {
        LevelComponentCallback.EVENT.register((properties, components) -> components.put(SOFT_BAN_MANAGER, new SoftBanManager()));

        ChunkComponentCallback.EVENT.register((chunk, components) -> components.put(BLOCK_AUDIT, new BlockAudit(chunk)));

        EntityComponents.setRespawnCopyStrategy(NATURAL_BREAKS, RespawnCopyStrategy.ALWAYS_COPY);
        EntityComponentCallback.event(ServerPlayerEntity.class).register((player, components) -> components.put(NATURAL_BREAKS, new NaturalBreakRecord(player)));

        CommandRegistrationCallback.EVENT.register(AuditCommand::register);
        CommandRegistrationCallback.EVENT.register(ManageSoftBans::register);
    }
}
