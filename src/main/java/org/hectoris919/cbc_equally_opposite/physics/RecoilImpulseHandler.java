package org.hectoris919.cbc_equally_opposite.physics;

import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import org.hectoris919.cbc_equally_opposite.compat.goingballistic.GoingBallisticAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

public final class RecoilImpulseHandler {
	private RecoilImpulseHandler() { }

	public static void onProjectileShot(AbstractCannonProjectile projectile, boolean autocannon) {
		if (Config.disableRecoil()) return;

		Level level = projectile.level();
		if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

		Vec3 deltaBpt = projectile.getDeltaMovement();
		if (!ImpulseMath.isUsefulVector(deltaBpt)) return;

		Vec3 forward = deltaBpt.normalize();
		double speedBpt = deltaBpt.length();
		double speedMps = ImpulseMath.bptToMps(speedBpt);
		double projectileMass = PhysicalProjectileMass.mass(projectile, autocannon);

		PowderAndGas powderAndGas = computeGasImpulse(projectile, autocannon, speedMps);
		double impulse = Config.projectileMomentumMultiplier() * projectileMass * speedMps + powderAndGas.gasImpulse();
		impulse = Math.max(impulse, 0);

		if (!Double.isFinite(impulse) || impulse <= 0.0D) return;

		double sableImpulse = ImpulseMath.siImpulseToSable(impulse);
		Vec3 recoilImpulse = forward.scale(-sableImpulse);
		Vec3 applicationPoint = projectile.position().subtract(forward.scale(Config.recoilApplicationOffset()));
		int recoilSteps = computeRecoilSteps(speedBpt, powderAndGas.powderChargeEquivalent(), powderAndGas.barrelLengthBlocks());

		boolean queued = PendingImpulseQueue.enqueueForContaining(serverLevel, applicationPoint, recoilImpulse, ImpulseKind.RECOIL, recoilSteps);
		if (queued && Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"{} recoil: mass={} kg, speed={} m/s, gas={} N*s, total={} N*s, sable={} kpg*b/t, steps={}, powderEquivalent={}, barrelLength={} blocks, point={}, impulse={}",
					autocannon ? "Autocannon" : "Big cannon",
					projectileMass,
					speedMps,
					powderAndGas.gasImpulse(),
					impulse,
					sableImpulse,
					recoilSteps,
					powderAndGas.powderChargeEquivalent(),
					powderAndGas.barrelLengthBlocks(),
					applicationPoint,
					recoilImpulse
			);
		}

		Vec3 muzzleEndPosition = MountedCannonContextTracker.currentMuzzleEndPosition();
		if (muzzleEndPosition == null || !ImpulseMath.isFinite(muzzleEndPosition)) muzzleEndPosition = projectile.position();

