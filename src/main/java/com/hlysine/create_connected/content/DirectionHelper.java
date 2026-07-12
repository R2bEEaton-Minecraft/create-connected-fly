package com.hlysine.create_connected.content;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

// Direction.fromDelta(int, int, int) is gone entirely (confirmed via javap - no direct replacement,
// unlike Direction.getNearest(int, int, int, Direction), which gained a required fallback param and
// always returns *something*, making it unsuitable here: every call site below specifically needs
// to distinguish "this delta is exactly one axis-aligned step" from "it isn't" (a diagonal/multi-step
// offset), which getNearest can no longer express since it never returns null).
public class DirectionHelper {
    public static Direction fromDelta(Vec3i delta) {
        return fromDelta(delta.getX(), delta.getY(), delta.getZ());
    }

    public static Direction fromDelta(int x, int y, int z) {
        if (Math.abs(x) + Math.abs(y) + Math.abs(z) != 1)
            return null;
        for (Direction direction : Direction.values())
            if (direction.getStepX() == x && direction.getStepY() == y && direction.getStepZ() == z)
                return direction;
        return null;
    }
}
