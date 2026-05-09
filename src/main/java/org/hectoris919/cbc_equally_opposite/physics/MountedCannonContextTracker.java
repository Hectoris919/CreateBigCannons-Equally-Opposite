package org.hectoris919.cbc_equally_opposite.physics;

import org.hectoris919.cbc_equally_opposite.compat.goingballistic.GoingBallisticAccess;

public final class MountedCannonContextTracker {
	private static final ThreadLocal<Double> BARREL_LENGTH = new ThreadLocal<>();

	private MountedCannonContextTracker() { }

	public static void enter(Object mountedCannonContraption) {
		double barrelLength = GoingBallisticAccess.estimateMountedBarrelLengthBlocks(
				mountedCannonContraption
		);
		if (Double.isFinite(barrelLength) && barrelLength > 0.0D) BARREL_LENGTH.set(barrelLength);
	}

	public static void exit() {
		BARREL_LENGTH.remove();
	}

	public static Double currentBarrelLengthBlocks() {
		return BARREL_LENGTH.get();
	}
}
