package org.hectoris919.cbc_equally_opposite.physics;

import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.utils.CBCUtils;

public final class ImpactImpulseHandler {
	private ImpactImpulseHandler() { }

	public static ImpactSnapshot capture(AbstractCannonProjectile projectile, BlockState state, BlockHitResult hitResult, Vec3 velocityBefore, boolean autocannon) {
		if (Config.disableImpact()) return null;

		Level level = projectile.level();
		if (level.isClientSide || !(level instanceof ServerLevel)) return null;
		if (SableProjectileAttachment.isAttached(projectile)) return null;
		if (!ImpulseMath.isUsefulVector(velocityBefore)) return null;

		Vec3 direction = velocityBefore.normalize();
		Vec3 surfaceNormal = CBCUtils.getSurfaceNormalVector(level, hitResult);
		double incidence = 1.0D;
		if (ImpulseMath.isUsefulVector(surfaceNormal)) incidence = Math.abs(direction.dot(surfaceNormal.normalize()));

		return new ImpactSnapshot(
				hitResult,
				velocityBefore,
				ImpulseMath.isUsefulVector(surfaceNormal)
						? surfaceNormal.normalize()
						: Vec3.ZERO,
				incidence,
				projectile.getProjectileMass(),
				PhysicalProjectileMass.mass(projectile, autocannon),
				materialFactor(level, state, hitResult),
				autocannon
		);
	}

