package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachment;
import org.hectoris919.cbc_equally_opposite.physics.ImpactImpulseHandler;
import org.hectoris919.cbc_equally_opposite.physics.ImpactSnapshotStore;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

@Mixin(value = AbstractBigCannonProjectile.class, priority = 1500)
public abstract class AbstractBigCannonProjectileMixin extends AbstractCannonProjectile {
	protected AbstractBigCannonProjectileMixin(EntityType type, Level level) {
		super(type, level);
	}

	@Inject(method = "calculateBlockPenetration", at = @At("HEAD"), cancellable = true, require = 0, order = 0)
	private void cbeo$captureImpactBefore(ProjectileContext projectileContext, BlockState state, BlockHitResult blockHitResult, CallbackInfoReturnable<AbstractCannonProjectile.ImpactResult> cir) {
		if (SableProjectileAttachment.isAttached(this)) {
			cir.setReturnValue(new AbstractCannonProjectile.ImpactResult(AbstractCannonProjectile.ImpactResult.KinematicOutcome.STOP, false));
			ImpactSnapshotStore.remove(this);
			return;
		}

		Vec3 velocity = this.getDeltaMovement();
		try {
			velocity = velocity.add(this.getForces(this.position(), this.getDeltaMovement()));
		} catch (Throwable ignored) { }

		ImpactSnapshotStore.put(this, ImpactImpulseHandler.capture(this, state, blockHitResult, velocity, false));
	}
}
