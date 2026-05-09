package org.hectoris919.cbc_equally_opposite.physics;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PendingImpulseQueue {
	private static final Map<ResourceKey<Level>, Queue<PendingPointImpulse>> BY_DIMENSION = new ConcurrentHashMap<>();

	private PendingImpulseQueue() { }

	public static boolean enqueueForContaining(ServerLevel level, Vec3 pointInSubLevelSpace, Vec3 totalImpulse, ImpulseKind kind, int steps) {
		if (!ImpulseMath.isUsefulVector(totalImpulse)) return false;

		SubLevelAccess containing = SableCompanion.INSTANCE.getContaining(level, pointInSubLevelSpace);
		if (containing == null) return false;

		if (!ImpulseMath.isUsefulVector(totalImpulse)) return false;

		int safeSteps = Math.max(1, steps);
		Vec3 impulsePerStep = totalImpulse.scale(1.0D / safeSteps);
		PendingPointImpulse pending = new PendingPointImpulse(
				containing.getUniqueId(),
				pointInSubLevelSpace,
				impulsePerStep,
				kind,
				safeSteps
		);

		enqueue(level.dimension(), pending);

		if (Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"Queued {} impulse for sublevel {} at {}: sourceTotal={}, perStep={}, steps={}",
					kind,
					containing.getUniqueId(),
					pointInSubLevelSpace,
					totalImpulse,
					impulsePerStep,
					safeSteps
			);
		}

		return true;
	}

	public static void enqueue(ResourceKey<Level> dimension, PendingPointImpulse impulse) {
		BY_DIMENSION.computeIfAbsent(dimension, ignored -> new ConcurrentLinkedQueue<>()).add(impulse);
	}

	public static List<PendingPointImpulse> drain(ResourceKey<Level> dimension) {
		Queue<PendingPointImpulse> queue = BY_DIMENSION.get(dimension);
		if (queue == null || queue.isEmpty()) return List.of();

		List<PendingPointImpulse> drained = new ArrayList<>();
		PendingPointImpulse impulse;
		while ((impulse = queue.poll()) != null) {
			drained.add(impulse);
		}
		return drained;
	}
}