	public static void applyFromCurrentProjectileState(AbstractCannonProjectile projectile, ImpactSnapshot snapshot, AbstractCannonProjectile.ImpactResult result) {
		if (snapshot == null || Config.disableImpact()) return;

		Level level = projectile.level();
		if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

		boolean wasAlreadyAttached = SableProjectileAttachment.isAttached(projectile);
		OutcomeClass outcomeClass = classifyOutcome(projectile, snapshot, result);
		if (wasAlreadyAttached) return;
		if (outcomeClass == OutcomeClass.STOPPED) SableProjectileAttachment.attach(projectile, snapshot.hitResult(), snapshot.velocityBefore());

		Vec3 velocityBefore = snapshot.velocityBefore().scale(20.0D);
		double massBeforeKg = snapshot.physicalMassBefore();
		if (!Double.isFinite(massBeforeKg) || massBeforeKg <= 0.0D || !ImpulseMath.isUsefulVector(velocityBefore)) return;

		Vec3 momentumBefore = velocityBefore.scale(massBeforeKg);
		Vec3 impulseNs = switch (outcomeClass) {
			case PENETRATION -> penetrationImpulse(snapshot, projectile, momentumBefore, massBeforeKg, velocityBefore);
			case RICOCHET -> ricochetImpulse(snapshot, projectile, momentumBefore);
			case STOPPED -> stoppedImpulse(momentumBefore);
			case SHATTERED -> shatteredImpulse(momentumBefore);
		};

		impulseNs = clampImpulseVector(impulseNs);
		if (!ImpulseMath.isUsefulVector(impulseNs)) return;

		Vec3 impactImpulse = ImpulseMath.siImpulseToSable(impulseNs);
		if (!ImpulseMath.isUsefulVector(impactImpulse)) return;

		boolean queued = PendingImpulseQueue.enqueueForContaining(
				serverLevel,
				snapshot.hitResult().getLocation(),
				impactImpulse,
				ImpulseKind.IMPACT,
				1
		);
		if (queued && Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"{} impact {}: beforeMass={} kg, beforeVel={} m/s, materialFactor={}, impulse={} N*s, sable={}, point={}",
					snapshot.autocannon() ? "Autocannon" : "Big cannon",
					outcomeClass,
					massBeforeKg,
					velocityBefore,
					snapshot.materialFactor(),
					impulseNs,
					impactImpulse,
					snapshot.hitResult().getLocation()
			);
		}
	}

	private static OutcomeClass classifyOutcome(AbstractCannonProjectile projectile, ImpactSnapshot snapshot, AbstractCannonProjectile.ImpactResult result) {
		if (result != null) {
			if (result.shouldRemove()) return OutcomeClass.SHATTERED;
			if (projectile.getProjectileMass() <= 1.0e-4F || result.kinematics() == AbstractCannonProjectile.ImpactResult.KinematicOutcome.STOP) return OutcomeClass.STOPPED;
			if (result.kinematics() == AbstractCannonProjectile.ImpactResult.KinematicOutcome.BOUNCE) return OutcomeClass.RICOCHET;
			return OutcomeClass.PENETRATION;
		}

		if (projectile.isRemoved()) return OutcomeClass.SHATTERED;
		if (projectile.getProjectileMass() <= 1.0e-4F) return OutcomeClass.STOPPED;

		Vec3 after = projectile.getDeltaMovement();
		if (ImpulseMath.isUsefulVector(after) && ImpulseMath.isUsefulVector(snapshot.velocityBefore())) {
			double dot = snapshot.velocityBefore().normalize().dot(after.normalize());
			if (dot < 0.5D) return OutcomeClass.RICOCHET;
		}

		return OutcomeClass.PENETRATION;
	}

	private static Vec3 penetrationImpulse(ImpactSnapshot snapshot, AbstractCannonProjectile projectile, Vec3 momentumBefore, double massBeforeKg, Vec3 velocityBefore) {
		Vec3 momentumAfter = currentMomentumAfter(projectile, snapshot);
		Vec3 measuredImpulseNs = momentumBefore.subtract(momentumAfter);
		Vec3 travel = snapshot.velocityBefore();
		if (!ImpulseMath.isUsefulVector(travel) || !ImpulseMath.isUsefulVector(velocityBefore)) return measuredImpulseNs.scale(Config.impactEfficiency()).scale(Config.penetrationImpulseMultiplier());

		Vec3 incomingDirection = travel.normalize();
		double measuredForwardImpulseNs = 0.0D;
		if (ImpulseMath.isUsefulVector(measuredImpulseNs)) measuredForwardImpulseNs = Math.max(0.0D, measuredImpulseNs.dot(incomingDirection));

		double measuredForwardAfterEfficiencyNs = measuredForwardImpulseNs * Config.impactEfficiency();
		double speedBefore = velocityBefore.length();
		double kineticEnergyBeforeJ = 0.5D * massBeforeKg * speedBefore * speedBefore;
		double effectiveEnergyLossFraction = Config.penetrationEnergyLossFraction() * snapshot.materialFactor();
		double energyLossJ = kineticEnergyBeforeJ * effectiveEnergyLossFraction;
		double transferVelocity = Config.penetrationEnergyTransferVelocity();

		double energyLossImpulseNs = 0.0D;
		if (Double.isFinite(energyLossJ) && energyLossJ > 0.0D && Double.isFinite(transferVelocity) && transferVelocity > 0.0D) {
			energyLossImpulseNs = energyLossJ / transferVelocity;
			energyLossImpulseNs *= Config.penetrationEfficiency();
		}

		double finalForwardImpulseNs = Math.max(measuredForwardAfterEfficiencyNs, energyLossImpulseNs);
		if (!Double.isFinite(finalForwardImpulseNs) || finalForwardImpulseNs <= 0.0D) return measuredImpulseNs.scale(Config.impactEfficiency()).scale(Config.penetrationImpulseMultiplier());

		return incomingDirection.scale(finalForwardImpulseNs * Config.penetrationImpulseMultiplier());
	}

	private static Vec3 ricochetImpulse(ImpactSnapshot snapshot, AbstractCannonProjectile projectile, Vec3 momentumBefore) {
		double massAfterKg = PhysicalProjectileMass.remainingMass(snapshot, projectile);
		Vec3 velocityAfter = estimateRicochetVelocity(snapshot).scale(20.0D);
		Vec3 momentumAfter = velocityAfter.scale(Math.max(0.0D, massAfterKg));
		return momentumBefore.subtract(momentumAfter)
				.scale(Config.impactEfficiency())
				.scale(Config.ricochetImpulseMultiplier());
	}

	private static Vec3 stoppedImpulse(Vec3 momentumBefore) {
		return momentumBefore.scale(Config.impactEfficiency()).scale(Config.destroyedImpulseMultiplier());
	}

	private static Vec3 shatteredImpulse(Vec3 momentumBefore) {
		double retained = Config.shatterMomentumRetention();
		retained = ImpulseMath.clamp(retained, 0.0D, 1.0D);
		return momentumBefore.scale(1.0D - retained)
				.scale(Config.impactEfficiency())
				.scale(Config.destroyedImpulseMultiplier());
	}

	private static Vec3 currentMomentumAfter(AbstractCannonProjectile projectile, ImpactSnapshot snapshot) {
		double massAfterKg = PhysicalProjectileMass.remainingMass(snapshot, projectile);
		Vec3 currentVelocity = projectile.getDeltaMovement();
		if (!ImpulseMath.isUsefulVector(currentVelocity)) currentVelocity = snapshot.velocityBefore();
		return currentVelocity.scale(20.0D).scale(Math.max(0.0D, massAfterKg));
	}

	private static Vec3 estimateRicochetVelocity(ImpactSnapshot snapshot) {
		Vec3 velocity = snapshot.velocityBefore();
		Vec3 normal = snapshot.surfaceNormal();
		if (!ImpulseMath.isUsefulVector(velocity) || !ImpulseMath.isUsefulVector(normal)) return Vec3.ZERO;

		Vec3 n = normal.normalize();
		Vec3 normalComponent = n.scale(velocity.dot(n));
		Vec3 tangentialComponent = velocity.subtract(normalComponent);
		return tangentialComponent.scale(Config.ricochetTangentialRetention())
				.subtract(normalComponent.scale(Config.ricochetNormalRestitution()));
	}

	private static double materialFactor(Level level, BlockState state, BlockHitResult hitResult) {
		try {
			double hardness = state.getDestroySpeed(level, hitResult.getBlockPos());
			if (!Double.isFinite(hardness) || hardness < 0.0D) return 4.0D;

			double reference = Config.penetrationHardnessReference();
			double exponent = Config.penetrationMaterialExponent();
			return exponent <= 0.0D
					? 1.0D
					: Math.pow(Math.max(0.0D, hardness) / reference, exponent);
		} catch (Throwable ignored) {
			return 1.0D;
		}
	}

	private static Vec3 clampImpulseVector(Vec3 impulseNs) {
		if (!ImpulseMath.isUsefulVector(impulseNs)) return Vec3.ZERO;

		double magnitude = impulseNs.length();
		double clamped = Math.max(magnitude, 0);
		if (!Double.isFinite(clamped) || clamped <= 0.0D) return Vec3.ZERO;

		return impulseNs.scale(clamped / magnitude);
	}

	private enum OutcomeClass {
		PENETRATION,
		RICOCHET,
		STOPPED,
		SHATTERED
	}
}
