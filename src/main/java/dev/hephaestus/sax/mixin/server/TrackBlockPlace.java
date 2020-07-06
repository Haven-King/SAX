package dev.hephaestus.sax.mixin.server;

import dev.hephaestus.sax.component.BlockAudit;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class TrackBlockPlace {
    @Shadow @Final private Block block;

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void trackBlockPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue().isAccepted() && context.getPlayer() != null) {
            BlockPos pos = context.getBlockPos();
            BlockAudit auditedChunk = BlockAudit.of(context.getWorld().getChunk(pos));
            auditedChunk.put(context.getPlayer().getUuid(), pos, this.block);
        }
    }
}
