package dev.hephaestus.sax.util;

import dev.hephaestus.sax.mixin.VisionAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;

public class FastCaster {
    private static final ThreadLocal<BlockPos.Mutable> MUTABLE = ThreadLocal.withInitial(BlockPos.Mutable::new);

    private FastCaster() {
    }

    public static boolean fastcast(ServerWorld blockView, Vec3d startPos, Vec3d endPos, Vec3i target) {
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
                BlockView chunk = blockView.getChunkManager().getChunk(x >> 4, z >> 4);

                if (chunk != null) {
                    blockState = chunk.getBlockState(mut.set(x, y, z));

                    if (((VisionAccessor) blockState).getBlockVisionPredicate().test(blockState, blockView, mut)) {
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
}
