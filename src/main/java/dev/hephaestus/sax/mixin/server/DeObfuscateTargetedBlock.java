package dev.hephaestus.sax.mixin.server;

import com.mojang.authlib.GameProfile;
import dev.hephaestus.sax.server.DeObfuscator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class DeObfuscateTargetedBlock {
    @Unique private DeObfuscator deObfuscator;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeDeObfuscator(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager interactionManager, CallbackInfo ci) {
        this.deObfuscator = new DeObfuscator((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void tickDeObfuscator(CallbackInfo ci) {
        this.deObfuscator.tick();
    }
}
