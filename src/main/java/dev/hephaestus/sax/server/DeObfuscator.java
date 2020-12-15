package dev.hephaestus.sax.server;

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
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static dev.hephaestus.sax.server.Config.SEARCH_RADIUS;

public class DeObfuscator {
    private final ServerPlayerEntity player;
    private final HashSet<BlockPos> revealed = new HashSet<>();
    private final BlockPos.Mutable mutable = new BlockPos.Mutable();
    private final RaycastContext raycastContext;
    private final ExecutorService executor;
    private final MutableBoolean finished = new MutableBoolean(true);

    public DeObfuscator(ServerPlayerEntity player) {
        this.player = player;
        this.raycastContext = new RaycastContext(
                Vec3d.ZERO, Vec3d.ZERO, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, player
        );

        executor = Executors.newSingleThreadExecutor((runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void tick() {
        if (finished.booleanValue()) {
            executor.submit(() -> {
                finished.setFalse();
                Vec3d origin = this.raycastContext.start = this.player.getCameraPosVec(1F);

                this.revealed.removeIf(pos -> !pos.isWithinDistance(origin, SEARCH_RADIUS));

                for (byte x = (byte) -SEARCH_RADIUS; x <= SEARCH_RADIUS; ++x) {
                    for (byte y = (byte) -SEARCH_RADIUS; y <= SEARCH_RADIUS; ++y) {
                        for (byte z = (byte) -SEARCH_RADIUS; z <= SEARCH_RADIUS; ++z) {
                            if (x * x + y * y + z * z <= SEARCH_RADIUS * SEARCH_RADIUS) {
                                mutable.set(origin.x, origin.y, origin.z);
                                mutable.move(x, y, z);

                                if (Config.HIDDEN.containsKey(this.player.world.getBlockState(mutable).getBlock())) {
                                    if (!this.revealed.contains(mutable)) {
                                        if (this.traceForBlock(origin, mutable)) {
                                            BlockPos pos = mutable.toImmutable();
                                            this.revealed.add(pos);
                                            this.sendBlockUpdate(pos, this.player.networkHandler.connection::send);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                finished.setTrue();
            });
        }
    }

    private void sendBlockUpdate(BlockPos pos, Consumer<BlockUpdateS2CPacket> consumer) {
        if (this.player.getServer() != null) {
            this.player.getServer().execute(() -> consumer.accept(new BlockUpdateS2CPacket(this.player.world, pos)));
        }
    }

    private boolean traceForBlock(Vec3d origin, Vec3i target) {
        Vec3d pos = new Vec3d(origin.x, origin.y, origin.z);

        for (byte dX = 0; dX <= 1; ++dX) {
            for (byte dY = 0; dY <= 1; ++dY) {
                for (byte dZ = 0; dZ <= 1; ++dZ) {
                    // We want to avoid creating a ton of new vectors.
                    pos.x = target.getX() + dX;
                    pos.y = target.getY() + dY;
                    pos.z = target.getZ() + dZ;

                    // Also avoiding making new RaycastContext's.
                    this.raycastContext.end = pos;

                    BlockHitResult hitResult = rayTrace(this.raycastContext);

                    if (hitResult.getPos().isInRange(pos, 0.5F)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private BlockHitResult rayTrace(RaycastContext context) {
        BlockView blockView = this.player.world;
        return BlockView.raycast(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = blockView.getBlockState(blockPos);
            Vec3d start = rayTraceContext.getStart();
            Vec3d end = rayTraceContext.getEnd();
            VoxelShape voxelShape = rayTraceContext.getBlockShape(blockState, blockView, blockPos);
            return blockView.raycastBlock(start, end, blockPos, voxelShape, blockState);
        }, (rayTraceContext) -> {
            Vec3d vec3d = rayTraceContext.getStart().subtract(rayTraceContext.getEnd());
            return BlockHitResult.createMissed(rayTraceContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(rayTraceContext.getEnd()));
        });
    }
}
