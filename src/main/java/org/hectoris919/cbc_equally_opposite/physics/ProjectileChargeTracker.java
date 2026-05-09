package org.hectoris919.cbc_equally_opposite.physics;

import net.minecraft.world.entity.Entity;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.WeakHashMap;

public final class ProjectileChargeTracker {
	private static final Map<Entity, Float> CHARGE_POWER_BY_PROJECTILE = Collections.synchronizedMap(new WeakHashMap<>());

	private ProjectileChargeTracker() { }

	public static void record(Entity projectile, float chargePower) {
		if (Float.isFinite(chargePower)) CHARGE_POWER_BY_PROJECTILE.put(projectile, chargePower);
	}
	public static OptionalDouble getAndClear(Entity projectile) {
		Float value = CHARGE_POWER_BY_PROJECTILE.remove(projectile);
		return value == null
				? OptionalDouble.empty()
				: OptionalDouble.of(value.doubleValue());
	}
}
