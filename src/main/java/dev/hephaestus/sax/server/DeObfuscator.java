package dev.hephaestus.sax.server;

import dev.hephaestus.sax.SAX;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;

import java.util.HashSet;
import java.util.function.Consumer;

public class DeObfuscator {
    private static final byte SEARCH_RADIUS = 30;

    private final ServerPlayerEntity player;
    private final HashSet<Vec3i> revealed = new HashSet<>();

    public DeObfuscator(ServerPlayerEntity player) {
        this.player = player;
    }

    public void tick() {
        Vec3d origin = this.player.getCameraPosVec(1F);

        for (Vec3i pos : this.revealed) {
            if (!pos.isWithinDistance(origin, SEARCH_RADIUS)) {
                this.sendBlockUpdate(new BlockPos(pos), this.player.networkHandler::sendPacket);
            }
        }

        this.revealed.removeIf(pos -> !pos.isWithinDistance(origin, SEARCH_RADIUS));
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int i = 0;
        for (byte x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; ++x) {
            for (byte y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; ++y) {
                for (byte z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; ++z) {
                    if (x * x + y * y + z * z <= SEARCH_RADIUS * SEARCH_RADIUS) {
                        ++i;
                        mutable.set(origin.x, origin.y, origin.z);
                        mutable.move(x, y, z);

                        if (Config.HIDDEN.containsKey(this.player.world.getBlockState(mutable).getBlock())) {
                            Vec3i pos = mutable.toImmutable();

                            if (!this.revealed.contains(pos)) {
                                if (this.traceForBlock(this.player, pos)) {
                                    this.revealed.add(pos);
                                    this.sendBlockUpdate(new BlockPos(pos), this.player.networkHandler.connection::send);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendBlockUpdate(BlockPos pos, Consumer<BlockUpdateS2CPacket> consumer) {
        if (this.player.getServer() != null) {
            this.player.getServer().execute(() -> consumer.accept(new BlockUpdateS2CPacket(this.player.world, pos)));
        }
    }

    private boolean traceForBlock(ServerPlayerEntity player, Vec3i target) {
        for (byte dX = 0; dX <= 1; ++dX) {
            for (byte dY = 0; dY <= 1; ++dY) {
                for (byte dZ = 0; dZ <= 1; ++dZ) {
                    Vec3d pos = new Vec3d(target.getX() + dX, target.getY() + dY, target.getZ() + dZ);
                        RayTraceContext context = new RayTraceContext(
                                player.getCameraPosVec(1F),
                                pos,
                                RayTraceContext.ShapeType.VISUAL, RayTraceContext.FluidHandling.NONE, player
                        );

                        BlockHitResult hitResult = rayTrace(context);

                        if (hitResult.getPos().isInRange(pos, 0.5F)) {
                            return true;
                        }
                }
            }
        }

        return false;
    }

    private BlockHitResult rayTrace(RayTraceContext context) {
        BlockView blockView = this.player.world;
        return BlockView.rayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = blockView.getBlockState(blockPos);
            Vec3d vec3d = rayTraceContext.getStart();
            Vec3d vec3d2 = rayTraceContext.getEnd();
            VoxelShape voxelShape = rayTraceContext.getBlockShape(blockState, blockView, blockPos);
            return blockView.rayTraceBlock(vec3d, vec3d2, blockPos, voxelShape, blockState);
        }, (rayTraceContext) -> {
            Vec3d vec3d = rayTraceContext.getStart().subtract(rayTraceContext.getEnd());
            return BlockHitResult.createMissed(rayTraceContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(rayTraceContext.getEnd()));
        });
    }
}
