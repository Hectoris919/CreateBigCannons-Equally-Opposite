package org.hectoris919.cbc_equally_opposite.physics;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public record ImpactSnapshot(
		BlockHitResult hitResult,
		Vec3 velocityBefore,
		Vec3 surfaceNormal,
		double incidence,
		double cbcMassBefore,
		double physicalMassBefore,
		double materialFactor,
		boolean autocannon
) {
}
