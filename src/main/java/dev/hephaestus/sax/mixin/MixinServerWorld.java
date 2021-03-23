package dev.hephaestus.sax.mixin;

import dev.hephaestus.sax.server.DeObfuscator;
import dev.hephaestus.sax.util.ObfuscatedWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerWorld.class)
public class MixinServerWorld implements ObfuscatedWorld {
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Unique private final Map<UUID, DeObfuscator> deObfuscatorMap = new HashMap<>();

    @Inject(method = "addPlayer", at = @At("TAIL"))
    private void addDeobfuscator(ServerPlayerEntity player, CallbackInfo ci) {
        this.deObfuscatorMap.put(player.getUuid(), new DeObfuscator(player));
    }

    @Inject(method = "removePlayer", at = @At("TAIL"))
    private void removeDeobfuscator(ServerPlayerEntity player, CallbackInfo ci) {
        this.deObfuscatorMap.remove(player.getUuid()).remove();
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void tickDeObfuscator(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity) {
            this.deObfuscatorMap.get(entity.getUuid()).tick();
        }
    }

    @Override
    public void reset() {
        this.deObfuscatorMap.clear();

        for (ServerPlayerEntity player : this.players) {
            this.deObfuscatorMap.put(player.getUuid(), new DeObfuscator(player));
        }
    }
}
