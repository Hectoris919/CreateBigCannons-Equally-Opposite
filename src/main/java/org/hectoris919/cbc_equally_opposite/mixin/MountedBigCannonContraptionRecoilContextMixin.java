package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.physics.MountedCannonContextTracker;
import org.hectoris919.cbc_equally_opposite.physics.RecoilImpulseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBehavior;
import rbasamoyai.createbigcannons.cannons.big_cannons.BigCannonBlock;
import rbasamoyai.createbigcannons.cannons.big_cannons.IBigCannonBlockEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.cannon_end.BigCannonEnd;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCannonPropellantBlock;

@Mixin(MountedBigCannonContraption.class)
public abstract class MountedBigCannonContraptionRecoilContextMixin extends AbstractMountedCannonContraption {
	@Unique private BlankFireEstimate cbeo$pendingBlankFireEstimate;

	@Inject(method = "fireShot", at = @At("HEAD"), require = 0)
	private void cbeo$enterRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		MountedCannonContextTracker.enter(this);
		this.cbeo$pendingBlankFireEstimate = this.cbeo$estimateBlankFire(level, entity);
	}

	@Inject(
			method = "fireShot",
			at = @At(
					value = "FIELD",
					target = "Lrbasamoyai/createbigcannons/cannon_control/contraption/MountedBigCannonContraption;hasFired:Z",
					opcode = Opcodes.PUTFIELD,
					shift = At.Shift.AFTER
			),
			require = 0
	)
	private void cbeo$applyBlankRecoilOnSuccessfulFire(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		BlankFireEstimate estimate = this.cbeo$pendingBlankFireEstimate;
		if (estimate != null) {
			RecoilImpulseHandler.onCannonBlankFire(level, estimate.forward(), estimate.projectileSpawnPosition(), estimate.chargePower(), estimate.barrelLengthBlocks());
			this.cbeo$pendingBlankFireEstimate = null;
		}
	}

	@Inject(method = "fireShot", at = @At("RETURN"), require = 0)
	private void cbeo$exitRecoilContext(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		this.cbeo$pendingBlankFireEstimate = null;
		MountedCannonContextTracker.exit();
	}

	@Unique
	private BlankFireEstimate cbeo$estimateBlankFire(ServerLevel level, PitchOrientedContraptionEntity entity) {
		if (this.startPos == null || this.initialOrientation == null) return null;

		BlockPos currentPos = this.startPos.immutable();
		int openEndCount = 0;
		double chargePower = 0.0D;
		double barrelTravelled = 0.0D;
		boolean hasSeenAirGap;

		while (this.presentBlockEntities.get(currentPos) instanceof IBigCannonBlockEntity cbe) {
			BigCannonBehavior behavior = cbe.cannonBehavior();
			StructureBlockInfo containedBlockInfo = behavior.block();
			StructureBlockInfo cannonInfo = this.blocks.get(currentPos);
			if (cannonInfo == null) break;

			Block block = containedBlockInfo.state().getBlock();
			if (containedBlockInfo.state().isAir()) {
				if (openEndCount == 0) return null;
				chargePower = Math.max(chargePower - 1.0D, 0.0D);
				hasSeenAirGap = true;
			} else if (block instanceof BigCannonPropellantBlock propellant && !(block instanceof ProjectileBlock)) {
				chargePower += Math.max(0.0D, propellant.getChargePower(containedBlockInfo));
				hasSeenAirGap = false;
			} else if (block instanceof ProjectileBlock) {
				return null;
			} else {
				return null;
			}

			currentPos = currentPos.relative(this.initialOrientation);
			BlockState cannonState = cannonInfo.state();
			if (cannonState.getBlock() instanceof BigCannonBlock cannon && cannon.getOpeningType(level, cannonState, currentPos) == BigCannonEnd.OPEN) {
				++openEndCount;
			}
			if (!hasSeenAirGap) {
			++barrelTravelled;
			}
		}

		if (chargePower <= 0.0D) return null;

		Vec3 projectileSpawnPosition = entity.toGlobalVector(Vec3.atCenterOf(currentPos.relative(this.initialOrientation)), 0);
		Vec3 centerPosition = entity.toGlobalVector(Vec3.atCenterOf(BlockPos.ZERO), 0);
		Vec3 forward = projectileSpawnPosition.subtract(centerPosition);
		if (!Double.isFinite(forward.x) || !Double.isFinite(forward.y) || !Double.isFinite(forward.z) || forward.lengthSqr() < 1.0E-8D) {
			return null;
		}
		forward = forward.normalize();
		projectileSpawnPosition = projectileSpawnPosition.subtract(forward.scale(2.0D));

		Double trackedBarrelLength = MountedCannonContextTracker.currentBarrelLengthBlocks();
		double barrelLengthBlocks = trackedBarrelLength != null && Double.isFinite(trackedBarrelLength) && trackedBarrelLength > 0.0D
				? trackedBarrelLength
				: Math.max(1.0D, barrelTravelled);

		return new BlankFireEstimate(chargePower, barrelLengthBlocks, forward, projectileSpawnPosition);
	}

	@Unique
	private record BlankFireEstimate(double chargePower, double barrelLengthBlocks, Vec3 forward, Vec3 projectileSpawnPosition) { }
}
