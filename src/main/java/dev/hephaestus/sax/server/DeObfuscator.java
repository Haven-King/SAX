package dev.hephaestus.sax.server;

import dev.hephaestus.sax.util.ListView;
import dev.hephaestus.sax.util.OreChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
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

                int chunkX = this.player.chunkX;
                int chunkZ = this.player.chunkZ;

                ServerWorld world = this.player.getServerWorld();

                for (int x = chunkX - Config.CHUNK_RADIUS; x < chunkX + Config.CHUNK_RADIUS; ++x) {
                    for (int z = chunkZ - Config.CHUNK_RADIUS; z < chunkZ  + Config.CHUNK_RADIUS; ++z) {
                        OreChunk chunk = (OreChunk) world.getChunk(x, z);
                        ListView<BlockPos> positions = chunk.sax_getObfuscatedBlocks();

                        for (int i = 0; i < positions.size(); ++i) {
                            BlockPos pos = positions.get(i);
                            Block block = chunk.getBlock(pos);

                            if (Config.HIDDEN.containsKey(block) && this.traceForBlock(origin, pos)) {
                                this.sendBlockUpdate(pos, this.player.networkHandler.connection::send);
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
