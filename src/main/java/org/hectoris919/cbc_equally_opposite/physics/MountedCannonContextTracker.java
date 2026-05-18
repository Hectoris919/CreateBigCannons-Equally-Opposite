package org.hectoris919.cbc_equally_opposite.physics;

import org.hectoris919.cbc_equally_opposite.compat.goingballistic.GoingBallisticAccess;
import net.minecraft.world.phys.Vec3;

public final class MountedCannonContextTracker {
	private static final ThreadLocal<Double> BARREL_LENGTH = new ThreadLocal<>();
	private static final ThreadLocal<Vec3> MUZZLE_END_POSITION = new ThreadLocal<>();

	private MountedCannonContextTracker() { }

	public static void enter(Object mountedCannonContraption) {
		enter(mountedCannonContraption, null);
	}

	public static void enter(Object mountedCannonContraption, Vec3 muzzleEndPosition) {
		double barrelLength = GoingBallisticAccess.estimateMountedBarrelLengthBlocks(
				mountedCannonContraption
		);
		if (Double.isFinite(barrelLength) && barrelLength > 0.0D) BARREL_LENGTH.set(barrelLength);
		if (muzzleEndPosition != null && ImpulseMath.isFinite(muzzleEndPosition)) MUZZLE_END_POSITION.set(muzzleEndPosition);
	}

	public static void exit() {
		BARREL_LENGTH.remove();
		MUZZLE_END_POSITION.remove();
	}

	public static Double currentBarrelLengthBlocks() {
		return BARREL_LENGTH.get();
	}

	public static Vec3 currentMuzzleEndPosition() {
		return MUZZLE_END_POSITION.get();
	}
}
