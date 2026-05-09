package org.hectoris919.cbc_equally_opposite.compat.sable;

import net.minecraft.world.phys.Vec3;

public interface SableProjectileAttachmentState {
	boolean cbeo$isSableProjectileAttached();
	void cbeo$setSableProjectileAttachment(boolean attached, Vec3 plotPosition, Vec3 localDirection);
	Vec3 cbeo$getSableProjectilePlotPosition();
	Vec3 cbeo$getSableProjectileLocalDirection();
}
