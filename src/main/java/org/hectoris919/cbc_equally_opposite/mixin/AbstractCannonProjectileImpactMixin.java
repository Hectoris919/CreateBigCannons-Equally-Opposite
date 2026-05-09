package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.ImpactImpulseHandler;
import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachment;
import org.hectoris919.cbc_equally_opposite.physics.ImpactSnapshot;
import org.hectoris919.cbc_equally_opposite.physics.ImpactSnapshotStore;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileImpactMixin {
	@Inject(
			method = "onImpact(Lnet/minecraft/world/phys/HitResult;Lrbasamoyai/createbigcannons/munitions/AbstractCannonProjectile$ImpactResult;Lrbasamoyai/createbigcannons/munitions/ProjectileContext;)Z",
			at = @At("RETURN"),
			require = 0
	)
	private void cbeo$applyStoredBlockImpact(HitResult hitResult, AbstractCannonProjectile.ImpactResult impactResult, ProjectileContext projectileContext, CallbackInfoReturnable<Boolean> cir) {
		if (!(hitResult instanceof BlockHitResult)) return;

		AbstractCannonProjectile projectile = (AbstractCannonProjectile) (Object) this;
		ImpactSnapshot snapshot = ImpactSnapshotStore.remove(projectile);

		if (SableProjectileAttachment.isAttached(projectile)) return;
		if (snapshot == null) SableProjectileAttachment.attachIfStoppedFromResult(projectile, (BlockHitResult) hitResult, projectile.getDeltaMovement(), impactResult);

		ImpactImpulseHandler.applyFromCurrentProjectileState(projectile, snapshot, impactResult);
	}
}
