package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.ImpactImpulseHandler;
import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachment;
import org.hectoris919.cbc_equally_opposite.physics.ImpactSnapshot;
import org.hectoris919.cbc_equally_opposite.physics.ImpactSnapshotStore;
import org.hectoris919.cbc_equally_opposite.physics.RecoilImpulseHandler;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

@Mixin(Projectile.class)
public abstract class ProjectileRecoilMixin {
	@Inject(method = "shoot(DDDFF)V", at = @At("RETURN"), require = 0)
	private void cbeo$onShoot(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
		Projectile projectile = (Projectile) (Object) this;
		if (projectile instanceof AbstractAutocannonProjectile autocannonProjectile) {
			RecoilImpulseHandler.onProjectileShot(autocannonProjectile, true);
		} else if (projectile instanceof AbstractBigCannonProjectile bigCannonProjectile) {
			RecoilImpulseHandler.onProjectileShot(bigCannonProjectile, false);
		}
	}

	@Inject(method = "tick", at = @At("RETURN"), require = 0)
	private void cbeo$applyOrphanedImpactSnapshot(CallbackInfo ci) {
		Projectile projectile = (Projectile) (Object) this;
		if (!(projectile instanceof AbstractCannonProjectile cannonProjectile)) return;

		SableProjectileAttachment.tick(cannonProjectile);

		ImpactSnapshot snapshot = ImpactSnapshotStore.remove(cannonProjectile);
		if (snapshot != null && !SableProjectileAttachment.isAttached(cannonProjectile)) {
			ImpactImpulseHandler.applyFromCurrentProjectileState(cannonProjectile, snapshot, null);
		}
	}
}
