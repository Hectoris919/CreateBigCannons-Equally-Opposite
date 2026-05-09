package org.hectoris919.cbc_equally_opposite.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import org.hectoris919.cbc_equally_opposite.physics.ImpulseMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SableProjectileAttachment {
	private static final double SUPPORT_PROBE_RADIUS = 0.0625D;
	private static final double SUPPORT_INWARD_NUDGE = 0.03125D;
	private static final double SUPPORT_EPSILON = 1.0e-4D;
	private static final Map<UUID, Attachment> SERVER_ATTACHMENTS = new ConcurrentHashMap<>();

	private SableProjectileAttachment() { }

	public static void attachIfStoppedFromResult(AbstractCannonProjectile projectile, BlockHitResult hitResult, Vec3 incomingMotion, AbstractCannonProjectile.ImpactResult result) {
		if (!isStopped(projectile, result)) return;
		attach(projectile, hitResult, incomingMotion);
	}

	public static void attach(AbstractCannonProjectile projectile, BlockHitResult hitResult, Vec3 incomingMotion) {
		Level level = projectile.level();
		if (level.isClientSide || !(level instanceof ServerLevel)) return;

		Vec3 plotPosition = hitResult.getLocation();
		SubLevelAccess containingAccess = SableCompanion.INSTANCE.getContaining(level, plotPosition);
		if (containingAccess == null) return;

		SubLevel subLevel = Sable.HELPER.getContaining(level, plotPosition);
		if (subLevel == null || subLevel.isRemoved()) return;

		Vec3 localDirection = localDirection(subLevel, incomingMotion);
		Attachment attachment = new Attachment(subLevel.getUniqueId(), plotPosition, hitResult.getBlockPos(), localDirection);
		SERVER_ATTACHMENTS.put(projectile.getUUID(), attachment);
		setSyncedAttachment(projectile, true, attachment);
		applyLockedState(projectile, subLevel, attachment);

		if (Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"Attached stopped projectile {} to Sable sublevel {} at plot position {}",
					projectile.getUUID(),
					subLevel.getUniqueId(),
					plotPosition
			);
		}
	}

	public static void tick(AbstractCannonProjectile projectile) {
		if (projectile.isRemoved()) {
			if (!projectile.level().isClientSide) SERVER_ATTACHMENTS.remove(projectile.getUUID());
			return;
		}

		Level level = projectile.level();
		Attachment attachment = getAttachment(projectile);
		if (attachment == null) {
			clearLocalSableHooksIfWeSetThem(projectile);
			return;
		}

		SubLevel subLevel = Sable.HELPER.getContaining(level, attachment.plotPosition());
		if (subLevel == null || subLevel.isRemoved() || !matchesSubLevel(attachment, subLevel)) {
			if (!level.isClientSide) {
				detach(projectile);
			} else {
				clearLocalSableHooks(projectile);
			}
			return;
		}

		if (!level.isClientSide && level instanceof ServerLevel serverLevel && isUnsupported(serverLevel, attachment)) {
			detach(projectile);
			return;
		}

		if (!level.isClientSide) {
			Attachment normalized = withSubLevelId(attachment, subLevel);
			SERVER_ATTACHMENTS.putIfAbsent(projectile.getUUID(), normalized);
			if (!(projectile instanceof SableProjectileAttachmentState state) || !state.cbeo$isSableProjectileAttached()) setSyncedAttachment(projectile, true, normalized);
		}
		applyLockedState(projectile, subLevel, attachment);
	}

	public static boolean isAttached(AbstractCannonProjectile projectile) {
		if (projectile == null) return false;
		if (SERVER_ATTACHMENTS.containsKey(projectile.getUUID())) return true;
		return projectile instanceof SableProjectileAttachmentState state && state.cbeo$isSableProjectileAttached();
	}

	public static void detach(AbstractCannonProjectile projectile) {
		Attachment removed = SERVER_ATTACHMENTS.remove(projectile.getUUID());
		setSyncedAttachment(projectile, false, null);
		clearLocalSableHooks(projectile);
		projectile.setInGround(false);
		projectile.setGroundPos(null);

		if (removed != null && Config.debugImpulses()) EquallyOpposite.LOGGER.info("Detached stopped projectile {} from Sable sublevel {}", projectile.getUUID(), removed.subLevelId());
	}

	private static Attachment getAttachment(AbstractCannonProjectile projectile) {
		Attachment attachment = SERVER_ATTACHMENTS.get(projectile.getUUID());
		if (attachment != null) return attachment;
		if (!(projectile instanceof SableProjectileAttachmentState state) || !state.cbeo$isSableProjectileAttached()) return null;

		Vec3 plotPosition = state.cbeo$getSableProjectilePlotPosition();
		Vec3 localDirection = state.cbeo$getSableProjectileLocalDirection();
		if (!isFinite(plotPosition)) return null;

		BlockPos supportBlockPos = BlockPos.containing(plotPosition);
		return new Attachment(null, plotPosition, supportBlockPos, isFinite(localDirection)
				? localDirection
				: Vec3.ZERO);
	}

	private static void setSyncedAttachment(AbstractCannonProjectile projectile, boolean attached, Attachment attachment) {
		if (!(projectile instanceof SableProjectileAttachmentState state)) return;

		if (!attached || attachment == null) {
			state.cbeo$setSableProjectileAttachment(false, Vec3.ZERO, Vec3.ZERO);
			return;
		}

		state.cbeo$setSableProjectileAttachment(true, attachment.plotPosition(), attachment.localDirection());
	}

	private static Attachment withSubLevelId(Attachment attachment, SubLevel subLevel) {
		if (attachment.subLevelId() != null) return attachment;
		return new Attachment(subLevel.getUniqueId(), attachment.plotPosition(), attachment.supportBlockPos(), attachment.localDirection());
	}

	private static boolean matchesSubLevel(Attachment attachment, SubLevel subLevel) {
		return attachment.subLevelId() == null || attachment.subLevelId().equals(subLevel.getUniqueId());
	}

	private static void applyLockedState(AbstractCannonProjectile projectile, SubLevel subLevel, Attachment attachment) {
		clearLocalSableHooks(projectile);

		Vec3 globalPosition = updateGlobalPose(projectile, subLevel, attachment);
		projectile.setDeltaMovement(Vec3.ZERO);
		projectile.setInGround(true);
		projectile.setGroundPos(globalPosition != null ? globalPosition : projectile.position());
	}

	private static void clearLocalSableHooksIfWeSetThem(AbstractCannonProjectile projectile) {
		clearLocalSableHooks(projectile);
	}

	private static void clearLocalSableHooks(AbstractCannonProjectile projectile) {
		if (projectile instanceof EntityStickExtension stickExtension) stickExtension.sable$setPlotPosition(null);
		if (projectile instanceof EntityMovementExtension movementExtension) movementExtension.sable$setTrackingSubLevel(null);
	}

	private static boolean isStopped(AbstractCannonProjectile projectile, AbstractCannonProjectile.ImpactResult result) {
		if (result != null) {
			return !result.shouldRemove()
					&& (projectile.getProjectileMass() <= 1.0e-4F
					|| result.kinematics() == AbstractCannonProjectile.ImpactResult.KinematicOutcome.STOP);
		}
		return !projectile.isRemoved() && projectile.getProjectileMass() <= 1.0e-4F;
	}

	public static boolean shouldStayAttached(AbstractCannonProjectile projectile) {
		Attachment attachment = getAttachment(projectile);
		if (attachment == null) return false;

		Level level = projectile.level();
		SubLevel subLevel = Sable.HELPER.getContaining(level, attachment.plotPosition());
		if (subLevel == null || subLevel.isRemoved() || !matchesSubLevel(attachment, subLevel)) {
			if (!level.isClientSide) {
				detach(projectile);
			} else {
				clearLocalSableHooks(projectile);
			}
			return false;
		}

		if (level instanceof ServerLevel serverLevel && isUnsupported(serverLevel, attachment)) {
			detach(projectile);
			return false;
		}

		return true;
	}

	private static boolean isUnsupported(ServerLevel level, Attachment attachment) {
		LevelAccelerator accelerator = new LevelAccelerator(level);
		AABB supportProbe = localSupportProbe(attachment);

		if (intersectsSolidCollision(accelerator, attachment.supportBlockPos(), supportProbe)) return false;

		BlockPos min = BlockPos.containing(supportProbe.minX, supportProbe.minY, supportProbe.minZ);
		BlockPos max = BlockPos.containing(supportProbe.maxX, supportProbe.maxY, supportProbe.maxZ);

		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			if (pos.equals(attachment.supportBlockPos())) continue;
			if (intersectsSolidCollision(accelerator, pos, supportProbe)) return false;
		}
		return true;
	}

	private static AABB localSupportProbe(Attachment attachment) {
		Vec3 center = attachment.plotPosition();
		if (ImpulseMath.isUsefulVector(attachment.localDirection())) center = center.add(attachment.localDirection().normalize().scale(SUPPORT_INWARD_NUDGE));
		return new AABB(center, center).inflate(SUPPORT_PROBE_RADIUS + SUPPORT_EPSILON);
	}

	private static boolean intersectsSolidCollision(LevelAccelerator accelerator, BlockPos pos, AABB localSupportBox) {
		BlockState state = accelerator.getBlockState(pos);
		if (state.isAir()) return false;

		VoxelShape shape = state.getCollisionShape(accelerator, pos);
		if (shape.isEmpty()) return false;

		for (AABB shapeBox : shape.toAabbs()) {
			if (shapeBox.move(pos).intersects(localSupportBox)) return true;
		}
		return false;
	}

	private static Vec3 localDirection(SubLevel subLevel, Vec3 incomingMotion) {
		if (!ImpulseMath.isUsefulVector(incomingMotion)) return Vec3.ZERO;

		try {
			Vec3 local = subLevel.logicalPose().transformNormalInverse(incomingMotion.normalize());
			return ImpulseMath.isUsefulVector(local) ? local.normalize() : Vec3.ZERO;
		} catch (Throwable ignored) {
			return incomingMotion.normalize();
		}
	}

	private static Vec3 updateGlobalPose(AbstractCannonProjectile projectile, SubLevel subLevel, Attachment attachment) {
		Vec3 globalPosition = null;
		try {
			globalPosition = subLevel.logicalPose().transformPosition(attachment.plotPosition());
			projectile.setPos(globalPosition);
		} catch (Throwable ignored) { }

		if (globalPosition == null || !ImpulseMath.isUsefulVector(attachment.localDirection())) return globalPosition;

		try {
			Vec3 globalEndpoint = subLevel.logicalPose().transformPosition(attachment.plotPosition().add(attachment.localDirection()));
			Vec3 globalDirection = globalEndpoint.subtract(globalPosition);
			if (ImpulseMath.isUsefulVector(globalDirection)) applyRotationFromDirection(projectile, globalDirection.normalize());
		} catch (Throwable ignored) { }

		return globalPosition;
	}

	private static void applyRotationFromDirection(AbstractCannonProjectile projectile, Vec3 direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0D / Math.PI));
		float pitch = (float) (Math.atan2(direction.y, horizontal) * (180.0D / Math.PI));
		projectile.setYRot(yaw);
		projectile.setXRot(pitch);
		projectile.setOrientation(direction);
	}

	private static boolean isFinite(Vec3 vector) {
		return vector != null
				&& Double.isFinite(vector.x)
				&& Double.isFinite(vector.y)
				&& Double.isFinite(vector.z);
	}

	private record Attachment(UUID subLevelId, Vec3 plotPosition, BlockPos supportBlockPos, Vec3 localDirection) { }
}
