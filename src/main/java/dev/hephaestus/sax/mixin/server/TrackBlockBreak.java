package dev.hephaestus.sax.mixin.server;

import dev.hephaestus.sax.component.BlockAudit;
import dev.hephaestus.sax.component.NaturalBreakRecord;
import dev.hephaestus.sax.world.level.SoftBanManager;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public class TrackBlockBreak {
    @Shadow public ServerPlayerEntity player;

    @Shadow public ServerWorld world;

    @Inject(method = "tryBreakBlock", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void trackBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir, BlockState state) {
        if (cir.getReturnValue()) {
            BlockAudit auditedChunk = BlockAudit.of(this.world.getChunk(pos));
            boolean isNatural = auditedChunk.getPlacer(pos) == null;

            if (isNatural) {
                NaturalBreakRecord.of(this.player).onBreak(state.getBlock());
                SoftBanManager.of(this.player.getServerWorld().getLevelProperties()).banIfNecessary(this.player);
            }

            auditedChunk.put(this.player.getUuid(), pos, state.getBlock());
        }
    }
}
