package com.fallenreaper.createutilities.core.utils;

import com.google.common.primitives.Ints;
import com.jozufozu.flywheel.repack.joml.Vector3d;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
@SuppressWarnings("ALL")
public class MathUtil {
    /**
     * https://en.wikipedia.org/wiki/B%C3%A9zier_curve
     */
    Couple<Vec3> starts;
    Couple<Vec3> ends;

    public static Vec3 quadraticBezierCurve(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        Vec3 lerpValue1 = lerpVector(p0, p1, t);
        Vec3 lerpValue2 = lerpVector(p1, p2, t);
        return lerpVector(lerpValue1, lerpValue2, t);
    }

    public static Vec3 lerpVector(Vec3 from, Vec3 to, float value) {
        Vec3 result = from.add(to.subtract(from).scale(value));
        return result;
    }

    public static int getIndexOf(int[] arr, int t) {


        return Ints.lastIndexOf(arr, t);

    }

    public static double lerp(double start, double target, float increment) {

        return start + ((target - start) * increment);
    }

    private static double getAngleForFacing(Direction facing) {
        return 90 * (facing.equals(Direction.NORTH) ? 4 : facing.equals(Direction.SOUTH) ? 2 : facing.equals(Direction.EAST) ? 3 : 1);
    }

    public static void rotateCenteredInDirection(SuperByteBuffer model, Direction direction, Direction facing) {
        model.rotateCentered(direction, (float) Math.toRadians(getAngleForFacing(facing)));
    }

    /**
     * https://en.wikipedia.org/wiki/B%C3%A9zier_curve
     */
    public static Vec3 cubicBezierCurve(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        //main points lerp
        Vec3 lerpValue1 = lerpVector(p0, p1, t);
        Vec3 lerpValue2 = lerpVector(p1, p2, t);
        Vec3 lerpValue3 = lerpVector(p2, p3, t);
        //inner lines lerp
        Vec3 lerpValue4 = lerpVector(lerpValue1, lerpValue2, t);
        Vec3 lerpValue5 = lerpVector(lerpValue2, lerpValue3, t);
        Vec3 cubicLerp = lerpVector(lerpValue4, lerpValue5, t);
        return cubicLerp;
    }

    public Vec3 getPositions(double t) {

        return lerpVector(starts.getFirst(), starts.getSecond(), (float) t);
    }

    public static boolean isInsideCircle(double radius, BlockPos blockPos, BlockPos target) {
        Vector3d centerPos = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Vector3d targetPos = new Vector3d(target.getX(), target.getY(), target.getZ());
        double distance = (int) centerPos.distance(targetPos);

        return distance <= radius;

    }
    public static boolean isInsideCircleHorizontal(double radius, BlockPos blockPos, BlockPos target) {
        Vector3d centerPos = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Vector3d targetPos = new Vector3d(target.getX(), target.getY(), target.getZ());
        double distance = (int) centerPos.distance(targetPos);

        return distance <= radius;

    }
    public static boolean isInsideCircle(double radius, Vector3d center, Vector3d target) {
        double distance = center.distance(target);
        return distance <= radius;
    }

    public static String formatTime(int ticks) {
        String base = "";
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        int days = hours / 24;
        int weeks = days / 7;

        seconds = seconds % 60;
        minutes = minutes % 60;
        hours = hours % 60;

        if (weeks > 0)
            base += weeks + "w ";

        if (days > 0)
            base += days + "d ";

        if (hours > 0)
            base += hours + "h ";

        if (minutes > 0)
            base += minutes + "m ";

        base += seconds + "s";
        return base;

    }

    public static String formatEnumName(Enum<?> target) {
        var stateString = target.name();
        stateString = stateString.substring(0, 1).toUpperCase() + stateString.substring(1).toLowerCase();
        return stateString;
    }

    public static int sortLowest(int[] to) {
        int base;
        int max = to[1];
        for (int i = 0; i < to.length; i++)
            if (to[i] < max)
                max = to[i];
        return max;
    }
}
