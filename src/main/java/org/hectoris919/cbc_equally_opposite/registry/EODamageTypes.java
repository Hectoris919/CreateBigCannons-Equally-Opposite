package org.hectoris919.cbc_equally_opposite.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;

public final class EODamageTypes {
	public static final ResourceKey<DamageType> CANNON_BLAST_BURN = key("cannon_blast_burn");
	public static final ResourceKey<DamageType> CANNON_BLAST_GAS = key("cannon_blast_gas");
	public static final ResourceKey<DamageType> CANNON_BLAST_SHOCK = key("cannon_blast_shock");

	private EODamageTypes() { }

	public static DamageSource cannonBlastBurn(ServerLevel level) {
		return source(level, CANNON_BLAST_BURN);
	}

	public static DamageSource cannonBlastGas(ServerLevel level) {
		return source(level, CANNON_BLAST_GAS);
	}

	public static DamageSource cannonBlastShock(ServerLevel level) {
		return source(level, CANNON_BLAST_SHOCK);
	}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, EquallyOpposite.id(name));
	}

	private static DamageSource source(ServerLevel level, ResourceKey<DamageType> key) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
	}
}
