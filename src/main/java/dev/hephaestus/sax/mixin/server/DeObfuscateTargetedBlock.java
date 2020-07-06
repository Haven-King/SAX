package dev.hephaestus.sax.mixin.server;

import com.mojang.authlib.GameProfile;
import dev.hephaestus.sax.SAX;
import dev.hephaestus.sax.world.OreCountRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.function.Consumer;

@Mixin(ServerPlayerEntity.class)
public abstract class DeObfuscateTargetedBlock extends PlayerEntity {
    public DeObfuscateTargetedBlock(World world, BlockPos blockPos, GameProfile gameProfile) {
        super(world, blockPos, gameProfile);
    }

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Shadow public abstract boolean isDisconnected();

    @Unique private BlockPos targetedBlock = null;

    private static final int SEARCH_RADIUS = 30;

    @Unique private final HashSet<BlockPos> revealed = new HashSet<>();

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void updateTargetedBlock(CallbackInfo ci) {
//        if (!this.isDisconnected() && SoftBanManager.of(this.getServerWorld().getLevelProperties()).isBanned((ServerPlayerEntity) (Object) this)) {
//            BlockHitResult hitResult = traceForBlock((ServerPlayerEntity) (Object) this, 5);
//
//            if (hitResult != null && hitResult.getBlockPos() != this.targetedBlock) {
//                // Send them the fibbed version of the block they were previously looking at.
//                if (this.targetedBlock != null) {
//                    this.sendAroundTargetedBlocks(this.networkHandler::sendPacket);
//                }
//
//                // Send them the non-fibbed version of the block they're looking at.
//                this.targetedBlock = hitResult.getBlockPos();
//                this.sendAroundTargetedBlocks(this.networkHandler.connection::send);
//            }
//        }

        BlockPos blockPos = new BlockPos(this.getCameraPosVec(1F));

        for (BlockPos pos : this.revealed) {
            if (!pos.isWithinDistance(blockPos, SEARCH_RADIUS)) {
                this.sendBlockUpdate(pos, this.networkHandler::sendPacket);
            }
        }

        this.revealed.removeIf(pos -> !pos.isWithinDistance(blockPos, SEARCH_RADIUS));
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; ++x) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; ++y) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; ++z) {
                    mutable.set(blockPos);
                    mutable.move(x, y, z);

                    if (mutable.isWithinDistance(blockPos, SEARCH_RADIUS) && OreCountRegistry.isTracked(this.world.getBlockState(mutable).getBlock())) {
                        BlockPos pos = mutable.toImmutable();

                        if (!this.revealed.contains(pos)) {
                            Vec3d blockCenter = new Vec3d(
                                    pos.getX() + 0.5 /*+ 0.5 * Integer.compare(x, 0)*/,
                                    pos.getY() + 0.5 /*+ 0.5 * Integer.compare(y, 0)*/,
                                    pos.getZ() + 0.5 /*+ 0.5 * Integer.compare(z, 0)*/
                            );
                            BlockHitResult hitResult = traceForBlock(
                                    (ServerPlayerEntity) (Object) this,
                                    blockCenter
                            );

                            if (hitResult != null && hitResult.getBlockPos().isWithinDistance(blockCenter, 1)) {
//                                SAX.LOG.info("HIT:  [{}, {}, {}]", x, y, z);
//                                SAX.LOG.info("[{}, {}, {}]", Integer.compare(x, 0), Integer.compare(y, 0), Integer.compare(z, 0));
                                this.revealed.add(pos);
                                this.sendBlockUpdate(pos, this.networkHandler.connection::send);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendBlockUpdate(BlockPos pos, Consumer<BlockUpdateS2CPacket> consumer) {
        consumer.accept(new BlockUpdateS2CPacket(this.world, pos));
    }

    private static BlockHitResult traceForBlock(ServerPlayerEntity player, Vec3d target) {
        RayTraceContext context = new RayTraceContext(
                player.getCameraPosVec(1F),
                target,
                RayTraceContext.ShapeType.VISUAL, RayTraceContext.FluidHandling.NONE, player
        );

        return player.world.rayTrace(context);
    }
}
