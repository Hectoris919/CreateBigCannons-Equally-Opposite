package org.hectoris919.cbc_equally_opposite.registry;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EOForceGroups {
	private EOForceGroups() { }

	public static final DeferredRegister<ForceGroup> FORCE_GROUPS =
			DeferredRegister.create(ForceGroups.REGISTRY_KEY, EquallyOpposite.MODID);

	public static final DeferredHolder<ForceGroup, ForceGroup> RECOIL = FORCE_GROUPS.register(
			"recoil",
			() -> new ForceGroup(
					Component.translatable("force_group.cbc_equally_opposite.recoil"),
					Component.translatable("force_group.cbc_equally_opposite.recoil.desc"),
					0xD07A2D,
					true
			)
	);

	public static final DeferredHolder<ForceGroup, ForceGroup> IMPACT = FORCE_GROUPS.register(
			"impact",
			() -> new ForceGroup(
					Component.translatable("force_group.cbc_equally_opposite.impact"),
					Component.translatable("force_group.cbc_equally_opposite.impact.desc"),
					0xB83A3A,
					true
			)
	);

	public static void register(IEventBus modEventBus) {
		FORCE_GROUPS.register(modEventBus);
	}
}
