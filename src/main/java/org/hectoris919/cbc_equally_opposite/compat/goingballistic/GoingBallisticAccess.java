package org.hectoris919.cbc_equally_opposite.compat.goingballistic;

import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import net.minecraft.world.entity.Entity;
import java.lang.reflect.Method;

public final class GoingBallisticAccess {
	private static final String HELPER_CLASS = "org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper";
	private static final String PARAMETER_CLASS = "org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry";

	private static boolean initialized;
	private static boolean available;

	private static Method getProjectileMassWithFallback;
	private static Method getProjectileMassProperties;
	private static Method cannonPowderMass;
	private static Method autocannonPowderMass;
	private static Method estimateMountedBarrelLengthMeters;

	private GoingBallisticAccess() { }

	public static boolean isAvailable() {
		init();
		return available;
	}

	public static double getProjectileMass(Entity projectile, double fallbackMass) {
		init();

		if (!available || getProjectileMassWithFallback == null) return fallbackMass;

		try {
			Object value = getProjectileMassWithFallback.invoke(null, projectile, fallbackMass);
			return value instanceof Number number
					? number.doubleValue()
					: fallbackMass;
		} catch (Throwable throwable) {
			logDebugFailure("getProjectileMass", throwable);
			return fallbackMass;
		}
	}

	public static double cannonPowderMass(double fallback) {
		init();

		if (!available || cannonPowderMass == null) return fallback;

		try {
			Object value = cannonPowderMass.invoke(null);
			return value instanceof Number number
					? number.doubleValue()
					: fallback;
		} catch (Throwable throwable) {
			logDebugFailure("cannonPowderMass", throwable);
			return fallback;
		}
	}

	public static double autocannonPowderMass(Entity projectile, double fallback) {
		init();

		if (!available) return fallback;

		try {
			Object properties = getProjectileMassProperties.invoke(null, projectile);
			if (properties != null) {
				Method method = properties.getClass().getMethod("autocannonPowderMassOr", double.class);
				Object value = method.invoke(properties, fallback);
				if (value instanceof Number number) return number.doubleValue();
			}
		} catch (Throwable throwable) {
			logDebugFailure("autocannonProjectileMassProperties", throwable);
		}

		if (autocannonPowderMass == null) return fallback;

		try {
			Object value = autocannonPowderMass.invoke(null);
			return value instanceof Number number
					? number.doubleValue()
					: fallback;
		} catch (Throwable throwable) {
			logDebugFailure("autocannonPowderMass", throwable);
			return fallback;
		}
	}

	public static double estimateMountedBarrelLengthBlocks(Object mountedCannonContraption) {
		init();

		if (!available || estimateMountedBarrelLengthMeters == null || mountedCannonContraption == null) return 1.0D;

		try {
			Object value = estimateMountedBarrelLengthMeters.invoke(null, mountedCannonContraption);
			return value instanceof Number number
					? number.doubleValue()
					: 1.0D;
		} catch (Throwable throwable) {
			logDebugFailure("estimateMountedBarrelLengthMeters", throwable);
			return 1.0D;
		}
	}

	private static void init() {
		if (initialized) return;

		initialized = true;

		try {
			Class<?> helperClass = Class.forName(HELPER_CLASS);
			Class<?> parameterClass = Class.forName(PARAMETER_CLASS);

			getProjectileMassWithFallback = helperClass.getMethod("getProjectileMass", Entity.class, double.class);
			getProjectileMassProperties = helperClass.getMethod("getProjectileMassProperties", Entity.class);
			cannonPowderMass = parameterClass.getMethod("cannonPowderMass");
			autocannonPowderMass = parameterClass.getMethod("autocannonPowderMass");
			try {
				Class<?> mountedContraptionClass = Class.forName("rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption");
				estimateMountedBarrelLengthMeters = helperClass.getMethod("estimateMountedBarrelLengthMeters", mountedContraptionClass);
			} catch (Throwable throwable) {
				estimateMountedBarrelLengthMeters = null;
			}

			available = true;
		} catch (Throwable throwable) {
			available = false;
		}
	}

	private static void logDebugFailure(String operation, Throwable throwable) {
		if (org.hectoris919.cbc_equally_opposite.Config.debugImpulses()) EquallyOpposite.LOGGER.debug("Going Ballistic reflection call failed: {}", operation, throwable);
	}
}
