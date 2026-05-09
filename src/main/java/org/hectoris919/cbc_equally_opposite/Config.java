package org.hectoris919.cbc_equally_opposite;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
	private Config() { }

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.DoubleValue IMPULSE_SCALE;
	private static final ModConfigSpec.BooleanValue DISABLE_RECOIL;
	private static final ModConfigSpec.BooleanValue DISABLE_IMPACT;
	private static final ModConfigSpec.BooleanValue DEBUG_IMPULSES;

	private static final ModConfigSpec.DoubleValue PROJECTILE_MOMENTUM_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue GAS_MOMENTUM_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue GAS_VELOCITY_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue RECOIL_APPLICATION_OFFSET;

	private static final ModConfigSpec.DoubleValue BASE_CANNON_PROJECTILE_MASS;
	private static final ModConfigSpec.DoubleValue BASE_AUTOCANNON_PROJECTILE_MASS;
	private static final ModConfigSpec.DoubleValue BASE_CANNON_GAS_IMPULSE;
	private static final ModConfigSpec.DoubleValue BASE_AUTOCANNON_GAS_IMPULSE;

	private static final ModConfigSpec.DoubleValue IMPACT_EFFICIENCY;
	private static final ModConfigSpec.DoubleValue PENETRATION_IMPULSE_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue PENETRATION_ENERGY_LOSS_FRACTION;
	private static final ModConfigSpec.DoubleValue PENETRATION_ENERGY_TRANSFER_VELOCITY;
	private static final ModConfigSpec.DoubleValue PENETRATION_EFFICIENCY;
	private static final ModConfigSpec.DoubleValue PENETRATION_HARDNESS_REFERENCE;
	private static final ModConfigSpec.DoubleValue PENETRATION_MATERIAL_EXPONENT;

	private static final ModConfigSpec.DoubleValue DESTROYED_IMPULSE_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue RICOCHET_IMPULSE_MULTIPLIER;
	private static final ModConfigSpec.DoubleValue RICOCHET_NORMAL_RESTITUTION;
	private static final ModConfigSpec.DoubleValue RICOCHET_TANGENTIAL_RETENTION;
	private static final ModConfigSpec.DoubleValue SHATTER_MOMENTUM_RETENTION;
	private static final ModConfigSpec.DoubleValue RECOIL_VENT_TICKS_PER_CHARGE;

	public static final ModConfigSpec SPEC;

	static {
		BUILDER.push("general");
		DISABLE_RECOIL = BUILDER
				.comment("Whether cannons mounted on Sable sublevels should apply recoil impulses when fired.")
				.define("disableRecoil", false);
		DISABLE_IMPACT = BUILDER
				.comment("Whether projectiles should apply impact impulses when striking Sable sublevels.")
				.define("disableImpact", false);
		DEBUG_IMPULSES = BUILDER
				.comment("Logs the details of recoil and impact events")
				.define("debugImpulses", false);
		BUILDER.pop();

		BUILDER.push("units");
		IMPULSE_SCALE = defineDouble(
				"impulseScale",
				4.0D / 7873.0D,
				0.0D,
				Double.MAX_VALUE,
				"Converts SI impulse in kg*m/s or N*s into Sable impulse units. " +
						"Default assumes 4 Sable kpg = 7873 kg (based off of the iron block kpg weight vs irl weight), so scale = 4 / 7873."
		);
		BUILDER.pop();

		BUILDER.push("recoil");
		PROJECTILE_MOMENTUM_MULTIPLIER = defineDouble("projectileMomentumMultiplier", 1.0D, 0.0D, 1000.0D,
				"Multiplier for projectile momentum in the recoil equation.");
		GAS_MOMENTUM_MULTIPLIER = defineDouble("gasMomentumMultiplier", 1.0D, 0.0D, 1000.0D,
				"Multiplier for propellant gas momentum when Going Ballistic data or fallback gas impulse is used.");
		GAS_VELOCITY_MULTIPLIER = defineDouble("gasVelocityMultiplier", 1.25D, 0.0D, 1000.0D,
				"Effective propellant gas velocity as a multiple of projectile velocity.");
		RECOIL_APPLICATION_OFFSET = defineDouble("recoilApplicationOffsetBlocks", 1.0D, 0.0D, 16.0D,
				"Distance behind the projectile spawn point where recoil is applied. This approximates the breech position.");
		RECOIL_VENT_TICKS_PER_CHARGE = defineDouble("recoilVentTicksPerCharge", 1.0D, 0.0D, 100.0D,
				"Additional recoil application ticks per powder charge equivalent, approximating propellant vent time.");
		BUILDER.pop();

		BUILDER.push("baseCbcFallbacks");
		BASE_CANNON_PROJECTILE_MASS = defineDouble("baseCannonProjectileMass", 3000.0D, 0.0D, Double.MAX_VALUE,
				"Fallback projectile mass for base cannon recoil when Going Ballistic is absent.");
		BASE_AUTOCANNON_PROJECTILE_MASS = defineDouble("baseAutocannonProjectileMass", 33.0D, 0.0D, Double.MAX_VALUE,
				"Fallback projectile mass for base autocannon recoil when Going Ballistic is absent.");
		BASE_CANNON_GAS_IMPULSE = defineDouble("baseCannonGasImpulse", 50_000.0D, 0.0D, Double.MAX_VALUE,
				"Fallback cannon gas impulse in N*s.");
		BASE_AUTOCANNON_GAS_IMPULSE = defineDouble("baseAutocannonGasImpulse", 750.0D, 0.0D, Double.MAX_VALUE,
				"Fallback autocannon gas impulse in N*s.");
		BUILDER.pop();

		BUILDER.push("impact");
		IMPACT_EFFICIENCY = defineDouble("impactEfficiency", 0.65D, 0.0D, 1000.0D,
				"Fraction of projectile momentum transferred to the hit sublevel.");
		PENETRATION_IMPULSE_MULTIPLIER = defineDouble("penetrationImpulseMultiplier", 1.0D, 0.0D, 1000.0D,
				"Multiplier applied to lost-momentum impact impulse when the projectile penetrates.");
		PENETRATION_ENERGY_LOSS_FRACTION = defineDouble("penetrationEnergyLossFraction", 0.08D, 0.0D, 1.0D,
				"Fraction of projectile kinetic energy lost to plowing through each penetrated block before material scaling.");
		PENETRATION_ENERGY_TRANSFER_VELOCITY = defineDouble("penetrationEnergyTransferVelocity", 50.0D, 0.001D, Double.MAX_VALUE,
				"Effective velocity used to convert penetration energy loss into impulse: impulse = lostEnergy / transferVelocity.");
		PENETRATION_EFFICIENCY = defineDouble("penetrationEfficiency", 1.0D, 0.0D, 1000.0D,
				"Multiplier applied to the energy-loss impulse before penetrationImpulseMultiplier and global impact clamps.");
		PENETRATION_HARDNESS_REFERENCE = defineDouble("penetrationHardnessReference", 1.5D, 0.001D, Double.MAX_VALUE,
				"Block destroy speed/hardness treated as a neutral material factor for penetration energy loss. Stone is about 1.5.");
		PENETRATION_MATERIAL_EXPONENT = defineDouble("penetrationMaterialExponent", 0.5D, 0.0D, 8.0D,
				"Exponent applied to block hardness scaling for penetration energy loss. 0 disables material scaling; 1 is linear hardness scaling.");
		DESTROYED_IMPULSE_MULTIPLIER = defineDouble("destroyedImpulseMultiplier", 1.0D, 0.0D, 1000.0D,
				"Multiplier applied to impact impulse when the projectile stops, is destroyed, or shatters.");
		RICOCHET_IMPULSE_MULTIPLIER = defineDouble("ricochetImpulseMultiplier", 1.0D, 0.0D, 1000.0D,
				"Multiplier applied to lost-momentum impact impulse when the projectile bounces or ricochets.");
		RICOCHET_NORMAL_RESTITUTION = defineDouble("ricochetNormalRestitution", 0.35D, 0.0D, 2.0D,
				"Fraction of the surface-normal velocity component retained and reversed by a ricochet.");
		RICOCHET_TANGENTIAL_RETENTION = defineDouble("ricochetTangentialRetention", 0.85D, 0.0D, 2.0D,
				"Fraction of the surface-tangential velocity component retained by a ricochet.");
		SHATTER_MOMENTUM_RETENTION = defineDouble("shatterMomentumRetention", 0.25D, 0.0D, 1.0D,
				"Fraction of incoming projectile momentum assumed to remain in forward-moving fragments when the projectile shatters/is removed.");
		BUILDER.pop();

		SPEC = BUILDER.build();
	}
	private static ModConfigSpec.DoubleValue defineDouble(String name, double value, double min, double max, String comment) {
		return BUILDER.comment(comment).defineInRange(name, value, min, max);
	}

	public static boolean disableRecoil() { return DISABLE_RECOIL.get(); }
	public static boolean disableImpact() { return DISABLE_IMPACT.get(); }
	public static boolean debugImpulses() { return DEBUG_IMPULSES.get(); }

	public static double impulseScale() { return IMPULSE_SCALE.get(); }

	public static double projectileMomentumMultiplier() { return PROJECTILE_MOMENTUM_MULTIPLIER.get(); }
	public static double gasMomentumMultiplier() { return GAS_MOMENTUM_MULTIPLIER.get(); }
	public static double gasVelocityMultiplier() { return GAS_VELOCITY_MULTIPLIER.get(); }
	public static double recoilApplicationOffset() { return RECOIL_APPLICATION_OFFSET.get(); }

	public static double baseBigCannonProjectileMass() { return BASE_CANNON_PROJECTILE_MASS.get(); }
	public static double baseAutocannonProjectileMass() { return BASE_AUTOCANNON_PROJECTILE_MASS.get(); }
	public static double baseBigCannonGasImpulse() { return BASE_CANNON_GAS_IMPULSE.get(); }
	public static double baseAutocannonGasImpulse() { return BASE_AUTOCANNON_GAS_IMPULSE.get(); }

	public static double impactEfficiency() { return IMPACT_EFFICIENCY.get(); }

	public static double penetrationEfficiency() { return PENETRATION_EFFICIENCY.get(); }
	public static double penetrationImpulseMultiplier() { return PENETRATION_IMPULSE_MULTIPLIER.get(); }
	public static double penetrationEnergyLossFraction() { return PENETRATION_ENERGY_LOSS_FRACTION.get(); }
	public static double penetrationEnergyTransferVelocity() { return PENETRATION_ENERGY_TRANSFER_VELOCITY.get(); }
	public static double penetrationHardnessReference() { return PENETRATION_HARDNESS_REFERENCE.get(); }
	public static double penetrationMaterialExponent() { return PENETRATION_MATERIAL_EXPONENT.get(); }

	public static double destroyedImpulseMultiplier() { return DESTROYED_IMPULSE_MULTIPLIER.get(); }

	public static double ricochetImpulseMultiplier() { return RICOCHET_IMPULSE_MULTIPLIER.get(); }
	public static double ricochetNormalRestitution() { return RICOCHET_NORMAL_RESTITUTION.get(); }
	public static double ricochetTangentialRetention() { return RICOCHET_TANGENTIAL_RETENTION.get(); }

	public static double shatterMomentumRetention() { return SHATTER_MOMENTUM_RETENTION.get(); }
	public static double recoilVentTicksPerCharge() { return RECOIL_VENT_TICKS_PER_CHARGE.get(); }

}
