package org.hectoris919.cbc_equally_opposite.mixin;

import org.hectoris919.cbc_equally_opposite.compat.sable.SableProjectileAttachmentState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileAttachmentDataMixin implements SableProjectileAttachmentState {
	@Unique private static final String cbeo$TAG_ATTACHED = "CBEOSableAttached";
	@Unique private static final String cbeo$TAG_PLOT_X = "CBEOSablePlotX";
	@Unique private static final String cbeo$TAG_PLOT_Y = "CBEOSablePlotY";
	@Unique private static final String cbeo$TAG_PLOT_Z = "CBEOSablePlotZ";
	@Unique private static final String cbeo$TAG_DIR_X = "CBEOSableDirX";
	@Unique private static final String cbeo$TAG_DIR_Y = "CBEOSableDirY";
	@Unique private static final String cbeo$TAG_DIR_Z = "CBEOSableDirZ";

	@Unique private static final EntityDataAccessor<Boolean> cbeo$SABLE_ATTACHED = SynchedEntityData.defineId(AbstractCannonProjectile.class, EntityDataSerializers.BOOLEAN);
	@Unique private static final EntityDataAccessor<Vector3f> cbeo$SABLE_PLOT_POSITION = SynchedEntityData.defineId(AbstractCannonProjectile.class, EntityDataSerializers.VECTOR3);
	@Unique private static final EntityDataAccessor<Vector3f> cbeo$SABLE_LOCAL_DIRECTION = SynchedEntityData.defineId(AbstractCannonProjectile.class, EntityDataSerializers.VECTOR3);

	@Inject(method = "defineSynchedData", at = @At("RETURN"), require = 0)
	private void cbeo$defineSableAttachmentData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(cbeo$SABLE_ATTACHED, false);
		builder.define(cbeo$SABLE_PLOT_POSITION, new Vector3f());
		builder.define(cbeo$SABLE_LOCAL_DIRECTION, new Vector3f());
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"), require = 0)
	private void cbeo$saveSableAttachmentData(CompoundTag tag, CallbackInfo ci) {
		if (!this.cbeo$isSableProjectileAttached()) return;
		
		Vec3 plotPosition = this.cbeo$getSableProjectilePlotPosition();
		Vec3 localDirection = this.cbeo$getSableProjectileLocalDirection();
		tag.putBoolean(cbeo$TAG_ATTACHED, true);
		tag.putDouble(cbeo$TAG_PLOT_X, plotPosition.x);
		tag.putDouble(cbeo$TAG_PLOT_Y, plotPosition.y);
		tag.putDouble(cbeo$TAG_PLOT_Z, plotPosition.z);
		tag.putDouble(cbeo$TAG_DIR_X, localDirection.x);
		tag.putDouble(cbeo$TAG_DIR_Y, localDirection.y);
		tag.putDouble(cbeo$TAG_DIR_Z, localDirection.z);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"), require = 0)
	private void cbeo$readSableAttachmentData(CompoundTag tag, CallbackInfo ci) {
		if (!tag.getBoolean(cbeo$TAG_ATTACHED)) {
			this.cbeo$setSableProjectileAttachment(false, Vec3.ZERO, Vec3.ZERO);
			return;
		}

		Vec3 plotPosition = new Vec3(
				tag.getDouble(cbeo$TAG_PLOT_X),
				tag.getDouble(cbeo$TAG_PLOT_Y),
				tag.getDouble(cbeo$TAG_PLOT_Z)
		);
		Vec3 localDirection = new Vec3(
				tag.getDouble(cbeo$TAG_DIR_X),
				tag.getDouble(cbeo$TAG_DIR_Y),
				tag.getDouble(cbeo$TAG_DIR_Z)
		);
		this.cbeo$setSableProjectileAttachment(true, plotPosition, localDirection);
	}

	@Override
	public boolean cbeo$isSableProjectileAttached() {
		return cbeo$self().getEntityData().get(cbeo$SABLE_ATTACHED);
	}

	@Override
	public void cbeo$setSableProjectileAttachment(boolean attached, Vec3 plotPosition, Vec3 localDirection) {
		SynchedEntityData data = cbeo$self().getEntityData();
		data.set(cbeo$SABLE_ATTACHED, attached);
		data.set(cbeo$SABLE_PLOT_POSITION, cbeo$toVector3f(plotPosition));
		data.set(cbeo$SABLE_LOCAL_DIRECTION, cbeo$toVector3f(localDirection));
	}

	@Override
	public Vec3 cbeo$getSableProjectilePlotPosition() {
		return cbeo$toVec3(cbeo$self().getEntityData().get(cbeo$SABLE_PLOT_POSITION));
	}

	@Override
	public Vec3 cbeo$getSableProjectileLocalDirection() {
		return cbeo$toVec3(cbeo$self().getEntityData().get(cbeo$SABLE_LOCAL_DIRECTION));
	}

	@Unique
	private AbstractCannonProjectile cbeo$self() {
		return (AbstractCannonProjectile) (Object) this;
	}

	@Unique
	private static Vector3f cbeo$toVector3f(Vec3 vec) {
		if (vec == null) return new Vector3f();
		return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
	}

	@Unique
	private static Vec3 cbeo$toVec3(Vector3f vector) {
		if (vector == null) return Vec3.ZERO;
		return new Vec3(vector.x(), vector.y(), vector.z());
	}
}
