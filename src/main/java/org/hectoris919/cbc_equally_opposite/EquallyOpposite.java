package org.hectoris919.cbc_equally_opposite;

import com.mojang.logging.LogUtils;
import org.hectoris919.cbc_equally_opposite.compat.sable.SableImpulseEvents;
import org.hectoris919.cbc_equally_opposite.registry.EOForceGroups;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EquallyOpposite.MODID)
public final class EquallyOpposite {
	public static final String MODID = "cbc_equally_opposite";
	public static final Logger LOGGER = LogUtils.getLogger();

	public EquallyOpposite(IEventBus modEventBus, ModContainer modContainer) {
		EOForceGroups.register(modEventBus);
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
		NeoForge.EVENT_BUS.register(SableImpulseEvents.class);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
