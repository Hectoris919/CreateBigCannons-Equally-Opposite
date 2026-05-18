package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.MountedCannonContextTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;

@Mixin(MountedAutocannonContraption.class)
public abstract class MountedAutocannonContraptionRecoilContextMixin extends AbstractMountedCannonContraption {
	@Inject(method = "fireShot", at = @At("HEAD"), require = 0)
	private void cbeo$enterRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		MountedCannonContextTracker.enter(this, this.cbeo$estimateMuzzleEndPosition(entity));
	}

	@Inject(method = "fireShot", at = @At("RETURN"), require = 0)
	private void cbeo$exitRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		MountedCannonContextTracker.exit();
	}

	@Unique
	private Vec3 cbeo$estimateMuzzleEndPosition(PitchOrientedContraptionEntity entity) {
		if (this.startPos == null || this.initialOrientation == null || entity == null) return null;
		BlockPos currentPos = this.startPos.immutable();
		BlockPos lastCannonPos = null;
		int safety = 0;
		while (this.blocks.containsKey(currentPos) && safety++ < 512) {
			lastCannonPos = currentPos.immutable();
			currentPos = currentPos.relative(this.initialOrientation);
		}
		if (lastCannonPos == null) return null;

		Vec3 localForward = new Vec3(this.initialOrientation.getStepX(), this.initialOrientation.getStepY(), this.initialOrientation.getStepZ());
		if (!Double.isFinite(localForward.x) || !Double.isFinite(localForward.y) || !Double.isFinite(localForward.z) || localForward.lengthSqr() < 1.0E-8D) return null;
		Vec3 localMuzzleEnd = Vec3.atCenterOf(lastCannonPos).add(localForward.normalize().scale(0.501D));
		Vec3 globalMuzzleEnd = entity.toGlobalVector(localMuzzleEnd, 0);
		return Double.isFinite(globalMuzzleEnd.x) && Double.isFinite(globalMuzzleEnd.y) && Double.isFinite(globalMuzzleEnd.z) ? globalMuzzleEnd : null;
	}
}