		MuzzleBlastHandler.start(
				serverLevel,
				muzzleEndPosition,
				forward,
				powderAndGas.gasImpulse(),
				powderAndGas.powderMass(),
				powderAndGas.gasVelocity(),
				projectileMass,
				speedMps,
				recoilSteps,
				autocannon,
				projectile
		);
	}

	public static void onCannonBlankFire(ServerLevel level, Vec3 forward, Vec3 projectileSpawnPosition, double chargePower, double barrelLengthBlocks) {
		if (Config.disableRecoil()) return;
		if (!ImpulseMath.isUsefulVector(forward)) return;

		Vec3 normalizedForward = forward.normalize();
		double speedBpt = Math.max(0.0D, chargePower);
		double speedMps = ImpulseMath.bptToMps(speedBpt);
		PowderAndGas powderAndGas = computeCannonGasImpulse(chargePower, speedMps, barrelLengthBlocks);
		double impulse = Math.max(0.0D, powderAndGas.gasImpulse());
		if (!Double.isFinite(impulse) || impulse <= 0.0D) return;

		double sableImpulse = ImpulseMath.siImpulseToSable(impulse);
		Vec3 recoilImpulse = normalizedForward.scale(-sableImpulse);
		Vec3 applicationPoint = projectileSpawnPosition.subtract(normalizedForward.scale(Config.recoilApplicationOffset()));
		int recoilSteps = computeRecoilSteps(speedBpt, powderAndGas.powderChargeEquivalent(), powderAndGas.barrelLengthBlocks());

		boolean queued = PendingImpulseQueue.enqueueForContaining(level, applicationPoint, recoilImpulse, ImpulseKind.RECOIL, recoilSteps);
		if (queued && Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"Big cannon blank recoil: chargePower={}, gas={} N*s, sable={} kpg*b/t, steps={}, powderEquivalent={}, barrelLength={} blocks, point={}, impulse={}",
					chargePower,
					powderAndGas.gasImpulse(),
					sableImpulse,
					recoilSteps,
					powderAndGas.powderChargeEquivalent(),
					powderAndGas.barrelLengthBlocks(),
					applicationPoint,
					recoilImpulse
			);
		}

		Vec3 muzzleEndPosition = MountedCannonContextTracker.currentMuzzleEndPosition();
		if (muzzleEndPosition == null || !ImpulseMath.isFinite(muzzleEndPosition)) muzzleEndPosition = projectileSpawnPosition;

		MuzzleBlastHandler.start(
				level,
				muzzleEndPosition,
				normalizedForward,
				powderAndGas.gasImpulse(),
				powderAndGas.powderMass(),
				powderAndGas.gasVelocity(),
				0.0D,
				speedMps,
				recoilSteps,
				false,
				null
		);
	}

	private static int computeRecoilSteps(double projectileSpeedBpt, double powderChargeEquivalent, double barrelLengthBlocks) {
		double barrelTicks = Math.max(0.0D, barrelLengthBlocks) / Math.max(projectileSpeedBpt, 0.1D);
		double ventTicks = Math.max(0.0D, powderChargeEquivalent) * Config.recoilVentTicksPerCharge();
		return (int) Math.ceil(barrelTicks + ventTicks);
	}

	private static PowderAndGas computeGasImpulse(Entity projectile, boolean autocannon, double projectileSpeedMps) {
		double recordedChargePower = ProjectileChargeTracker.getAndClear(projectile).orElse(autocannon ? 1.0D : 2.0D);
		double powderChargeEquivalent = autocannon
				? 1.0D
				: Math.max(0.0D, recordedChargePower / 2.0D);
		double barrelLengthBlocks = autocannon
				? Math.max(1.0D, recordedChargePower)
				: MountedCannonContextTracker.currentBarrelLengthBlocks();

		if (!GoingBallisticAccess.isAvailable()) {
			double fallbackGasImpulse = autocannon
					? Config.baseAutocannonGasImpulse()
					: Config.baseCannonGasImpulse() * powderChargeEquivalent;
			double gasVelocityMps = projectileSpeedMps * Config.gasVelocityMultiplier();
			double powderMass = approximatePowderMassFromGasImpulse(fallbackGasImpulse, gasVelocityMps);
			return new PowderAndGas(fallbackGasImpulse, powderMass, gasVelocityMps, powderChargeEquivalent, barrelLengthBlocks);
		}

		double fallbackPowderMass = autocannon ? 0.09D : 0.33D;
		double powderMass;
		if (autocannon) {
			powderMass = GoingBallisticAccess.autocannonPowderMass(projectile, fallbackPowderMass);
		} else {
			powderMass = powderChargeEquivalent * GoingBallisticAccess.cannonPowderMass(fallbackPowderMass);
		}

		double gasVelocityMps = projectileSpeedMps * Config.gasVelocityMultiplier();
		double gasImpulse = Config.gasMomentumMultiplier() * powderMass * gasVelocityMps;

		if (!Double.isFinite(gasImpulse) || gasImpulse < 0.0D) {
			double fallbackGasImpulse = autocannon
					? Config.baseAutocannonGasImpulse()
					: Config.baseCannonGasImpulse() * powderChargeEquivalent;
			double fallbackPowderMassKg = approximatePowderMassFromGasImpulse(fallbackGasImpulse, gasVelocityMps);
			return new PowderAndGas(fallbackGasImpulse, fallbackPowderMass, gasVelocityMps, powderChargeEquivalent, barrelLengthBlocks);
		}

		return new PowderAndGas(gasImpulse, powderMass, gasVelocityMps, powderChargeEquivalent, barrelLengthBlocks);
	}

	private static PowderAndGas computeCannonGasImpulse(double chargePower, double referenceSpeedMps, double barrelLengthBlocks) {
		double powderChargeEquivalent = Math.max(0.0D, chargePower / 2.0D);
		double saneBarrelLength = Double.isFinite(barrelLengthBlocks) && barrelLengthBlocks > 0.0D ? barrelLengthBlocks : 1.0D;

		if (!GoingBallisticAccess.isAvailable()) {
			double fallbackGasImpulse = Config.baseCannonGasImpulse() * powderChargeEquivalent;
			double gasVelocityMps = referenceSpeedMps * Config.gasVelocityMultiplier();
			double powderMass = approximatePowderMassFromGasImpulse(fallbackGasImpulse, gasVelocityMps);
			return new PowderAndGas(fallbackGasImpulse, powderMass, gasVelocityMps, powderChargeEquivalent, saneBarrelLength);
		}

		double powderMass = powderChargeEquivalent * GoingBallisticAccess.cannonPowderMass(0.33D);
		double gasVelocityMps = referenceSpeedMps * Config.gasVelocityMultiplier();
		double gasImpulse = Config.gasMomentumMultiplier() * powderMass * gasVelocityMps;

		if (!Double.isFinite(gasImpulse) || gasImpulse < 0.0D) {
			double fallbackGasImpulse = Config.baseCannonGasImpulse() * powderChargeEquivalent;
			double fallbackPowderMass = approximatePowderMassFromGasImpulse(fallbackGasImpulse, gasVelocityMps);
			return new PowderAndGas(fallbackGasImpulse, fallbackPowderMass, gasVelocityMps, powderChargeEquivalent, saneBarrelLength);
		}

		return new PowderAndGas(gasImpulse, powderMass, gasVelocityMps, powderChargeEquivalent, saneBarrelLength);
	}

	private static double approximatePowderMassFromGasImpulse(double gasImpulse, double gasVelocityMps) {
		if (!Double.isFinite(gasImpulse) || gasImpulse <= 0.0D || !Double.isFinite(gasVelocityMps) || gasVelocityMps <= 0.0D) return 0.0D;
		return gasImpulse / gasVelocityMps;
	}

	private record PowderAndGas(double gasImpulse, double powderMass, double gasVelocity, double powderChargeEquivalent, double barrelLengthBlocks) { }
}
