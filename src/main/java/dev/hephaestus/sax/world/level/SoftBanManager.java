package dev.hephaestus.sax.world.level;

import dev.hephaestus.sax.SAX;
import dev.hephaestus.sax.component.NaturalBreakRecord;
import dev.hephaestus.sax.world.OreCountRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.extension.TypeAwareComponent;
import net.minecraft.block.Block;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.dimension.DimensionType;

import java.time.Instant;
import java.util.*;

public class SoftBanManager implements TypeAwareComponent {
    private final HashMap<UUID, Date> softBannedPlayers = new HashMap<>();

    public void ban(ServerPlayerEntity playerEntity) {
        this.broadcastBanMessageToOperators(playerEntity);
        this.softBannedPlayers.put(playerEntity.getUuid(), Date.from(Instant.now()));
//        playerEntity.networkHandler.disconnect(new LiteralText("You've been caught cheating! This is your first warning. Ores will now be obfuscated."));
    }

    public void broadcastBanMessageToOperators(ServerPlayerEntity playerEntity) {
        Text banMessage = new LiteralText(String.format("%s has been detected X-Ray", playerEntity.getNameAndUuid())).formatted(Formatting.RED);

        if (playerEntity.getServer() != null) {
            playerEntity.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                if (player.hasPermissionLevel(2)) {
                    player.sendMessage(banMessage, false);
                }
            });
        }
    }

    public void unBan(ServerPlayerEntity playerEntity) {
        this.softBannedPlayers.remove(playerEntity.getUuid());
    }

    public boolean isBanned(ServerPlayerEntity playerEntity) {
        return this.softBannedPlayers.containsKey(playerEntity.getUuid());
    }

    @SuppressWarnings("unchecked")
    public Map<UUID, Date> getBans() {
        return (Map<UUID, Date>) this.softBannedPlayers.clone();
    }

    public void banIfNecessary(ServerPlayerEntity player) {
        NaturalBreakRecord record = NaturalBreakRecord.of(player);
        RegistryKey<DimensionType> dimension = player.getServerWorld().getDimensionRegistryKey();

        for (Block block : OreCountRegistry.tracked()) {
            int numberBroken = record.count(block);
            int settingNumberBroken = record.count(OreCountRegistry.getSetting(dimension, block));
            int naturalOreGenPerChunk = OreCountRegistry.get(dimension, block);
            float f = (((float) settingNumberBroken) / ((float) naturalOreGenPerChunk)) * 2;

            if (numberBroken > 0/*(numberBroken - (naturalOreGenPerChunk /4)) > f*/) {
                SAX.LOG.info("{} {}", numberBroken, f);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        CompoundTag bans = new CompoundTag();

        for (Map.Entry<UUID, Date> entry : this.softBannedPlayers.entrySet()) {
            bans.put(entry.getKey().toString(), LongTag.of(entry.getValue().getTime()));
        }

        tag.put("bans", bans);

        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag bans = tag.getCompound("bans");

        for (String key : bans.getKeys()) {
            this.softBannedPlayers.put(UUID.fromString(key), new Date(bans.getLong(key)));
        }
    }

    @Override
    public ComponentType<?> getComponentType() {
        return SAX.SOFT_BAN_MANAGER;
    }

    public static SoftBanManager of(WorldProperties properties) {
        return SAX.SOFT_BAN_MANAGER.get(properties);
    }
}
