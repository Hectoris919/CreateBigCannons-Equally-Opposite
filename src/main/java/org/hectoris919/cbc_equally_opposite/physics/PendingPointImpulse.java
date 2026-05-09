package org.hectoris919.cbc_equally_opposite.physics;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record PendingPointImpulse(
		UUID subLevelId,
		Vec3 pointInSubLevelSpace,
		Vec3 impulseInSubLevelSpace,
		ImpulseKind kind,
		int remainingSteps
) {
	public PendingPointImpulse nextStep() {
		return new PendingPointImpulse(
				this.subLevelId,
				this.pointInSubLevelSpace,
				this.impulseInSubLevelSpace,
				this.kind,
				this.remainingSteps - 1
		);
	}
}
