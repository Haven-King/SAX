package dev.hephaestus.sax.component;

import dev.hephaestus.sax.SAX;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.extension.TypeAwareComponent;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.HashMap;
import java.util.Optional;

public class NaturalBreakRecord implements TypeAwareComponent {
    private final ServerPlayerEntity player;
    private final HashMap<Block, Integer> blockBreakRecord = new HashMap<>();

    public NaturalBreakRecord(ServerPlayerEntity serverPlayerEntity) {
        this.player = serverPlayerEntity;
    }

    public void onBreak(Block block) {
        blockBreakRecord.compute(block, (x, existing) -> (existing == null ? 0 : existing) + 1);
    }

    public int count(Block setting) {
        return this.blockBreakRecord.getOrDefault(setting, 0);
    }

    public void audit() {
        SAX.LOG.info("{}", this.player.getNameAndUuid().getString());

        for (Block block : this.blockBreakRecord.keySet()) {
            SAX.LOG.info("{}: {} broken", block, this.blockBreakRecord.get(block));
        }
    }

    @Override
    public ComponentType<?> getComponentType() {
        return SAX.NATURAL_BREAKS;
    }

    @Override
    public void fromTag(CompoundTag compoundTag) {
        CompoundTag record = compoundTag.getCompound("record");

        for (String key : record.getKeys()) {
            try {
                RegistryKey<Block> blockRegistryKey = RegistryKey.of(Registry.BLOCK_KEY, new Identifier(key));
                this.blockBreakRecord.put(Registry.BLOCK.get(blockRegistryKey), record.getInt(key));
            } catch (InvalidIdentifierException e) {
                e.printStackTrace();
                SAX.LOG.info("Failed to load for key {}", key);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        CompoundTag record = new CompoundTag();

        for (Block block : this.blockBreakRecord.keySet()) {
            Optional<RegistryKey<Block>> key = Registry.BLOCK.getKey(block);
            key.ifPresent(blockRegistryKey -> record.putInt(blockRegistryKey.getValue().toString(), this.blockBreakRecord.get(block)));
        }

        compoundTag.put("record", record);

        return compoundTag;
    }

    public static NaturalBreakRecord of(ServerPlayerEntity serverPlayerEntity) {
        return SAX.NATURAL_BREAKS.get(serverPlayerEntity);
    }
}
