package org.hectoris919.cbc_equally_opposite.physics;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.hectoris919.cbc_equally_opposite.Config;

public final class ImpulseMath {
	private static final double MIN_VECTOR_LENGTH = 1.0e-9D;

	private ImpulseMath() { }

	public static double siImpulseToSable(double impulse) {
		return impulse * Config.impulseScale();
	}

	public static Vec3 siImpulseToSable(Vec3 impulse) {
		return impulse.scale(Config.impulseScale());
	}

	public static double bptToMps(double blocksPerTick) {
		return blocksPerTick * 20.0D;
	}

	public static double clamp(double value, double min, double max) {
		return Mth.clamp(value, min, max);
	}

	public static boolean isUsefulVector(Vec3 vector) {
		return isFinite(vector) && vector.lengthSqr() > MIN_VECTOR_LENGTH;
	}

	public static boolean isFinite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
