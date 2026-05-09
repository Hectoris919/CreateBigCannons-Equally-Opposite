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
					: Config.baseBigCannonGasImpulse() * powderChargeEquivalent;
			return new PowderAndGas(fallbackGasImpulse, powderChargeEquivalent, barrelLengthBlocks);
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
					: Config.baseBigCannonGasImpulse() * powderChargeEquivalent;
			return new PowderAndGas(fallbackGasImpulse, powderChargeEquivalent, barrelLengthBlocks);
		}

		return new PowderAndGas(gasImpulse, powderChargeEquivalent, barrelLengthBlocks);
	}

	private record PowderAndGas(double gasImpulse, double powderChargeEquivalent, double barrelLengthBlocks) { }
}
