package org.hectoris919.cbc_equally_opposite.registry;

import net.minecraft.world.level.GameRules;

public final class EOGameRules {
	public static final GameRules.Key<GameRules.BooleanValue> CANNON_BLAST_DAMAGE = GameRules.register(
			"cannonBlastDamage",
			GameRules.Category.MISC,
			GameRules.BooleanValue.create(true)
	);

	private EOGameRules() { }

	public static void register() { }
}
