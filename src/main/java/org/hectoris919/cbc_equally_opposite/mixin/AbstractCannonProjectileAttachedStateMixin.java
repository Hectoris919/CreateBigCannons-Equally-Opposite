package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileAttachedStateMixin {
	@Inject(method = "shouldFall", at = @At("HEAD"), cancellable = true, require = 0)
	private void cbeo$doNotFallWhileSableAttached(CallbackInfoReturnable<Boolean> cir) {
		AbstractCannonProjectile projectile = (AbstractCannonProjectile) (Object) this;
		if (!SableProjectileAttachment.isAttached(projectile)) return;

		if (SableProjectileAttachment.shouldStayAttached(projectile)) {
			cir.setReturnValue(false);
		} else {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "startFalling", at = @At("HEAD"), cancellable = true, require = 0)
	private void cbeo$doNotStartFallingWhileSableAttached(CallbackInfo ci) {
		AbstractCannonProjectile projectile = (AbstractCannonProjectile) (Object) this;
		if (SableProjectileAttachment.shouldStayAttached(projectile)) ci.cancel();
	}
}
