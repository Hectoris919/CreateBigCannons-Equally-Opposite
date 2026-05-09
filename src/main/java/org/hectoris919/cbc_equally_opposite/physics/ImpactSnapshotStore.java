package org.hectoris919.cbc_equally_opposite.physics;

import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ImpactSnapshotStore {
	private static final Map<AbstractCannonProjectile, ImpactSnapshot> SNAPSHOTS = Collections.synchronizedMap(new WeakHashMap<>());

	private ImpactSnapshotStore() { }

	public static void put(AbstractCannonProjectile projectile, ImpactSnapshot snapshot) {
		if (projectile == null || snapshot == null) return;
		SNAPSHOTS.put(projectile, snapshot);
	}

	public static ImpactSnapshot remove(AbstractCannonProjectile projectile) {
		if (projectile == null) return null;
		return SNAPSHOTS.remove(projectile);
	}
}
