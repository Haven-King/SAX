package dev.hephaestus.sax.mixin.server;

import dev.hephaestus.sax.world.level.SoftBanManager;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ServerPlayerEntity.class)
public abstract class DeObfuscateTargetedBlock {
    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Shadow public abstract boolean isDisconnected();

    @Unique private BlockPos targetedBlock = null;

    private static final int EXPOSE_RADIUS = 3;

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void updateTargetedBlock(CallbackInfo ci) {
        if (!this.isDisconnected() && SoftBanManager.of(this.getServerWorld().getLevelProperties()).isBanned((ServerPlayerEntity) (Object) this)) {
            BlockHitResult hitResult = traceForBlock((ServerPlayerEntity) (Object) this, 5);

            if (hitResult != null && hitResult.getBlockPos() != this.targetedBlock) {
                // Send them the fibbed version of the block they were previously looking at.
                if (this.targetedBlock != null) {
                    this.sendAroundTargetedBlocks(this.networkHandler::sendPacket);
                }

                // Send them the non-fibbed version of the block they're looking at.
                this.targetedBlock = hitResult.getBlockPos();
                this.sendAroundTargetedBlocks(this.networkHandler.connection::send);
            }
        }
    }

    private void sendAroundTargetedBlocks(Consumer<BlockUpdateS2CPacket> consumer) {
//        for (int x = -EXPOSE_RADIUS; x <= EXPOSE_RADIUS; ++x) {
//            for (int y = -EXPOSE_RADIUS; y <= EXPOSE_RADIUS; ++y) {
//                for (int z = -EXPOSE_RADIUS; z <= EXPOSE_RADIUS; ++z) {
                    consumer.accept(new BlockUpdateS2CPacket(this.getServerWorld(), this.targetedBlock));
//                }
//            }
//        }
    }

    private static BlockHitResult traceForBlock(ServerPlayerEntity player, double maxViewDistance) {
        Vec3d angle = player.getRotationVec(1F);
        return player.world.rayTrace(new RayTraceContext(
                player.getCameraPosVec(1F),
                player.getCameraPosVec(1F).add(maxViewDistance * angle.x, maxViewDistance * angle.y, maxViewDistance * angle.z),
                RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, player
        ));
    }
}
