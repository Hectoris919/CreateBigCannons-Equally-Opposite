package org.hectoris919.cbc_equally_opposite.compat.sable;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import org.hectoris919.cbc_equally_opposite.physics.ImpulseKind;
import org.hectoris919.cbc_equally_opposite.physics.MuzzleBlastHandler;
import org.hectoris919.cbc_equally_opposite.physics.PendingImpulseQueue;
import org.hectoris919.cbc_equally_opposite.physics.PendingPointImpulse;
import org.hectoris919.cbc_equally_opposite.registry.EOForceGroups;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.List;

public final class SableImpulseEvents {
	private SableImpulseEvents() { }

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) MuzzleBlastHandler.tick(serverLevel);
	}

	@SubscribeEvent
	public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
		ServerLevel level = event.getPhysicsSystem().getLevel();
		List<PendingPointImpulse> impulses = PendingImpulseQueue.drain(level.dimension());
		if (impulses.isEmpty()) return;

		ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container == null) return;

		int applied = 0;
		for (PendingPointImpulse impulse : impulses) {
			SubLevel subLevel = container.getSubLevel(impulse.subLevelId());
			if (!(subLevel instanceof ServerSubLevel serverSubLevel) || serverSubLevel.isRemoved()) continue;

			ForceGroup forceGroup = impulse.kind() == ImpulseKind.RECOIL
					? EOForceGroups.RECOIL.get()
					: EOForceGroups.IMPACT.get();

			QueuedForceGroup queuedForceGroup = serverSubLevel.getOrCreateQueuedForceGroup(forceGroup);
			queuedForceGroup.applyAndRecordPointForce(
					JOMLConversion.toJOML(impulse.pointInSubLevelSpace()),
					JOMLConversion.toJOML(impulse.impulseInSubLevelSpace())
			);
			applied++;

			if (impulse.remainingSteps() > 1) PendingImpulseQueue.enqueue(level.dimension(), impulse.nextStep());
		}

		if (Config.debugImpulses() && applied > 0) EquallyOpposite.LOGGER.info("Applied {} pending Equally Opposite impulse(s) during Sable pre-physics tick", applied);
	}
}
