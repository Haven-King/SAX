package dev.hephaestus.sax.component;

import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import dev.hephaestus.sax.SAX;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.extension.CopyableComponent;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class BlockAudit implements CopyableComponent<BlockAudit> {
    private final Chunk chunk;
    private final HashBiMap<UUID, Short> palette = HashBiMap.create();
    private final HashMap<BlockPos, LinkedList<Short>> audit = new HashMap<>();

    public BlockAudit(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public ComponentType<?> getComponentType() {
        return SAX.BLOCK_AUDIT;
    }

    public void put(UUID uuid, BlockPos pos, Block block) {
        short internalId = palette.getOrDefault(uuid, (short) this.palette.size());
        this.palette.put(uuid, internalId);
        this.audit.computeIfAbsent(pos, (x) -> new LinkedList<>()).add(internalId);
    }

    public void audit(MinecraftServer server) {
        PlayerManager manager = server.getPlayerManager();
        for (BlockPos key : this.audit.keySet()) {
            SAX.LOG.info("{}:", key.toString());

            for (Short s : this.audit.get(key)) {
                UUID id = this.palette.inverse().get(s);
                GameProfile profile = server.getUserCache().getByUuid(id);
                String name = profile == null ? id.toString() : String.format("%s (%s)", profile.getName(), id.toString());
                SAX.LOG.info("  {}", name);
            }

            SAX.LOG.info("");
        }
    }

    public void audit(ServerPlayerEntity playerEntity) {
        short internalId = palette.getOrDefault(playerEntity.getUuid(), (short) this.palette.size());

        if (playerEntity.getServer() != null) {
            PlayerManager manager = playerEntity.getServer().getPlayerManager();
            for (BlockPos key : this.audit.keySet()) {
                SAX.LOG.info("{}:", key.toString());

                for (Short s : this.audit.get(key)) {
                    if (s == internalId) {
                        SAX.LOG.info("  {}", manager.getPlayer(this.palette.inverse().get(s)));
                    }
                }

                SAX.LOG.info("");
            }
        }
    }

    public UUID getPlacer(BlockPos pos) {
        return this.audit.containsKey(pos) ? this.palette.inverse().get(this.audit.get(pos).getFirst() ): null;
    }

    @Override
    public void fromTag(CompoundTag compoundTag) {
        CompoundTag palette = compoundTag.getCompound("palette");
        for (String key : palette.getKeys()) {
            this.palette.put(UUID.fromString(key), palette.getShort(key));
        }

        CompoundTag audit = compoundTag.getCompound("audit");
        for (String key : audit.getKeys()) {
            BlockPos pos = fromString(key);
            List<Short> list = this.audit.computeIfAbsent(pos, (p) -> new LinkedList<>());

            for (Tag tag : audit.getList(key, 2)) {
                list.add(((ShortTag) tag).getShort());
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        CompoundTag palette = new CompoundTag();
        CompoundTag audit = new CompoundTag();

        for (Map.Entry<UUID, Short> entry : this.palette.entrySet()) {
            palette.putShort(entry.getKey().toString(), entry.getValue());
        }

        for (Map.Entry<BlockPos, LinkedList<Short>> entry : this.audit.entrySet()) {
            ListTag tag = new ListTag();

            for (Short s : entry.getValue()) {
                tag.add(ShortTag.of(s));
            }

            audit.put(toString(entry.getKey()), tag);
        }

        compoundTag.put("palette", palette);
        compoundTag.put("audit", audit);

        return compoundTag;
    }

    public static BlockAudit of(Chunk chunk) {
        return SAX.BLOCK_AUDIT.get(chunk);
    }

    private static BlockPos fromString(String s) {
        return BlockPos.fromLong(Long.parseLong(s));
    }

    private static String toString(BlockPos pos) {
        return String.valueOf(pos.asLong());
    }
}
