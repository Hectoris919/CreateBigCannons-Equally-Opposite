package org.hectoris919.cbc_equally_opposite;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
	private Config() { }

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.DoubleValue IMPULSE_SCALE;
	private static final ModConfigSpec.BooleanValue DISABLE_RECOIL;
	private static final ModConfigSpec.BooleanValue DISABLE_IMPACT;
	private static final ModConfigSpec.BooleanValue DISABLE_MUZZLE_BLAST;
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

	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_CANNON_HALF_ANGLE_DEGREES;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_AUTOCANNON_HALF_ANGLE_DEGREES;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_PROFILE_EXPONENT;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_COUPLING;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_CANNON_BORE_RADIUS;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_AUTOCANNON_BORE_RADIUS;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_CALCULATION_RANGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_ENTITY_DENSITY;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_FORCE_DOSE_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_PRESSURE_IMPULSE_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_KINETIC_ENERGY_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_THERMAL_DOSE_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_ENTITY_DRAG_COEFFICIENT;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_POWDER_SPECIFIC_ENERGY;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_THERMAL_EFFICIENCY;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_GAS_CP;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_GAS_CV;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_AMBIENT_TEMPERATURE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_BODY_TEMPERATURE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_TEMPERATURE_MIXING_EXPONENT;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_HEAT_TRANSFER_EFFICIENCY;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_IGNITION_TEMPERATURE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_IGNITION_DOSE;
	private static final ModConfigSpec.DoubleValue MUZZLE_BLAST_BURN_DOSE_PER_SECOND;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_EFFICIENCY;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_IMPULSE_SCALE;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_RANGE;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_RAY_COUNT;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_FORWARD_BIAS;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_AIR_DENSITY;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_SOUND_SPEED;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_POSITIVE_PHASE_SECONDS;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_PRESSURE_IMPULSE_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_PEAK_PRESSURE_PER_DAMAGE;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_SOLID_REFLECTION_COEFFICIENT;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_ENTITY_ABSORPTION_COEFFICIENT;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_ENTITY_REFLECTION_COEFFICIENT;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_MAX_REFLECTIONS;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_MAX_REFLECTION_SOURCES;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_MIN_REFLECTION_ENERGY_FRACTION;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_MAX_SUBLEVEL_SURFACE_CONTACTS;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_MAX_SUBLEVEL_CONTACTS_PER_BODY;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_NEGATIVE_PHASE_FRACTION;
	private static final ModConfigSpec.IntValue MUZZLE_SHOCK_NEGATIVE_PHASE_DELAY_TICKS;
	private static final ModConfigSpec.DoubleValue MUZZLE_SHOCK_NEGATIVE_PHASE_DURATION_MULTIPLIER;

	public static final ModConfigSpec SPEC;

	static {
		BUILDER.push("general");
		DISABLE_RECOIL = BUILDER
				.comment("Whether cannons mounted on Sable sublevels should apply recoil impulses when fired.")
				.define("disableRecoil", false);
		DISABLE_IMPACT = BUILDER
				.comment("Whether projectiles should apply impact impulses when striking Sable sublevels.")
				.define("disableImpact", false);
		DISABLE_MUZZLE_BLAST = BUILDER
				.comment("Whether escaping muzzle gases apply forces and potentially damage to entities and Sable sublevels caught within a cannon blast.")
				.define("disableMuzzleBlast", false);
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

		BUILDER.push("muzzleBlast");
		MUZZLE_BLAST_CANNON_HALF_ANGLE_DEGREES = defineDouble("bigCannonHalfAngleDegrees", 14.0D, 0.0D, Double.MAX_VALUE,
				"Half-angle of the big-cannon gas jet cone in degrees. Values outside a finite physical range cause the blast calculation to be skipped rather than capped.");
		MUZZLE_BLAST_AUTOCANNON_HALF_ANGLE_DEGREES = defineDouble("autocannonHalfAngleDegrees", 7.0D, 0.0D, Double.MAX_VALUE,
				"Half-angle of the autocannon gas jet cone in degrees. Values outside a finite physical range cause the blast calculation to be skipped rather than capped.");
		MUZZLE_BLAST_PROFILE_EXPONENT = defineDouble("profileExponent", 1.5D, 0.0D, Double.MAX_VALUE,
				"Radial jet profile exponent n in W=(n+1)/(pi*R^2)*(1-eta^2)^n.");
		MUZZLE_BLAST_COUPLING = defineDouble("momentumCoupling", 0.25D, 0.0D, Double.MAX_VALUE,
				"Fraction of local gas momentum density coupled into targets instead of flowing around them.");
		MUZZLE_BLAST_CANNON_BORE_RADIUS = defineDouble("bigCannonBoreRadiusMeters", 0.25D, 0.0D, Double.MAX_VALUE,
				"Effective big-cannon bore radius used as the starting radius of the blast cone.");
		MUZZLE_BLAST_AUTOCANNON_BORE_RADIUS = defineDouble("autocannonBoreRadiusMeters", 0.04D, 0.0D, Double.MAX_VALUE,
				"Effective autocannon bore radius used as the starting radius of the blast cone.");
		MUZZLE_BLAST_CALCULATION_RANGE = defineDouble("calculationRangeBlocks", 16.0D, 0.0D, Double.MAX_VALUE,
				"Maximum distance ahead of the muzzle searched for blast targets. This is a computational search boundary, not a force clamp.");
		MUZZLE_BLAST_ENTITY_DENSITY = defineDouble("entityEffectiveDensityKgPerBlock3", 110.0D, 0.0D, Double.MAX_VALUE,
				"Fallback effective density for entities whose true mass is unknown. Entity mass = bounding-box volume * this density.");
		MUZZLE_BLAST_FORCE_DOSE_PER_DAMAGE = defineDouble("forceDosePerDamage", 12_000.0D, 0.0D, Double.MAX_VALUE,
				"Impulse-equivalent force dose in N*s per Minecraft damage point from muzzle blast loading.");
		MUZZLE_BLAST_PRESSURE_IMPULSE_PER_DAMAGE = defineDouble("pressureImpulsePerDamage", 900.0D, 0.0D, Double.MAX_VALUE,
				"Pressure impulse in N*s/m^2 per Minecraft damage point. This makes close muzzle blast trauma scale with local gas momentum density.");
		MUZZLE_BLAST_KINETIC_ENERGY_PER_DAMAGE = defineDouble("kineticEnergyPerDamage", 250.0D, 0.0D, Double.MAX_VALUE,
				"Kinetic energy in joules transferred to an entity per Minecraft damage point from sudden blast acceleration.");
		MUZZLE_BLAST_THERMAL_DOSE_PER_DAMAGE = defineDouble("thermalDosePerDamage", 30_000.0D, 0.0D, Double.MAX_VALUE,
				"Hot-gas heat dose in J/m^2 per Minecraft damage point before ignition/fire damage is considered.");
		MUZZLE_BLAST_ENTITY_DRAG_COEFFICIENT = defineDouble("entityDragCoefficient", 1.1D, 0.0D, Double.MAX_VALUE,
				"Drag/transmittance coefficient for entities in muzzle gas. Larger values make entities absorb more of each gas packet and leave less wake behind them.");
		MUZZLE_BLAST_POWDER_SPECIFIC_ENERGY = defineDouble("powderSpecificEnergyJPerKg", 3_000_000.0D, 0.0D, Double.MAX_VALUE,
				"Chemical energy of the propellant used for thermal muzzle blast calculations.");
		MUZZLE_BLAST_THERMAL_EFFICIENCY = defineDouble("thermalEfficiency", 0.35D, 0.0D, Double.MAX_VALUE,
				"Fraction of propellant chemical energy initially available as hot-gas thermal energy before projectile/gas kinetic losses.");
		MUZZLE_BLAST_GAS_CP = defineDouble("gasCpJPerKgK", 1050.0D, 0.0D, Double.MAX_VALUE,
				"Propellant-gas constant-pressure specific heat used for heat transfer.");
		MUZZLE_BLAST_GAS_CV = defineDouble("gasCvJPerKgK", 750.0D, 0.0D, Double.MAX_VALUE,
				"Propellant-gas constant-volume specific heat used for gas temperature.");
		MUZZLE_BLAST_AMBIENT_TEMPERATURE = defineDouble("ambientTemperatureK", 293.0D, 0.0D, Double.MAX_VALUE,
				"Ambient air temperature in kelvin for gas expansion/mixing.");
		MUZZLE_BLAST_BODY_TEMPERATURE = defineDouble("bodyTemperatureK", 310.0D, 0.0D, Double.MAX_VALUE,
				"Target body/surface reference temperature in kelvin for heat-flow direction.");
		MUZZLE_BLAST_TEMPERATURE_MIXING_EXPONENT = defineDouble("temperatureMixingExponent", 0.55D, 0.0D, Double.MAX_VALUE,
				"Exponent for cooling by conical expansion/mixing: T = ambient + (T0-ambient)*(boreArea/jetArea)^exponent.");
		MUZZLE_BLAST_HEAT_TRANSFER_EFFICIENCY = defineDouble("heatTransferEfficiency", 0.06D, 0.0D, Double.MAX_VALUE,
				"Fraction of hot gas sensible heat transferred into caught entities per gas tick.");
		MUZZLE_BLAST_IGNITION_TEMPERATURE = defineDouble("ignitionTemperatureK", 800.0D, 0.0D, Double.MAX_VALUE,
				"Local gas temperature required before accumulated heat dose can ignite an entity.");
		MUZZLE_BLAST_IGNITION_DOSE = defineDouble("ignitionDoseJPerM2", 20_000.0D, 0.0D, Double.MAX_VALUE,
				"Accumulated heat dose per square meter required before an entity ignites.");
		MUZZLE_BLAST_BURN_DOSE_PER_SECOND = defineDouble("burnDosePerSecondJPerM2", 10_000.0D, 0.0D, Double.MAX_VALUE,
				"Excess heat dose per square meter required to add one second of burning.");
		MUZZLE_SHOCK_EFFICIENCY = defineDouble("shockEfficiency", 0.08D, 0.0D, Double.MAX_VALUE,
				"Fraction of propellant thermal energy converted into the primary muzzle overpressure shockwave.");
		MUZZLE_SHOCK_IMPULSE_SCALE = defineDouble("shockImpulseScale", 1.0D, 0.0D, Double.MAX_VALUE,
				"Direct multiplier for muzzle shock pressure impulse and peak pressure after propellant-energy scaling. Use this to scale shockwave impulse per powder charge without changing blast range, heat, or gas-jet recoil.");
		MUZZLE_SHOCK_RANGE = defineDouble("shockRangeBlocks", 20.0D, 0.0D, Double.MAX_VALUE,
				"Maximum path length searched by primary and reflected muzzle shock rays. This is a computational boundary, not a force clamp.");
		MUZZLE_SHOCK_RAY_COUNT = defineInt("shockRayCount", 384, 1, Integer.MAX_VALUE,
				"Number of quasi-uniform spherical rays used to integrate the muzzle overpressure shockwave.");
		MUZZLE_SHOCK_FORWARD_BIAS = defineDouble("shockForwardBias", 0.35D, 0.0D, Double.MAX_VALUE,
				"Extra energy weighting for shock rays traveling with the cannon axis. 0 is spherical; larger values emphasize the forward near-field muzzle blast.");
		MUZZLE_SHOCK_AIR_DENSITY = defineDouble("shockAirDensityKgPerM3", 1.225D, 0.0D, Double.MAX_VALUE,
				"Air density used by the acoustic impulse relation E/A = I^2/(rho*c*tau).");
		MUZZLE_SHOCK_SOUND_SPEED = defineDouble("shockSoundSpeedMps", 343.0D, 0.0D, Double.MAX_VALUE,
				"Speed of sound used by the acoustic impulse relation E/A = I^2/(rho*c*tau).");
		MUZZLE_SHOCK_POSITIVE_PHASE_SECONDS = defineDouble("shockPositivePhaseSeconds", 0.004D, 0.0D, Double.MAX_VALUE,
				"Duration of the initial positive overpressure phase in seconds for pressure and damage calculations.");
		MUZZLE_SHOCK_PRESSURE_IMPULSE_PER_DAMAGE = defineDouble("shockPressureImpulsePerDamage", 120.0D, 0.0D, Double.MAX_VALUE,
				"Shock pressure impulse in Pa*s per Minecraft damage point.");
		MUZZLE_SHOCK_PEAK_PRESSURE_PER_DAMAGE = defineDouble("shockPeakPressurePerDamage", 80_000.0D, 0.0D, Double.MAX_VALUE,
				"Shock peak overpressure in Pa per Minecraft damage point.");
		MUZZLE_SHOCK_SOLID_REFLECTION_COEFFICIENT = defineDouble("shockSolidReflectionCoefficient", 1.1D, 0.0D, Double.MAX_VALUE,
				"Exponential reflection coefficient for solid blocks and sublevel block surfaces hit by the muzzle shock. Higher values reflect more shock energy.");
		MUZZLE_SHOCK_ENTITY_ABSORPTION_COEFFICIENT = defineDouble("shockEntityAbsorptionCoefficient", 0.12D, 0.0D, Double.MAX_VALUE,
				"Exponential absorption coefficient for entity bodies in the shockwave. Entities mostly transmit overpressure, but still absorb damaging momentum.");
		MUZZLE_SHOCK_ENTITY_REFLECTION_COEFFICIENT = defineDouble("shockEntityReflectionCoefficient", 0.035D, 0.0D, Double.MAX_VALUE,
				"Exponential reflection coefficient for entity bodies in the shockwave. This lets entities reflect some overpressure without acting like solid walls.");
		MUZZLE_SHOCK_MAX_REFLECTIONS = defineInt("shockMaxReflections", 2, 0, Integer.MAX_VALUE,
				"Enables first-order image-source shock reflections when greater than 0. Higher-order recursive reflections are intentionally not simulated for performance.");
		MUZZLE_SHOCK_MAX_REFLECTION_SOURCES = defineInt("shockMaxReflectionSources", 48, 0, Integer.MAX_VALUE,
				"Maximum number of image-source reflected shock emitters retained after the initial wall scan. Lower values greatly reduce enclosed-space lag spikes.");
		MUZZLE_SHOCK_MIN_REFLECTION_ENERGY_FRACTION = defineDouble("shockMinReflectionEnergyFraction", 0.001D, 0.0D, Double.MAX_VALUE,
				"Reflected shock sources below this fractional energy are discarded. This removes tiny reflection paths that cost CPU but have little gameplay effect.");
		MUZZLE_SHOCK_MAX_SUBLEVEL_SURFACE_CONTACTS = defineInt("shockMaxSubLevelSurfaceContacts", 96, 0, Integer.MAX_VALUE,
				"Maximum number of cached direct shock/sublevel surface contacts retained per cannon blast. Lower values reduce enclosed-space cost; higher values improve force distribution on complex hollow vehicles.");
		MUZZLE_SHOCK_MAX_SUBLEVEL_CONTACTS_PER_BODY = defineInt("shockMaxSubLevelContactsPerBody", 16, 0, Integer.MAX_VALUE,
				"Maximum number of cached direct shock/sublevel surface contacts retained for any single sublevel body per cannon blast.");
		MUZZLE_SHOCK_NEGATIVE_PHASE_FRACTION = defineDouble("shockNegativePhaseFraction", 0.30D, 0.0D, Double.MAX_VALUE,
				"Fraction of the positive shock energy represented by the trailing negative-pressure suction phase.");
		MUZZLE_SHOCK_NEGATIVE_PHASE_DELAY_TICKS = defineInt("shockNegativePhaseDelayTicks", 1, 0, Integer.MAX_VALUE,
				"Ticks after the positive shock to apply the negative-pressure phase. If the gas blast ends sooner, it is applied on the final active blast tick.");
		MUZZLE_SHOCK_NEGATIVE_PHASE_DURATION_MULTIPLIER = defineDouble("shockNegativePhaseDurationMultiplier", 3.0D, 0.0D, Double.MAX_VALUE,
				"Negative pressure phase duration as a multiple of the positive phase duration. Longer duration lowers peak suction for the same phase energy.");
		BUILDER.pop();

		SPEC = BUILDER.build();
	}
	private static ModConfigSpec.DoubleValue defineDouble(String name, double value, double min, double max, String comment) {
		return BUILDER.comment(comment).defineInRange(name, value, min, max);
	}
	private static ModConfigSpec.IntValue defineInt(String name, int value, int min, int max, String comment) {
		return BUILDER.comment(comment).defineInRange(name, value, min, max);
	}

	public static boolean disableRecoil() { return DISABLE_RECOIL.get(); }
	public static boolean disableImpact() { return DISABLE_IMPACT.get(); }
	public static boolean disableMuzzleBlast() { return DISABLE_MUZZLE_BLAST.get(); }
	public static boolean debugImpulses() { return DEBUG_IMPULSES.get(); }

	public static double impulseScale() { return IMPULSE_SCALE.get(); }

	public static double projectileMomentumMultiplier() { return PROJECTILE_MOMENTUM_MULTIPLIER.get(); }
	public static double gasMomentumMultiplier() { return GAS_MOMENTUM_MULTIPLIER.get(); }
	public static double gasVelocityMultiplier() { return GAS_VELOCITY_MULTIPLIER.get(); }
	public static double recoilApplicationOffset() { return RECOIL_APPLICATION_OFFSET.get(); }

	public static double baseBigCannonProjectileMass() { return BASE_CANNON_PROJECTILE_MASS.get(); }
	public static double baseAutocannonProjectileMass() { return BASE_AUTOCANNON_PROJECTILE_MASS.get(); }
	public static double baseCannonGasImpulse() { return BASE_CANNON_GAS_IMPULSE.get(); }
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

	public static double muzzleBlastBigCannonHalfAngleDegrees() { return MUZZLE_BLAST_CANNON_HALF_ANGLE_DEGREES.get(); }
	public static double muzzleBlastAutocannonHalfAngleDegrees() { return MUZZLE_BLAST_AUTOCANNON_HALF_ANGLE_DEGREES.get(); }
	public static double muzzleBlastProfileExponent() { return MUZZLE_BLAST_PROFILE_EXPONENT.get(); }
	public static double muzzleBlastCoupling() { return MUZZLE_BLAST_COUPLING.get(); }
	public static double muzzleBlastBigCannonBoreRadius() { return MUZZLE_BLAST_CANNON_BORE_RADIUS.get(); }
	public static double muzzleBlastAutocannonBoreRadius() { return MUZZLE_BLAST_AUTOCANNON_BORE_RADIUS.get(); }
	public static double muzzleBlastCalculationRange() { return MUZZLE_BLAST_CALCULATION_RANGE.get(); }
	public static double muzzleBlastEntityDensity() { return MUZZLE_BLAST_ENTITY_DENSITY.get(); }
	public static double muzzleBlastForceDosePerDamage() { return MUZZLE_BLAST_FORCE_DOSE_PER_DAMAGE.get(); }
	public static double muzzleBlastPressureImpulsePerDamage() { return MUZZLE_BLAST_PRESSURE_IMPULSE_PER_DAMAGE.get(); }
	public static double muzzleBlastKineticEnergyPerDamage() { return MUZZLE_BLAST_KINETIC_ENERGY_PER_DAMAGE.get(); }
	public static double muzzleBlastThermalDosePerDamage() { return MUZZLE_BLAST_THERMAL_DOSE_PER_DAMAGE.get(); }
	public static double muzzleBlastEntityDragCoefficient() { return MUZZLE_BLAST_ENTITY_DRAG_COEFFICIENT.get(); }
	public static double muzzleBlastPowderSpecificEnergy() { return MUZZLE_BLAST_POWDER_SPECIFIC_ENERGY.get(); }
	public static double muzzleBlastThermalEfficiency() { return MUZZLE_BLAST_THERMAL_EFFICIENCY.get(); }
	public static double muzzleBlastGasCp() { return MUZZLE_BLAST_GAS_CP.get(); }
	public static double muzzleBlastGasCv() { return MUZZLE_BLAST_GAS_CV.get(); }
	public static double muzzleBlastAmbientTemperature() { return MUZZLE_BLAST_AMBIENT_TEMPERATURE.get(); }
	public static double muzzleBlastBodyTemperature() { return MUZZLE_BLAST_BODY_TEMPERATURE.get(); }
	public static double muzzleBlastTemperatureMixingExponent() { return MUZZLE_BLAST_TEMPERATURE_MIXING_EXPONENT.get(); }
	public static double muzzleBlastHeatTransferEfficiency() { return MUZZLE_BLAST_HEAT_TRANSFER_EFFICIENCY.get(); }
	public static double muzzleBlastIgnitionTemperature() { return MUZZLE_BLAST_IGNITION_TEMPERATURE.get(); }
	public static double muzzleBlastIgnitionDose() { return MUZZLE_BLAST_IGNITION_DOSE.get(); }
	public static double muzzleBlastBurnDosePerSecond() { return MUZZLE_BLAST_BURN_DOSE_PER_SECOND.get(); }
	public static double muzzleShockEfficiency() { return MUZZLE_SHOCK_EFFICIENCY.get(); }
	public static double muzzleShockImpulseScale() { return MUZZLE_SHOCK_IMPULSE_SCALE.get(); }
	public static double muzzleShockRange() { return MUZZLE_SHOCK_RANGE.get(); }
	public static int muzzleShockRayCount() { return MUZZLE_SHOCK_RAY_COUNT.get(); }
	public static double muzzleShockForwardBias() { return MUZZLE_SHOCK_FORWARD_BIAS.get(); }
	public static double muzzleShockAirDensity() { return MUZZLE_SHOCK_AIR_DENSITY.get(); }
	public static double muzzleShockSoundSpeed() { return MUZZLE_SHOCK_SOUND_SPEED.get(); }
	public static double muzzleShockPositivePhaseSeconds() { return MUZZLE_SHOCK_POSITIVE_PHASE_SECONDS.get(); }
	public static double muzzleShockPressureImpulsePerDamage() { return MUZZLE_SHOCK_PRESSURE_IMPULSE_PER_DAMAGE.get(); }
	public static double muzzleShockPeakPressurePerDamage() { return MUZZLE_SHOCK_PEAK_PRESSURE_PER_DAMAGE.get(); }
	public static double muzzleShockSolidReflectionCoefficient() { return MUZZLE_SHOCK_SOLID_REFLECTION_COEFFICIENT.get(); }
	public static double muzzleShockEntityAbsorptionCoefficient() { return MUZZLE_SHOCK_ENTITY_ABSORPTION_COEFFICIENT.get(); }
	public static double muzzleShockEntityReflectionCoefficient() { return MUZZLE_SHOCK_ENTITY_REFLECTION_COEFFICIENT.get(); }
	public static int muzzleShockMaxReflections() { return MUZZLE_SHOCK_MAX_REFLECTIONS.get(); }
	public static int muzzleShockMaxReflectionSources() { return MUZZLE_SHOCK_MAX_REFLECTION_SOURCES.get(); }
	public static double muzzleShockMinReflectionEnergyFraction() { return MUZZLE_SHOCK_MIN_REFLECTION_ENERGY_FRACTION.get(); }
	public static int muzzleShockMaxSubLevelSurfaceContacts() { return MUZZLE_SHOCK_MAX_SUBLEVEL_SURFACE_CONTACTS.get(); }
	public static int muzzleShockMaxSubLevelContactsPerBody() { return MUZZLE_SHOCK_MAX_SUBLEVEL_CONTACTS_PER_BODY.get(); }
	public static double muzzleShockNegativePhaseFraction() { return MUZZLE_SHOCK_NEGATIVE_PHASE_FRACTION.get(); }
	public static int muzzleShockNegativePhaseDelayTicks() { return MUZZLE_SHOCK_NEGATIVE_PHASE_DELAY_TICKS.get(); }
	public static double muzzleShockNegativePhaseDurationMultiplier() { return MUZZLE_SHOCK_NEGATIVE_PHASE_DURATION_MULTIPLIER.get(); }

}
