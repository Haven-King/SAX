package dev.hephaestus.sax.mixin.server;

import dev.hephaestus.sax.world.level.SoftBanManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class RemindOperatorsAboutBans {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void remindOperatorsAboutBans(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        SoftBanManager softBanManager = SoftBanManager.of((WorldProperties) this.server.getSaveProperties());

        if (softBanManager.isBanned(player)) {
            softBanManager.broadcastBanMessageToOperators(player);
        }
    }
}
