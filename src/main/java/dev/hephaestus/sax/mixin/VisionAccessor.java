package dev.hephaestus.sax.mixin;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlock.AbstractBlockState.class)
public interface VisionAccessor {
    @Accessor
    AbstractBlock.ContextPredicate getBlockVisionPredicate();
}
