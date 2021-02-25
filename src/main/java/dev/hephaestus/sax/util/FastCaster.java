package dev.hephaestus.sax.util;

import dev.hephaestus.sax.mixin.VisionAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FastCaster {
    private static final Map<RegistryKey<World>, Map<Integer, Map<Integer, WorldChunk>>> CHUNKS = new HashMap<>();

    private static final ThreadLocal<BlockPos.Mutable> MUTABLE = ThreadLocal.withInitial(BlockPos.Mutable::new);

    private FastCaster() {
    }

    public static boolean fastcast(ServerWorld world, Vec3d startPos, Vec3d endPos, Vec3i target) {
        if (startPos.equals(endPos)) {
            return true;
        } else {
            double startX = MathHelper.lerp(-1.0E-7D, endPos.x, startPos.x);
            double startY = MathHelper.lerp(-1.0E-7D, endPos.y, startPos.y);
            double startZ = MathHelper.lerp(-1.0E-7D, endPos.z, startPos.z);
            double endX = MathHelper.lerp(-1.0E-7D, startPos.x, endPos.x);
            double endY = MathHelper.lerp(-1.0E-7D, startPos.y, endPos.y);
            double endZ = MathHelper.lerp(-1.0E-7D, startPos.z, endPos.z);
            int x = MathHelper.floor(endX);
            int y = MathHelper.floor(endY);
            int z = MathHelper.floor(endZ);

            BlockPos.Mutable mut = MUTABLE.get();
            mut.set(x, y, z);

            double xDistance = startX - endX;
            double yDistance = startY - endY;
            double zDistance = startZ - endZ;
            int xDirection = MathHelper.sign(xDistance);
            int yDirection = MathHelper.sign(yDistance);
            int zDirection = MathHelper.sign(zDistance);
            double s = xDirection == 0 ? 1.7976931348623157E308D : (double)xDirection / xDistance;
            double t = yDirection == 0 ? 1.7976931348623157E308D : (double)yDirection / yDistance;
            double u = zDirection == 0 ? 1.7976931348623157E308D : (double)zDirection / zDistance;
            double dX = s * (xDirection > 0 ? 1.0D - MathHelper.fractionalPart(endX) : MathHelper.fractionalPart(endX));
            double dY = t * (yDirection > 0 ? 1.0D - MathHelper.fractionalPart(endY) : MathHelper.fractionalPart(endY));
            double dZ = u * (zDirection > 0 ? 1.0D - MathHelper.fractionalPart(endZ) : MathHelper.fractionalPart(endZ));

            BlockState blockState;
            while (dX <= 1D || dY <= 1D || dZ <= 1D) {
                WorldChunk chunk = getChunk(world, x >> 4, z >> 4);

                if (chunk != null) {
                    blockState = getBlockState(chunk, mut.set(x, y, z));

                    if (((VisionAccessor) blockState).getBlockVisionPredicate().test(blockState, world, mut)) {
                        break;
                    }
                }

                if (dX < dY) {
                    if (dX < dZ) {
                        x += xDirection;
                        dX += s;
                    } else {
                        z += zDirection;
                        dZ += u;
                    }
                } else if (dY < dZ) {
                    y += yDirection;
                    dY += t;
                } else {
                    z += zDirection;
                    dZ += u;
                }
            }

            double x1 = x + 0.5;
            double y1 = y + 0.5;
            double z1 = z + 0.5;

            double x2 = target.getX() + 0.5;
            double y2 = target.getY() + 0.5;
            double z2 = target.getZ() + 0.5;

            double distance = 1;

            return Math.abs(x1 - x2) < distance && Math.abs(y1 - y2) < distance && Math.abs(z1 - z2) < distance;
        }
    }

    private static BlockState getBlockState(WorldChunk chunk, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        ChunkSection[] sections = chunk.getSectionArray();

        if (y >= 0 && y >> 4 < sections.length) {
            ChunkSection chunkSection = sections[y >> 4];
            if (!ChunkSection.isEmpty(chunkSection)) {
                return chunkSection.getBlockState(x & 15, y & 15, z & 15);
            }
        }

        return Blocks.AIR.getDefaultState();
    }

    private static @Nullable WorldChunk getChunk(ServerWorld world, int x, int z) {
        return CHUNKS.getOrDefault(world.getRegistryKey(), Collections.emptyMap())
                .getOrDefault(x, Collections.emptyMap())
                .get(z);
    }

    public static void load(ServerWorld world, WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        CHUNKS.computeIfAbsent(world.getRegistryKey(), key -> new HashMap<>())
                .computeIfAbsent(pos.x, x -> new HashMap<>())
                .putIfAbsent(pos.z, chunk);
    }

    public static void unload(ServerWorld world, WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        CHUNKS.computeIfAbsent(world.getRegistryKey(), key -> new HashMap<>())
                .computeIfAbsent(pos.x, x -> new HashMap<>())
                .remove(pos.z);
    }
}
