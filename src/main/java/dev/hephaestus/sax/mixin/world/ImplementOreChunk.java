package dev.hephaestus.sax.mixin.world;

import dev.hephaestus.sax.server.Config;
import dev.hephaestus.sax.util.ListView;
import dev.hephaestus.sax.util.OreChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(WorldChunk.class)
public abstract class ImplementOreChunk implements OreChunk {
    @Shadow @Final private ChunkSection[] sections;
    @Shadow @Final private ChunkPos pos;

    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Unique private List<BlockPos> obscuredBlockList;
    @Unique private Set<BlockPos> obscuredBlockSet;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/biome/source/BiomeArray;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/TickScheduler;Lnet/minecraft/world/TickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Ljava/util/function/Consumer;)V", at = @At("TAIL"))
    private void initializeObscureBlocks(World world, ChunkPos pos, BiomeArray biomes, UpgradeData upgradeData, TickScheduler<Block> blockTickScheduler, TickScheduler<Fluid> fluidTickScheduler, long inhabitedTime, ChunkSection[] sections, Consumer<WorldChunk> loadToWorldConsumer, CallbackInfo ci) {
        this.obscuredBlockList = new ArrayList<>();
        this.obscuredBlockSet = new HashSet<>();
        this.sax_init();
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void removeOldBlocks(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (obscuredBlockSet.contains(pos) && !Config.HIDDEN.containsKey(state.getBlock())) {
            this.obscuredBlockList.remove(pos);
            this.obscuredBlockSet.remove(pos);
        } else if (!obscuredBlockSet.contains(pos) && Config.HIDDEN.containsKey(state.getBlock())) {
            this.obscuredBlockList.add(pos);
            this.obscuredBlockSet.add(pos);
        }
    }

    @Override
    public void sax_init() {
        this.obscuredBlockList.clear();
        this.obscuredBlockSet.clear();

        final BlockPos start = this.pos.getStartPos();

        for (int i = 0; i < this.sections.length; ++i) {
            ChunkSection section = this.sections[i];
            if (section != null && !section.isEmpty()) {
                for (int x = 0; x < 16; ++x) {
                    for (int y = 0; y <16; ++y) {
                        for (int z = 0; z < 16; ++z) {
                            if (Config.HIDDEN.containsKey(section.getBlockState(x, y, z).getBlock())) {
                                BlockPos pos = new BlockPos(
                                        start.getX() + x,
                                        start.getY() + 16 * i + y,
                                        start.getZ() + z);

                                this.obscuredBlockList.add(pos);
                                this.obscuredBlockSet.add(pos);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<BlockPos> sax_getObfuscatedBlocks() {
        return this.obscuredBlockList;
    }

    @Override
    public Block sax_getBlock(BlockPos pos) {
        return this.getBlockState(pos).getBlock();
    }
}
