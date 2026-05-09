package org.hectoris919.cbc_equally_opposite.physics;

import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.compat.goingballistic.GoingBallisticAccess;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

public final class PhysicalProjectileMass {
	private PhysicalProjectileMass() { }

	public static double mass(AbstractCannonProjectile projectile, boolean autocannon) {
		double fallbackMass = autocannon
				? Config.baseAutocannonProjectileMass()
				: Config.baseBigCannonProjectileMass();
		return GoingBallisticAccess.getProjectileMass(projectile, fallbackMass);
	}

	public static double remainingMass(ImpactSnapshot snapshot, AbstractCannonProjectile projectile) {
		if (snapshot == null) return 0.0D;

		double cbcMassBefore = snapshot.cbcMassBefore();
		if (!Double.isFinite(cbcMassBefore) || cbcMassBefore <= 1.0e-6D) return snapshot.physicalMassBefore();

		double ratio = ImpulseMath.clamp(projectile.getProjectileMass() / cbcMassBefore, 0.0D, 1.0D);
		return snapshot.physicalMassBefore() * ratio;
	}
}
