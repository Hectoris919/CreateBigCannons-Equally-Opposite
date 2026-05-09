package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.ProjectileChargeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileChargeMixin {
	@Inject(method = "setChargePower(F)V", at = @At("HEAD"), require = 0)
	private void cbeo$recordChargePower(float power, CallbackInfo ci) {
		ProjectileChargeTracker.record((AbstractCannonProjectile) (Object) this, power);
	}
}
