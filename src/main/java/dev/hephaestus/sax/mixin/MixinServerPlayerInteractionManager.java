package dev.hephaestus.sax.mixin;

import dev.hephaestus.fiblib.impl.FibLib;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "changeGameMode", at = @At("RETURN"))
    public void resendChunks(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        FibLib.resendChunks(this.player);
    }
}
