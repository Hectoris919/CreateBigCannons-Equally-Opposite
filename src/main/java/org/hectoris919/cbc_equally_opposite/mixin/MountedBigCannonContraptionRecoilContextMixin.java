package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.MountedCannonContextTracker;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

@Mixin(MountedBigCannonContraption.class)
public abstract class MountedBigCannonContraptionRecoilContextMixin {
	@Inject(method = "fireShot", at = @At("HEAD"), require = 0)
	private void cbeo$enterRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		MountedCannonContextTracker.enter(this);
	}

	@Inject(method = "fireShot", at = @At("RETURN"), require = 0)
	private void cbeo$exitRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		MountedCannonContextTracker.exit();
	}
}
