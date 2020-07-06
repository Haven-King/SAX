package dev.hephaestus.sax.mixin.world;

import dev.hephaestus.sax.world.OreCountRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.decorator.CountDepthDecoratorConfig;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultBiomeFeatures.class)
public class OreCountCapture {
    @Unique private static Block SETTING = null;
    @Unique private static Block BLOCK = null;
    @Unique private static int SIZE = 0;

    @Redirect(method = "addDefaultOres", at = @At(value = "NEW", target = "net/minecraft/world/gen/feature/OreFeatureConfig"))
    private static OreFeatureConfig captureDefaultOreVeinTypeAndSize(OreFeatureConfig.Target target, BlockState state, int size) {
        switch (target) {
            case NATURAL_STONE:
                SETTING = Blocks.STONE;
                break;

            case NETHER_ORE_REPLACEABLES:
            case NETHERRACK:
                SETTING = Blocks.NETHERRACK;
        }

        BLOCK = state.getBlock();
        SIZE = size;

        return new OreFeatureConfig(target, state, size);
    }

    @Redirect(method = "addDefaultOres", at = @At(value = "NEW", target = "net/minecraft/world/gen/decorator/RangeDecoratorConfig"))
    private static RangeDecoratorConfig captureDefaultOreVeinNumber(int count, int bottomOffset, int topOffset, int maximum) {
        OreCountRegistry.put(DimensionType.OVERWORLD_REGISTRY_KEY, SETTING, BLOCK, count * SIZE);
        OreCountRegistry.put(DimensionType.OVERWORLD_CAVES_REGISTRY_KEY, SETTING, BLOCK, count * SIZE);
        return new RangeDecoratorConfig(count, bottomOffset, topOffset, maximum);
    }

    @Redirect(method = "addDefaultOres", at = @At(value = "NEW", target = "net/minecraft/world/gen/decorator/CountDepthDecoratorConfig"))
    private static CountDepthDecoratorConfig captureLapisOreVeinNumber(int count, int baseline, int spread) {
        OreCountRegistry.put(DimensionType.OVERWORLD_REGISTRY_KEY, SETTING, BLOCK, count * SIZE);
        OreCountRegistry.put(DimensionType.OVERWORLD_CAVES_REGISTRY_KEY, SETTING, BLOCK, count * SIZE);
        return new CountDepthDecoratorConfig(count, baseline, spread);
    }
}
