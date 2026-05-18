package org.hectoris919.cbc_equally_opposite.physics;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.hectoris919.cbc_equally_opposite.Config;
import org.hectoris919.cbc_equally_opposite.EquallyOpposite;
import org.hectoris919.cbc_equally_opposite.registry.EODamageTypes;
import org.hectoris919.cbc_equally_opposite.registry.EOGameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MuzzleBlastHandler {
	private static final double RAY_EPSILON = 1.0E-5D;
	private static final double SECONDS_PER_TICK = 1.0D / 20.0D;
	private static final int DIRECT_SHOCK_SOURCE_ID = 0;
	private static final Map<ResourceKey<Level>, Queue<ActiveBlast>> ACTIVE = new ConcurrentHashMap<>();

	private MuzzleBlastHandler() { }

	public static void start(ServerLevel level, Vec3 muzzlePosition, Vec3 forward, double gasImpulseNs, double gasMassKg, double gasVelocityMps, double projectileMassKg, double projectileVelocityMps, int gasEscapeSteps, boolean autocannon, Entity excludedEntity) {
		if (Config.disableMuzzleBlast()) return;
		if (!ImpulseMath.isUsefulVector(forward)) return;
		if (!ImpulseMath.isFinite(muzzlePosition)) return;
		if (!Double.isFinite(gasImpulseNs) || gasImpulseNs <= 0.0D) return;
		if (gasEscapeSteps <= 0) return;

		SubLevelAccess sourceSubLevel = SableCompanion.INSTANCE.getContaining(level, muzzlePosition);
		Vec3 globalMuzzlePosition = sourceSubLevel == null ? muzzlePosition : sourceSubLevel.logicalPose().transformPosition(muzzlePosition);
		Vec3 globalForward = sourceSubLevel == null ? forward.normalize() : sourceSubLevel.logicalPose().transformNormal(forward).normalize();
		if (!ImpulseMath.isFinite(globalMuzzlePosition) || !ImpulseMath.isUsefulVector(globalForward)) return;

		double halfAngleDegrees = autocannon ? Config.muzzleBlastAutocannonHalfAngleDegrees() : Config.muzzleBlastBigCannonHalfAngleDegrees();
		double coneTan = Math.tan(Math.toRadians(halfAngleDegrees));
		if (!Double.isFinite(coneTan) || coneTan <= 0.0D) return;

		double boreRadius = autocannon ? Config.muzzleBlastAutocannonBoreRadius() : Config.muzzleBlastBigCannonBoreRadius();
		if (!Double.isFinite(boreRadius) || boreRadius <= 0.0D) return;

		double range = Config.muzzleBlastCalculationRange();
		if (!Double.isFinite(range) || range <= 0.0D) return;

		int lifetimeSteps = Math.max(gasEscapeSteps, shockLifetimeSteps());

		int excludedEntityId = excludedEntity == null ? Integer.MIN_VALUE : excludedEntity.getId();
		UUID sourceSubLevelId = sourceSubLevel == null ? null : sourceSubLevel.getUniqueId();
		ActiveBlast blast = new ActiveBlast(
				globalMuzzlePosition,
				globalForward,
				gasImpulseNs,
				gasMassKg,
				gasVelocityMps,
				projectileMassKg,
				projectileVelocityMps,
				gasEscapeSteps,
				lifetimeSteps,
				0,
				boreRadius,
				coneTan,
				range,
				excludedEntityId,
				sourceSubLevelId,
				new BlastState()
		);
		ACTIVE.computeIfAbsent(level.dimension(), ignored -> new ConcurrentLinkedQueue<>()).add(blast);

		if (Config.debugImpulses()) {
			EquallyOpposite.LOGGER.info(
					"{} continuous muzzle blast queued: gasImpulse={} N*s, gasMass={} kg, gasVelocity={} m/s, gasSteps={}, lifetimeSteps={}, muzzle={}, forward={}, sourceSublevel={}",
					autocannon ? "Autocannon" : "Big cannon",
					gasImpulseNs,
					gasMassKg,
					gasVelocityMps,
					gasEscapeSteps,
					lifetimeSteps,
					globalMuzzlePosition,
					globalForward,
					sourceSubLevelId
			);
		}
	}

	public static void tick(ServerLevel level) {
		Queue<ActiveBlast> queue = ACTIVE.get(level.dimension());
		if (queue == null || queue.isEmpty()) return;

		int queuedThisTick = queue.size();
		for (int i = 0; i < queuedThisTick; i++) {
			ActiveBlast blast = queue.poll();
			if (blast == null) break;
			ActiveBlast next = blast.tick(level);
			if (next != null) queue.add(next);
		}
	}

	private static int shockLifetimeSteps() {
		double range = Config.muzzleShockRange();
		double speed = Config.muzzleShockSoundSpeed();
		if (!Double.isFinite(range) || range <= 0.0D || !Double.isFinite(speed) || speed <= 0.0D) return 0;
		double blocksPerTick = speed * SECONDS_PER_TICK;
		if (!Double.isFinite(blocksPerTick) || blocksPerTick <= 0.0D) return 0;
		int travelSteps = (int) Math.ceil(range / blocksPerTick) + 1;
		int negativeDelay = Math.max(0, Config.muzzleShockNegativePhaseDelayTicks());
		return Math.max(travelSteps, negativeDelay + travelSteps);
	}

	private record ActiveBlast(
			Vec3 muzzlePosition,
			Vec3 forward,
			double gasImpulseNs,
			double gasMassKg,
			double gasVelocityMps,
			double projectileMassKg,
			double projectileVelocityMps,
			int gasEscapeSteps,
			int lifetimeSteps,
			int elapsedSteps,
			double boreRadius,
			double coneTan,
			double range,
			int excludedEntityId,
			UUID sourceSubLevelId,
			BlastState state
	) {
		private ActiveBlast tick(ServerLevel level) {
			if (this.elapsedSteps >= this.lifetimeSteps) return null;

			if (!this.state.reflectionSourcesBuilt) {
				this.buildShockReflectionSources(level);
				this.state.reflectionSourcesBuilt = true;
			}

			if (this.elapsedSteps < this.gasEscapeSteps) {
				double impulseThisStepNs = this.gasImpulseNs / this.gasEscapeSteps;
				double gasMassThisStepKg = this.gasMassKg / this.gasEscapeSteps;
				if (Double.isFinite(impulseThisStepNs) && impulseThisStepNs > 0.0D) this.applyGasJet(level, impulseThisStepNs, gasMassThisStepKg);
			}

			this.applyShockShell(level, false, this.elapsedSteps);
			this.applyShockShell(level, true, this.elapsedSteps);

			int nextElapsed = this.elapsedSteps + 1;
			if (nextElapsed >= this.lifetimeSteps) return null;
			return new ActiveBlast(this.muzzlePosition, this.forward, this.gasImpulseNs, this.gasMassKg, this.gasVelocityMps, this.projectileMassKg, this.projectileVelocityMps, this.gasEscapeSteps, this.lifetimeSteps, nextElapsed, this.boreRadius, this.coneTan, this.range, this.excludedEntityId, this.sourceSubLevelId, this.state);
		}

		private void applyGasJet(ServerLevel level, double impulseThisStepNs, double gasMassThisStepKg) {
			AABB searchBox = this.coneSearchBox();
			List<SubLevelAccess> candidateSubLevels = this.candidateSubLevels(level, searchBox);
			List<EntityCandidate> candidateEntities = this.candidateEntities(level, searchBox, candidateSubLevels);
			Map<Integer, EntityLoad> entityLoads = new HashMap<>();
			Map<UUID, SubLevelLoad> subLevelLoads = new HashMap<>();

			for (EntityCandidate candidate : candidateEntities) {
				this.applyGasJetToEntity(level, candidateSubLevels, entityLoads, candidate, impulseThisStepNs, gasMassThisStepKg);
			}

			if (!this.state.gasSurfaceContactsBuilt) {
				this.buildGasJetSurfaceContacts(level, candidateSubLevels);
				this.state.gasSurfaceContactsBuilt = true;
			}
			this.applyGasJetSurfaceContactsToSubLevels(subLevelLoads, impulseThisStepNs);

			this.flushEntityLoads(level, entityLoads, true);
			this.flushSubLevelLoads(level, subLevelLoads);
		}

		private void applyGasJetToEntity(ServerLevel level, List<SubLevelAccess> candidateSubLevels, Map<Integer, EntityLoad> entityLoads, EntityCandidate candidate, double impulseThisStepNs, double gasMassThisStepKg) {
			Entity entity = candidate.entity();
			Target target = this.entityTarget(candidate);
			if (target == null || !this.overlapsJet(target.center(), target.radius())) return;

			BlastSample sample = this.sample(target.center(), target.radius());
			if (sample == null) return;

			double visibility = this.visibilityToTarget(level, candidateSubLevels, this.entitySamplePoints(target), false);
			if (!Double.isFinite(visibility) || visibility <= 0.0D) return;

			double dragCoefficient = Config.muzzleBlastEntityDragCoefficient();
			if (!Double.isFinite(dragCoefficient) || dragCoefficient <= 0.0D) return;

			double projectedArea = target.projectedArea();
			if (!Double.isFinite(projectedArea) || projectedArea <= 0.0D) return;

			double pressureImpulsePaS = Config.muzzleBlastCoupling() * impulseThisStepNs * sample.profileWeight() * visibility;
			if (!Double.isFinite(pressureImpulsePaS) || pressureImpulsePaS <= 0.0D) return;

			double absorbedImpulseNs = pressureImpulsePaS * projectedArea * dragCoefficient;
			if (!Double.isFinite(absorbedImpulseNs) || absorbedImpulseNs <= 0.0D) return;

			EntityLoad load = entityLoads.computeIfAbsent(entity.getId(), ignored -> new EntityLoad(entity, target.trackingSubLevel()));
			load.addImpulse(sample.flowDirection().scale(absorbedImpulseNs));
			if (canReceiveBlastDamage(entity)) load.addGasDamage(this.mechanicalDamage(entity, absorbedImpulseNs, projectedArea));

			if (this.cannonBlastDamageEnabled(level) && canReceiveBlastDamage(entity)) {
				HeatSample heat = this.heatDoseThisStep(sample, gasMassThisStepKg * sample.profileWeight() * projectedArea * visibility, projectedArea, Math.min(1.0D, dragCoefficient));
				if (heat != null) {
					load.addBurnDamage(this.thermalDamage(heat.heatDosePerSquareMeter()));
					if (heat.localTemperatureK() >= Config.muzzleBlastIgnitionTemperature()) load.addIgnitionHeatDose(heat.heatDosePerSquareMeter());
				}
			}
		}

		private void buildGasJetSurfaceContacts(ServerLevel level, List<SubLevelAccess> candidateSubLevels) {
			GasJetRayDef[] rays = buildGasJetRayDefs(this.forward, this.coneTan);
			for (GasJetRayDef ray : rays) {
				if (ray == null || !ImpulseMath.isUsefulVector(ray.direction())) continue;
				Vec3 direction = ray.direction().normalize();
				SurfaceHit hit = this.nearestSolidHit(level, candidateSubLevels, this.muzzlePosition.add(direction.scale(RAY_EPSILON)), this.muzzlePosition.add(direction.scale(this.range)), true);
				if (hit == null || hit.kind() != SurfaceKind.SUBLEVEL || hit.subLevel() == null) continue;
				if (this.sourceSubLevelId != null && this.sourceSubLevelId.equals(hit.subLevel().getUniqueId())) continue;

				Vec3 normal = orientedSurfaceNormal(hit, direction);
				if (!ImpulseMath.isUsefulVector(normal)) continue;
				double incidence = Math.max(0.0D, -direction.dot(normal.normalize()));
				if (!Double.isFinite(incidence) || incidence <= 0.0D) continue;

				this.state.gasSurfaceContacts.add(new GasSurfaceContact(
						hit.subLevel().getUniqueId(),
						hit.subLevel(),
						hit.globalPoint(),
						normal.normalize(),
						direction,
						ray.areaFraction()
				));
			}

			if (Config.debugImpulses() && !this.state.gasSurfaceContacts.isEmpty()) {
				EquallyOpposite.LOGGER.info("Muzzle gas jet sublevel contact budget: retainedGasContacts={}", this.state.gasSurfaceContacts.size());
			}
		}

		private void applyGasJetSurfaceContactsToSubLevels(Map<UUID, SubLevelLoad> subLevelLoads, double impulseThisStepNs) {
			for (GasSurfaceContact contact : this.state.gasSurfaceContacts) {
				if (contact == null || contact.subLevel() == null) continue;
				BlastSample sample = this.sample(contact.hitPoint(), 0.0D);
				if (sample == null) continue;

				Vec3 normal = contact.normal();
				Vec3 flowDirection = sample.flowDirection();
				if (!ImpulseMath.isUsefulVector(normal) || !ImpulseMath.isUsefulVector(flowDirection)) continue;
				normal = normal.normalize();
				flowDirection = flowDirection.normalize();

				double incidence = Math.max(0.0D, -flowDirection.dot(normal));
				if (!Double.isFinite(incidence) || incidence <= 0.0D) continue;

				double jetArea = Math.PI * sample.jetRadius() * sample.jetRadius();
				double patchArea = jetArea * contact.areaFraction();
				if (!Double.isFinite(patchArea) || patchArea <= 0.0D) continue;

				double pressureImpulsePaS = Config.muzzleBlastCoupling() * impulseThisStepNs * sample.profileWeight();
				double reflection = Math.sqrt(this.solidShockReflectionFraction(incidence));
				double impulseNs = pressureImpulsePaS * patchArea * incidence * (1.0D + reflection);
				if (!Double.isFinite(impulseNs) || impulseNs <= 0.0D) continue;

				Vec3 globalImpulse = normal.scale(-impulseNs);
				SubLevelLoad load = subLevelLoads.computeIfAbsent(contact.subLevelId(), ignored -> new SubLevelLoad(contact.subLevel()));
				load.add(contact.hitPoint(), globalImpulse, impulseNs);
			}
		}

		private void applyShockShell(ServerLevel level, boolean negativePhase, int currentStep) {
			double range = Config.muzzleShockRange();
			double soundSpeed = Config.muzzleShockSoundSpeed();
			if (!Double.isFinite(range) || range <= 0.0D || !Double.isFinite(soundSpeed) || soundSpeed <= 0.0D) return;

			int phaseDelay = negativePhase ? Math.max(0, Config.muzzleShockNegativePhaseDelayTicks()) : 0;
			int phaseStep = currentStep - phaseDelay;
			if (phaseStep < 0) return;

			double previousRadius = phaseStep * soundSpeed * SECONDS_PER_TICK;
			double currentRadius = (phaseStep + 1) * soundSpeed * SECONDS_PER_TICK;
			if (previousRadius >= range) return;
			currentRadius = Math.min(currentRadius, range);
			if (!Double.isFinite(currentRadius) || currentRadius <= previousRadius) return;

			double phaseEnergyJ = this.shockEnergyJ();
			if (negativePhase) {
				double negativeFraction = Config.muzzleShockNegativePhaseFraction();
				if (!Double.isFinite(negativeFraction) || negativeFraction <= 0.0D) return;
				phaseEnergyJ *= negativeFraction;
			}
			if (!Double.isFinite(phaseEnergyJ) || phaseEnergyJ <= 0.0D) return;

			AABB searchBox = this.sphericalSearchBox(this.muzzlePosition, currentRadius + 2.0D);
			List<SubLevelAccess> candidateSubLevels = this.candidateSubLevels(level, searchBox);
			List<EntityCandidate> candidateEntities = this.candidateEntities(level, searchBox, candidateSubLevels);
			Map<Integer, EntityLoad> entityLoads = new HashMap<>();
			Map<UUID, SubLevelLoad> subLevelLoads = new HashMap<>();

			this.applyShockFromSource(level, candidateSubLevels, candidateEntities, entityLoads, DIRECT_SHOCK_SOURCE_ID, this.muzzlePosition, this.muzzlePosition, phaseEnergyJ, previousRadius, currentRadius, negativePhase, true, null);

			for (ImageShockSource source : this.state.reflectionSources) {
				if (source == null || source.energyFraction() <= 0.0D) continue;
				if (currentRadius <= source.startDistance()) continue;
				double reflectedEnergy = phaseEnergyJ * source.energyFraction();
				if (!Double.isFinite(reflectedEnergy) || reflectedEnergy <= 0.0D) continue;
				this.applyShockFromSource(level, candidateSubLevels, candidateEntities, entityLoads, source.id(), source.virtualSource(), source.reflectionPoint(), reflectedEnergy, previousRadius, currentRadius, negativePhase, false, source);
			}

			this.applyShockSurfaceContactsToSubLevels(subLevelLoads, phaseEnergyJ, previousRadius, currentRadius, negativePhase);

			this.flushEntityLoads(level, entityLoads, false);
			this.flushSubLevelLoads(level, subLevelLoads);
		}

		private void applyShockFromSource(ServerLevel level, List<SubLevelAccess> candidateSubLevels, List<EntityCandidate> candidateEntities, Map<Integer, EntityLoad> entityLoads, int sourceId, Vec3 virtualSource, Vec3 physicalSource, double energyJ, double previousRadius, double currentRadius, boolean negativePhase, boolean direct, ImageShockSource reflectedSource) {
			for (EntityCandidate candidate : candidateEntities) {
				Entity entity = candidate.entity();
				Target target = this.entityTarget(candidate);
				if (target == null) continue;

				double pathDistance = direct ? target.center().distanceTo(this.muzzlePosition) : reflectedSource.startDistance() + target.center().distanceTo(physicalSource);
				if (!Double.isFinite(pathDistance) || pathDistance <= RAY_EPSILON) continue;
				if (pathDistance + target.radius() < previousRadius || pathDistance - target.radius() > currentRadius) continue;

				if (direct) {
					Set<Integer> hitSet = negativePhase ? this.state.negativeShockEntities : this.state.positiveShockEntities;
					if (!hitSet.add(entity.getId())) continue;
				}

				Vec3 propagationDirection = target.center().subtract(direct ? this.muzzlePosition : physicalSource);
				if (!ImpulseMath.isUsefulVector(propagationDirection)) continue;
				propagationDirection = propagationDirection.normalize();

				double visibility = this.cachedVisibilityToTarget(
						level,
						candidateSubLevels,
						sourceId,
						target,
						!direct,
						direct ? this.muzzlePosition : physicalSource
				);
				if (!Double.isFinite(visibility) || visibility <= 0.0D) continue;

				double directivity = direct ? this.shockDirectivity(propagationDirection) : 1.0D;
				ShockLoading loading = this.shockLoading(energyJ * directivity, pathDistance, negativePhase);
				if (loading == null) continue;

				double projectedArea = target.projectedAreaFor(propagationDirection);
				if (!Double.isFinite(projectedArea) || projectedArea <= 0.0D) continue;

				double loadedFraction = this.shockEntityLoadedFraction(target, propagationDirection);
				if (!Double.isFinite(loadedFraction) || loadedFraction <= 0.0D) continue;

				double effectivePressureImpulse = loading.pressureImpulsePaS() * visibility * loadedFraction;
				double effectivePeakPressure = loading.peakPressurePa() * visibility * loadedFraction;
				double entityImpulseNs = effectivePressureImpulse * projectedArea;
				if (!Double.isFinite(entityImpulseNs) || entityImpulseNs <= 0.0D) continue;

				Vec3 impulseDirection = negativePhase ? propagationDirection.scale(-1.0D) : propagationDirection;
				EntityLoad load = entityLoads.computeIfAbsent(entity.getId(), ignored -> new EntityLoad(entity, target.trackingSubLevel()));
				load.addImpulse(impulseDirection.scale(entityImpulseNs));
				if (canReceiveBlastDamage(entity)) load.addShockDamage(this.shockDamage(entity, effectivePressureImpulse, effectivePeakPressure, entityImpulseNs));
			}
		}

		private void applyShockSurfaceContactsToSubLevels(Map<UUID, SubLevelLoad> subLevelLoads, double energyJ, double previousRadius, double currentRadius, boolean negativePhase) {
			for (ShockSurfaceContact contact : this.state.shockSurfaceContacts) {
				if (contact == null || contact.subLevel() == null) continue;
				if (this.sourceSubLevelId != null && this.sourceSubLevelId.equals(contact.subLevelId())) continue;

				double pathDistance = contact.pathDistance();
				if (!Double.isFinite(pathDistance) || pathDistance <= RAY_EPSILON) continue;
				if (pathDistance <= previousRadius || pathDistance > currentRadius) continue;

				Vec3 incomingDirection = contact.incomingDirection();
				Vec3 normal = contact.normal();
				if (!ImpulseMath.isUsefulVector(incomingDirection) || !ImpulseMath.isUsefulVector(normal)) continue;
				incomingDirection = incomingDirection.normalize();
				normal = normal.normalize();

				double normalFactor = Math.max(0.0D, -incomingDirection.dot(normal));
				if (!Double.isFinite(normalFactor) || normalFactor <= 0.0D) continue;

				ShockLoading loading = this.shockLoading(energyJ, pathDistance, negativePhase);
				if (loading == null) continue;

				double sphereArea = 4.0D * Math.PI * pathDistance * pathDistance;
				double patchArea = sphereArea * contact.energyFraction();
				if (!Double.isFinite(patchArea) || patchArea <= 0.0D) continue;

				double energyReflection = this.solidShockReflectionFraction(normalFactor);
				double pressureReflection = Math.sqrt(Math.max(0.0D, energyReflection));
				double impulseNs = loading.pressureImpulsePaS() * patchArea * normalFactor * (1.0D + pressureReflection);
				if (!Double.isFinite(impulseNs) || impulseNs <= 0.0D) continue;

				Vec3 surfaceDirection = negativePhase ? normal : normal.scale(-1.0D);
				if (!ImpulseMath.isUsefulVector(surfaceDirection)) surfaceDirection = negativePhase ? incomingDirection.scale(-1.0D) : incomingDirection;
				Vec3 globalImpulse = surfaceDirection.normalize().scale(impulseNs);
				SubLevelLoad load = subLevelLoads.computeIfAbsent(contact.subLevelId(), ignored -> new SubLevelLoad(contact.subLevel()));
				load.add(contact.hitPoint(), globalImpulse, impulseNs);
			}
		}

		private void buildShockReflectionSources(ServerLevel level) {
			double range = Config.muzzleShockRange();
			if (!Double.isFinite(range) || range <= 0.0D) return;

			boolean reflectionsEnabled = Config.muzzleShockMaxReflections() > 0;
			int maxSources = reflectionsEnabled ? Math.max(0, Config.muzzleShockMaxReflectionSources()) : 0;
			int maxContacts = Math.max(0, Config.muzzleShockMaxSubLevelSurfaceContacts());
			int maxContactsPerBody = Math.max(0, Config.muzzleShockMaxSubLevelContactsPerBody());
			if (maxSources <= 0 && (maxContacts <= 0 || maxContactsPerBody <= 0)) return;

			double minEnergyFraction = Config.muzzleShockMinReflectionEnergyFraction();
			if (!Double.isFinite(minEnergyFraction) || minEnergyFraction < 0.0D) minEnergyFraction = 0.0D;

			AABB searchBox = this.sphericalSearchBox(this.muzzlePosition, range);
			List<SubLevelAccess> candidateSubLevels = this.candidateSubLevels(level, searchBox);
			List<ImageShockSource> rawSources = new ArrayList<>();
			List<ShockSurfaceContact> rawContacts = new ArrayList<>();
			ShockRayDef[] rays = buildShockRayDefs(this.forward);
			for (ShockRayDef ray : rays) {
				if (ray == null || !ImpulseMath.isUsefulVector(ray.direction())) continue;
				Vec3 direction = ray.direction().normalize();
				SurfaceHit hit = this.nearestSolidHit(level, candidateSubLevels, this.muzzlePosition.add(direction.scale(RAY_EPSILON)), this.muzzlePosition.add(direction.scale(range)), true);
				if (hit == null) continue;

				Vec3 normal = orientedSurfaceNormal(hit, direction);
				if (!ImpulseMath.isUsefulVector(normal)) continue;
				double normalFactor = Math.max(0.0D, -direction.dot(normal));
				if (!Double.isFinite(normalFactor) || normalFactor <= 0.0D) continue;

				if (maxContacts > 0 && maxContactsPerBody > 0 && hit.kind() == SurfaceKind.SUBLEVEL && hit.subLevel() != null) {
					if (this.sourceSubLevelId == null || !this.sourceSubLevelId.equals(hit.subLevel().getUniqueId())) {
						rawContacts.add(new ShockSurfaceContact(
								hit.subLevel().getUniqueId(),
								hit.subLevel(),
								hit.globalPoint(),
								normal.normalize(),
								direction,
								hit.muzzleDistance(),
								ray.energyFraction()
						));
					}
				}

				if (!reflectionsEnabled || maxSources <= 0) continue;

				double reflectedEnergyFraction = ray.energyFraction() * this.solidShockReflectionFraction(normalFactor);
				if (!Double.isFinite(reflectedEnergyFraction) || reflectedEnergyFraction <= minEnergyFraction) continue;

				Vec3 reflectedDirection = direction.subtract(normal.scale(2.0D * direction.dot(normal)));
				if (!ImpulseMath.isUsefulVector(reflectedDirection)) continue;
				Vec3 virtualSource = mirrorAcrossPlane(this.muzzlePosition, hit.globalPoint(), normal);
				if (!ImpulseMath.isFinite(virtualSource)) continue;
				rawSources.add(new ImageShockSource(-1, virtualSource, hit.globalPoint().add(reflectedDirection.normalize().scale(RAY_EPSILON)), hit.muzzleDistance(), reflectedEnergyFraction));
			}

			this.state.shockSurfaceContacts.addAll(compactShockSurfaceContacts(rawContacts, maxContacts, maxContactsPerBody));
			this.state.reflectionSources.addAll(compactReflectionSources(rawSources, maxSources));
			if (Config.debugImpulses() && (!rawSources.isEmpty() || !rawContacts.isEmpty())) {
				EquallyOpposite.LOGGER.info(
						"Muzzle shock geometry budget: rawReflectionSources={}, retainedReflectionSources={}, rawSubLevelContacts={}, retainedSubLevelContacts={}",
						rawSources.size(),
						this.state.reflectionSources.size(),
						rawContacts.size(),
						this.state.shockSurfaceContacts.size()
				);
			}
		}

		private static List<ImageShockSource> compactReflectionSources(List<ImageShockSource> sources, int maxSources) {
			if (sources == null || sources.isEmpty() || maxSources <= 0) return List.of();
			sources.sort(Comparator.comparingDouble(ImageShockSource::energyFraction).reversed());
			int retained = Math.min(maxSources, sources.size());
			List<ImageShockSource> compacted = new ArrayList<>(retained);
			for (int i = 0; i < retained; i++) {
				ImageShockSource source = sources.get(i);
				compacted.add(new ImageShockSource(i + 1, source.virtualSource(), source.reflectionPoint(), source.startDistance(), source.energyFraction()));
			}
			return compacted;
		}

		private static List<ShockSurfaceContact> compactShockSurfaceContacts(List<ShockSurfaceContact> contacts, int maxContacts, int maxContactsPerBody) {
			if (contacts == null || contacts.isEmpty() || maxContacts <= 0 || maxContactsPerBody <= 0) return List.of();
			contacts.sort(Comparator.comparingDouble(ShockSurfaceContact::energyFraction).reversed());
			List<ShockSurfaceContact> compacted = new ArrayList<>(Math.min(maxContacts, contacts.size()));
			Map<UUID, Integer> contactsByBody = new HashMap<>();
			for (ShockSurfaceContact contact : contacts) {
				if (contact == null || contact.subLevelId() == null) continue;
				int bodyCount = contactsByBody.getOrDefault(contact.subLevelId(), 0);
				if (bodyCount >= maxContactsPerBody) continue;
				compacted.add(contact);
				contactsByBody.put(contact.subLevelId(), bodyCount + 1);
				if (compacted.size() >= maxContacts) break;
			}
			return compacted;
		}

		private void flushEntityLoads(ServerLevel level, Map<Integer, EntityLoad> entityLoads, boolean allowHeat) {
			boolean damageEnabled = this.cannonBlastDamageEnabled(level);
			for (EntityLoad load : entityLoads.values()) {
				if (ImpulseMath.isUsefulVector(load.globalImpulse) && !isBlastForceExempt(load.entity)) {
					Vec3 entityImpulse = load.trackingSubLevel == null ? load.globalImpulse : load.trackingSubLevel.logicalPose().transformNormalInverse(load.globalImpulse);
					this.pushEntity(load.entity, entityImpulse);
				}
				if (damageEnabled && canReceiveBlastDamage(load.entity)) {
					this.damageEntity(level, load.entity, load.totalDamage(), load.primaryDamageKind());
					if (allowHeat && load.maxIgnitionHeatDoseThisTick > 0.0D) this.heatEntity(load.entity, load.maxIgnitionHeatDoseThisTick);
				}
			}
		}

		private void flushSubLevelLoads(ServerLevel level, Map<UUID, SubLevelLoad> subLevelLoads) {
			for (SubLevelLoad load : subLevelLoads.values()) {
				if (!ImpulseMath.isUsefulVector(load.globalImpulse) || !Double.isFinite(load.scalarImpulseNs) || load.scalarImpulseNs <= 0.0D) continue;

				Vec3 globalApplicationPoint = load.impulseWeightedPoint.scale(1.0D / load.scalarImpulseNs);
				Vec3 sableGlobalImpulse = ImpulseMath.siImpulseToSable(load.globalImpulse);
				Vec3 localPoint = load.subLevel.logicalPose().transformPositionInverse(globalApplicationPoint);
				Vec3 localImpulse = load.subLevel.logicalPose().transformNormalInverse(sableGlobalImpulse);
				PendingImpulseQueue.enqueueForSubLevel(level, load.subLevel.getUniqueId(), localPoint, localImpulse, ImpulseKind.IMPACT, 1);
			}
		}

		private boolean overlapsJet(Vec3 center, double targetRadius) {
			Vec3 offset = center.subtract(this.muzzlePosition);
			double axialDistance = offset.dot(this.forward);
			if (!Double.isFinite(axialDistance)) return false;
			if (axialDistance + targetRadius <= 0.0D || axialDistance - targetRadius > this.range) return false;

			double clampedAxial = clamp(axialDistance, 0.0D, this.range);
			Vec3 axial = this.forward.scale(clampedAxial);
			Vec3 radial = offset.subtract(axial);
			double radialDistance = radial.length();
			double jetRadius = this.boreRadius + clampedAxial * this.coneTan;
			return Double.isFinite(radialDistance) && Double.isFinite(jetRadius) && radialDistance <= jetRadius + Math.max(0.0D, targetRadius);
		}

		private BlastSample sample(Vec3 globalPoint, double targetRadius) {
			Vec3 offset = globalPoint.subtract(this.muzzlePosition);
			double axialDistance = offset.dot(this.forward);
			if (!Double.isFinite(axialDistance) || axialDistance <= 0.0D || axialDistance > this.range) return null;

			Vec3 axial = this.forward.scale(axialDistance);
			Vec3 radial = offset.subtract(axial);
			double jetRadius = this.boreRadius + axialDistance * this.coneTan;
			if (!Double.isFinite(jetRadius) || jetRadius <= 0.0D) return null;

			double effectiveRadialDistance = Math.max(0.0D, radial.length() - Math.max(0.0D, targetRadius) * 0.5D);
			double eta = effectiveRadialDistance / jetRadius;
			if (!Double.isFinite(eta) || eta > 1.0D) return null;

			double exponent = Config.muzzleBlastProfileExponent();
			if (!Double.isFinite(exponent) || exponent < 0.0D) return null;

			double base = 1.0D - eta * eta;
			double radialProfile = Math.pow(base, exponent);
			double profileWeight = (exponent + 1.0D) * radialProfile / (Math.PI * jetRadius * jetRadius);
			if (!Double.isFinite(profileWeight) || profileWeight <= 0.0D) return null;

			Vec3 flowDirection = ImpulseMath.isUsefulVector(offset) ? offset.normalize() : this.forward;
			return new BlastSample(axialDistance, jetRadius, profileWeight, flowDirection);
		}

		private Target entityTarget(EntityCandidate candidate) {
			Entity entity = candidate.entity();
			AABB box = entity.getBoundingBox();
			Vec3 localCenter = boxCenter(box);
			SubLevelAccess trackingSubLevel = candidate.subLevelHint();
			Vec3 globalCenter = trackingSubLevel == null ? localCenter : trackingSubLevel.logicalPose().transformPosition(localCenter);
			if (!ImpulseMath.isFinite(globalCenter)) return null;

			double radius = boxRadius(box);
			if (!Double.isFinite(radius) || radius <= 0.0D) return null;

			Vec3 localForward = trackingSubLevel == null ? this.forward : trackingSubLevel.logicalPose().transformNormalInverse(this.forward);
			double projectedArea = projectedBoxArea(localForward, box);
			if (!Double.isFinite(projectedArea) || projectedArea <= 0.0D) return null;

			double volume = box.getXsize() * box.getYsize() * box.getZsize();
			if (!Double.isFinite(volume) || volume <= 0.0D) return null;
			return new Target(entity, box, trackingSubLevel, globalCenter, radius, projectedArea, volume);
		}

		private SubLevelAccess entitySubLevel(Entity entity, Vec3 localCenter) {
			SubLevelAccess trackingSubLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);
			if (trackingSubLevel != null) return trackingSubLevel;

			if (ImpulseMath.isFinite(localCenter)) {
				return SableCompanion.INSTANCE.getContaining(entity.level(), localCenter);
			}

			return null;
		}

		private List<Vec3> entitySamplePoints(Target target) {
			List<Vec3> points = new ArrayList<>(5);
			points.add(target.center());

			Vec3 basisA = perpendicular(this.forward);
			Vec3 basisB = ImpulseMath.isUsefulVector(basisA) ? this.forward.cross(basisA).normalize() : Vec3.ZERO;
			double offset = target.radius() * 0.65D;
			if (ImpulseMath.isUsefulVector(basisA)) {
				points.add(target.center().add(basisA.scale(offset)));
				points.add(target.center().subtract(basisA.scale(offset)));
			}
			if (ImpulseMath.isUsefulVector(basisB)) {
				points.add(target.center().add(basisB.scale(offset)));
				points.add(target.center().subtract(basisB.scale(offset)));
			}
			return points;
		}

		private double visibilityToTarget(ServerLevel level, List<SubLevelAccess> candidateSubLevels, List<Vec3> points, boolean strict, Vec3 source) {
			if (points == null || points.isEmpty()) return 0.0D;
			int visible = 0;
			int total = 0;
			for (Vec3 point : points) {
				if (!ImpulseMath.isFinite(point)) continue;
				double distance = point.distanceTo(source);
				if (!Double.isFinite(distance) || distance <= RAY_EPSILON) continue;
				total++;
				if (!this.hasSolidOccluderBefore(level, candidateSubLevels, source, point, distance, strict)) visible++;
			}
			return total <= 0 ? 0.0D : (double) visible / (double) total;
		}

		private double visibilityToTarget(ServerLevel level, List<SubLevelAccess> candidateSubLevels, List<Vec3> points, boolean strict) {
			return this.visibilityToTarget(level, candidateSubLevels, points, strict, this.muzzlePosition);
		}

		private double cachedVisibilityToTarget(
				ServerLevel level,
				List<SubLevelAccess> candidateSubLevels,
				int sourceId,
				Target target,
				boolean strict,
				Vec3 source
		) {
			int entityId = target.entity().getId();
			long key = (((long) sourceId) << 32) ^ (entityId & 0xFFFFFFFFL);

			Double cached = this.state.visibilityCache.get(key);
			if (cached != null) return cached;

			double visibility = this.visibilityToTarget(
					level,
					candidateSubLevels,
					this.entitySamplePoints(target),
					strict,
					source
			);

			if (Double.isFinite(visibility)) this.state.visibilityCache.put(key, visibility);
			return visibility;
		}

		private double shockDirectivity(Vec3 direction) {
			double bias = Config.muzzleShockForwardBias();
			if (!Double.isFinite(bias) || bias <= 0.0D) return 1.0D;
			double forwardPart = Math.max(0.0D, direction.normalize().dot(this.forward));
			double normalization = 1.0D + bias * 0.25D;
			return (1.0D + bias * forwardPart) / normalization;
		}

		private double shockEntityLoadedFraction(Target target, Vec3 direction) {
			double absorptionCoefficient = Config.muzzleShockEntityAbsorptionCoefficient();
			double reflectionCoefficient = Config.muzzleShockEntityReflectionCoefficient();
			if (!Double.isFinite(absorptionCoefficient) || absorptionCoefficient < 0.0D) absorptionCoefficient = 0.0D;
			if (!Double.isFinite(reflectionCoefficient) || reflectionCoefficient < 0.0D) reflectionCoefficient = 0.0D;
			double coefficientSum = absorptionCoefficient + reflectionCoefficient;
			if (coefficientSum <= 0.0D) return 0.0D;

			double projectedArea = target.projectedAreaFor(direction);
			if (!Double.isFinite(projectedArea) || projectedArea <= 0.0D) return 0.0D;
			double pathLength = target.volume() / projectedArea;
			double characteristicWidth = Math.sqrt(projectedArea);
			if (!Double.isFinite(pathLength) || pathLength <= 0.0D || !Double.isFinite(characteristicWidth) || characteristicWidth <= 0.0D) return 0.0D;

			double thicknessFactor = pathLength / characteristicWidth;
			double interactionFraction = 1.0D - Math.exp(-coefficientSum * thicknessFactor);
			if (!Double.isFinite(interactionFraction) || interactionFraction <= 0.0D) return 0.0D;
			return interactionFraction;
		}

		private ShockLoading shockLoading(double energyJ, double pathDistance, boolean negativePhase) {
			if (!Double.isFinite(energyJ) || energyJ <= 0.0D) return null;
			if (!Double.isFinite(pathDistance) || pathDistance <= RAY_EPSILON) return null;

			double airDensity = Config.muzzleShockAirDensity();
			double soundSpeed = Config.muzzleShockSoundSpeed();
			double duration = Config.muzzleShockPositivePhaseSeconds();
			if (negativePhase) duration *= Config.muzzleShockNegativePhaseDurationMultiplier();
			if (!Double.isFinite(airDensity) || airDensity <= 0.0D || !Double.isFinite(soundSpeed) || soundSpeed <= 0.0D || !Double.isFinite(duration) || duration <= 0.0D) return null;

			double area = 4.0D * Math.PI * pathDistance * pathDistance;
			if (!Double.isFinite(area) || area <= 0.0D) return null;

			double pressureImpulsePaS = Math.sqrt(energyJ * airDensity * soundSpeed * duration / area);
			if (!Double.isFinite(pressureImpulsePaS) || pressureImpulsePaS <= 0.0D) return null;

			double impulseScale = Config.muzzleShockImpulseScale();
			if (!Double.isFinite(impulseScale) || impulseScale <= 0.0D) return null;
			pressureImpulsePaS *= impulseScale;
			if (!Double.isFinite(pressureImpulsePaS) || pressureImpulsePaS <= 0.0D) return null;

			double peakPressurePa = pressureImpulsePaS / duration;
			if (!Double.isFinite(peakPressurePa) || peakPressurePa <= 0.0D) return null;
			return new ShockLoading(pressureImpulsePaS, peakPressurePa);
		}

		private double shockDamage(Entity entity, double pressureImpulsePaS, double peakPressurePa, double impulseNs) {
			double damage = 0.0D;

			double pressureImpulsePerDamage = Config.muzzleShockPressureImpulsePerDamage();
			if (Double.isFinite(pressureImpulsePerDamage) && pressureImpulsePerDamage > 0.0D && Double.isFinite(pressureImpulsePaS) && pressureImpulsePaS > 0.0D) damage += pressureImpulsePaS / pressureImpulsePerDamage;

			double peakPressurePerDamage = Config.muzzleShockPeakPressurePerDamage();
			if (Double.isFinite(peakPressurePerDamage) && peakPressurePerDamage > 0.0D && Double.isFinite(peakPressurePa) && peakPressurePa > 0.0D) damage += peakPressurePa / peakPressurePerDamage;

			double kineticEnergyPerDamage = Config.muzzleBlastKineticEnergyPerDamage();
			if (Double.isFinite(kineticEnergyPerDamage) && kineticEnergyPerDamage > 0.0D && Double.isFinite(impulseNs) && impulseNs > 0.0D) {
				double massKg = estimateEntityMassKg(entity);
				if (Double.isFinite(massKg) && massKg > 0.0D) {
					double kineticEnergyJ = impulseNs * impulseNs / (2.0D * massKg);
					if (Double.isFinite(kineticEnergyJ) && kineticEnergyJ > 0.0D) damage += kineticEnergyJ / kineticEnergyPerDamage;
				}
			}

			return Double.isFinite(damage) && damage > 0.0D ? damage : 0.0D;
		}

		private double shockEnergyJ() {
			if (!Double.isFinite(this.gasMassKg) || this.gasMassKg <= 0.0D) return 0.0D;
			double specificEnergy = Config.muzzleBlastPowderSpecificEnergy();
			double shockEfficiency = Config.muzzleShockEfficiency();
			if (!Double.isFinite(specificEnergy) || specificEnergy <= 0.0D || !Double.isFinite(shockEfficiency) || shockEfficiency <= 0.0D) return 0.0D;
			double energy = this.gasMassKg * specificEnergy * shockEfficiency;
			return Double.isFinite(energy) && energy > 0.0D ? energy : 0.0D;
		}

		private AABB coneSearchBox() {
			Vec3 end = this.muzzlePosition.add(this.forward.scale(this.range));
			double maxRadius = this.boreRadius + this.range * this.coneTan;
			double minX = Math.min(this.muzzlePosition.x, end.x) - maxRadius;
			double minY = Math.min(this.muzzlePosition.y, end.y) - maxRadius;
			double minZ = Math.min(this.muzzlePosition.z, end.z) - maxRadius;
			double maxX = Math.max(this.muzzlePosition.x, end.x) + maxRadius;
			double maxY = Math.max(this.muzzlePosition.y, end.y) + maxRadius;
			double maxZ = Math.max(this.muzzlePosition.z, end.z) + maxRadius;
			return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}

		private AABB sphericalSearchBox(Vec3 center, double radius) {
			return new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
		}

		private List<SubLevelAccess> candidateSubLevels(ServerLevel level, AABB searchBox) {
			List<SubLevelAccess> candidateSubLevels = new ArrayList<>();
			for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(searchBox))) {
				candidateSubLevels.add(subLevel);
			}
			return candidateSubLevels;
		}

		private List<EntityCandidate> candidateEntities(ServerLevel level, AABB globalSearchBox, List<SubLevelAccess> candidateSubLevels) {
			Map<Integer, EntityCandidate> candidates = new HashMap<>();
			this.addCandidateEntities(level, globalSearchBox, candidates, null);

			for (SubLevelAccess subLevel : candidateSubLevels) {
				AABB localSearchBox = this.globalBoxToSubLevelLocalBox(subLevel, globalSearchBox);
				if (localSearchBox != null) this.addCandidateEntities(level, localSearchBox, candidates, subLevel);
			}

			return new ArrayList<>(candidates.values());
		}

		private void addCandidateEntities(ServerLevel level, AABB searchBox, Map<Integer, EntityCandidate> candidates, SubLevelAccess subLevelHint) {
			for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox, entity -> entity.getId() != this.excludedEntityId && entity.isAlive())) {
				EntityCandidate existing = candidates.get(entity.getId());
				if (existing == null) {
					candidates.put(entity.getId(), new EntityCandidate(entity, subLevelHint));
					continue;
				}

				if (existing.subLevelHint() == null && subLevelHint != null && this.shouldPreferSubLevelEntityCandidate(entity, subLevelHint)) {
					candidates.put(entity.getId(), new EntityCandidate(entity, subLevelHint));
				}
			}
		}

		private boolean shouldPreferSubLevelEntityCandidate(Entity entity, SubLevelAccess subLevel) {
			if (entity == null || subLevel == null) return false;
			AABB rawBox = entity.getBoundingBox();
			AABB globalSubLevelBox = subLevel.boundingBox().toMojang();
			if (rawBox.intersects(globalSubLevelBox)) return false;

			AABB transformedBox = this.transformLocalEntityBoxToGlobal(subLevel, rawBox);
			return transformedBox != null && transformedBox.intersects(globalSubLevelBox);
		}

		private AABB transformLocalEntityBoxToGlobal(SubLevelAccess subLevel, AABB localBox) {
			Vec3[] corners = new Vec3[] {
					new Vec3(localBox.minX, localBox.minY, localBox.minZ),
					new Vec3(localBox.minX, localBox.minY, localBox.maxZ),
					new Vec3(localBox.minX, localBox.maxY, localBox.minZ),
					new Vec3(localBox.minX, localBox.maxY, localBox.maxZ),
					new Vec3(localBox.maxX, localBox.minY, localBox.minZ),
					new Vec3(localBox.maxX, localBox.minY, localBox.maxZ),
					new Vec3(localBox.maxX, localBox.maxY, localBox.minZ),
					new Vec3(localBox.maxX, localBox.maxY, localBox.maxZ)
			};

			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			double maxZ = Double.NEGATIVE_INFINITY;

			for (Vec3 corner : corners) {
				Vec3 global = subLevel.logicalPose().transformPosition(corner);
				if (!ImpulseMath.isFinite(global)) return null;
				if (global.x < minX) minX = global.x;
				if (global.y < minY) minY = global.y;
				if (global.z < minZ) minZ = global.z;
				if (global.x > maxX) maxX = global.x;
				if (global.y > maxY) maxY = global.y;
				if (global.z > maxZ) maxZ = global.z;
			}

			if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ) || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)) return null;
			return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}

		private AABB globalBoxToSubLevelLocalBox(SubLevelAccess subLevel, AABB globalBox) {
			Vec3[] corners = new Vec3[] {
					new Vec3(globalBox.minX, globalBox.minY, globalBox.minZ),
					new Vec3(globalBox.minX, globalBox.minY, globalBox.maxZ),
					new Vec3(globalBox.minX, globalBox.maxY, globalBox.minZ),
					new Vec3(globalBox.minX, globalBox.maxY, globalBox.maxZ),
					new Vec3(globalBox.maxX, globalBox.minY, globalBox.minZ),
					new Vec3(globalBox.maxX, globalBox.minY, globalBox.maxZ),
					new Vec3(globalBox.maxX, globalBox.maxY, globalBox.minZ),
					new Vec3(globalBox.maxX, globalBox.maxY, globalBox.maxZ)
			};

			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			double maxZ = Double.NEGATIVE_INFINITY;

			for (Vec3 corner : corners) {
				Vec3 local = subLevel.logicalPose().transformPositionInverse(corner);
				if (!ImpulseMath.isFinite(local)) return null;
				if (local.x < minX) minX = local.x;
				if (local.y < minY) minY = local.y;
				if (local.z < minZ) minZ = local.z;
				if (local.x > maxX) maxX = local.x;
				if (local.y > maxY) maxY = local.y;
				if (local.z > maxZ) maxZ = local.z;
			}

			if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ) || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)) return null;
			return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}

		private boolean hasSolidOccluderBefore(ServerLevel level, List<SubLevelAccess> candidateSubLevels, Vec3 rayStart, Vec3 targetPoint, double targetDistance, boolean strictSolidOcclusion) {
			if (!ImpulseMath.isFinite(rayStart) || !ImpulseMath.isFinite(targetPoint)) return true;
			if (!Double.isFinite(targetDistance) || targetDistance <= 0.0D) return true;
			SurfaceHit blocker = this.nearestSolidHit(level, candidateSubLevels, rayStart, targetPoint, strictSolidOcclusion);
			if (blocker != null && blocker.travelDistance() < targetDistance - RAY_EPSILON) return true;
			return this.hasConservativeWorldOccluderBefore(level, rayStart, targetPoint, targetDistance);
		}

		private boolean hasConservativeWorldOccluderBefore(ServerLevel level, Vec3 rayStart, Vec3 targetPoint, double targetDistance) {
			Vec3 delta = targetPoint.subtract(rayStart);
			double length = delta.length();
			if (!Double.isFinite(length) || length <= RAY_EPSILON) return true;

			Vec3 direction = delta.scale(1.0D / length);
			if (!ImpulseMath.isUsefulVector(direction)) return true;

			double startDistance = Math.min(0.125D, Math.max(RAY_EPSILON, targetDistance * 0.1D));
			double endDistance = targetDistance - 0.125D;
			if (endDistance <= startDistance) return false;

			Vec3 start = rayStart.add(direction.scale(startDistance));
			Vec3 end = rayStart.add(direction.scale(endDistance));
			Vec3 walkDelta = end.subtract(start);
			double walkLength = walkDelta.length();
			if (!Double.isFinite(walkLength) || walkLength <= RAY_EPSILON) return false;

			int x = floorToBlock(start.x);
			int y = floorToBlock(start.y);
			int z = floorToBlock(start.z);
			int endX = floorToBlock(end.x);
			int endY = floorToBlock(end.y);
			int endZ = floorToBlock(end.z);

			int stepX = Integer.compare(endX, x);
			int stepY = Integer.compare(endY, y);
			int stepZ = Integer.compare(endZ, z);

			double tMaxX = initialRayStepT(start.x, walkDelta.x, x, stepX);
			double tMaxY = initialRayStepT(start.y, walkDelta.y, y, stepY);
			double tMaxZ = initialRayStepT(start.z, walkDelta.z, z, stepZ);
			double tDeltaX = rayStepDeltaT(walkDelta.x);
			double tDeltaY = rayStepDeltaT(walkDelta.y);
			double tDeltaZ = rayStepDeltaT(walkDelta.z);

			int maxSteps = Math.max(8, (int) Math.ceil(walkLength * 4.0D) + 8);
			BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
			for (int steps = 0; steps <= maxSteps; steps++) {
				mutablePos.set(x, y, z);
				if (this.isSolidWorldOccluder(level, mutablePos)) return true;
				if (x == endX && y == endY && z == endZ) break;

				if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
					x += stepX;
					tMaxX += tDeltaX;
				} else if (tMaxY <= tMaxZ) {
					y += stepY;
					tMaxY += tDeltaY;
				} else {
					z += stepZ;
					tMaxZ += tDeltaZ;
				}
			}

			return false;
		}

		private boolean isSolidWorldOccluder(ServerLevel level, BlockPos pos) {
			if (SableCompanion.INSTANCE.getContaining(level, pos) != null) return false;

			BlockState state = level.getBlockState(pos);
			if (state.isAir()) return false;

			VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
			return !shape.isEmpty();
		}

		private SurfaceHit firstHitOnSubLevel(ServerLevel level, List<SubLevelAccess> candidateSubLevels, SubLevelAccess targetSubLevel, Vec3 rayStart, Vec3 rayEnd, boolean strictSolidOcclusion) {
			if (targetSubLevel == null) return null;
			SurfaceHit hit = this.nearestSolidHit(level, candidateSubLevels, rayStart, rayEnd, strictSolidOcclusion);
			if (hit == null || hit.kind() != SurfaceKind.SUBLEVEL || hit.subLevel() == null) return null;
			return targetSubLevel.getUniqueId().equals(hit.subLevel().getUniqueId()) ? hit : null;
		}

		private SurfaceHit nearestSolidHit(ServerLevel level, List<SubLevelAccess> candidateSubLevels, Vec3 rayStart, Vec3 rayEnd, boolean strictSolidOcclusion) {
			SurfaceHit nearest = this.worldBlockHit(level, rayStart, rayEnd, strictSolidOcclusion);
			double nearestDistance = nearest == null ? Double.POSITIVE_INFINITY : nearest.travelDistance();

			for (SubLevelAccess subLevel : candidateSubLevels) {
				SurfaceHit hit = this.subLevelBlockHit(level, subLevel, rayStart, rayEnd, strictSolidOcclusion);
				if (hit != null && hit.travelDistance() < nearestDistance) {
					nearest = hit;
					nearestDistance = hit.travelDistance();
				}
			}

			return nearest;
		}

		private SurfaceHit worldBlockHit(ServerLevel level, Vec3 rayStart, Vec3 rayEnd, boolean strictSolidOcclusion) {
			BlockHitResult hit = level.clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity) null));
			if (hit.getType() == HitResult.Type.MISS) return null;

			SubLevelAccess hitSubLevel = SableCompanion.INSTANCE.getContaining(level, hit.getBlockPos());
			if (hitSubLevel != null) return null;

			Vec3 hitLocation = hit.getLocation();
			if (!ImpulseMath.isFinite(hitLocation)) return null;
			double muzzleDistance = hitLocation.distanceTo(this.muzzlePosition);
			double travelDistance = hitLocation.distanceTo(rayStart);
			if (!Double.isFinite(muzzleDistance) || muzzleDistance > this.maximumOcclusionRange()) return null;
			if (!strictSolidOcclusion && muzzleDistance <= RAY_EPSILON) return null;
			if (!Double.isFinite(travelDistance)) return null;
			if (!strictSolidOcclusion && travelDistance <= RAY_EPSILON) return null;
			Vec3 normal = blockNormal(hit);
			if (!ImpulseMath.isUsefulVector(normal)) normal = rayStart.subtract(rayEnd).normalize();
			return new SurfaceHit(SurfaceKind.WORLD_BLOCK, hitLocation, hitLocation, travelDistance, muzzleDistance, null, null, 0.0D, 0.0D, 0.0D, normal);
		}

		private SurfaceHit subLevelBlockHit(ServerLevel level, SubLevelAccess subLevel, Vec3 rayStart, Vec3 rayEnd, boolean strictSolidOcclusion) {
			AABB globalBox = subLevel.boundingBox().toMojang();
			Double boxDistance = rayBoxIntersectionDistance(rayStart, rayEnd, globalBox);
			if (boxDistance == null) return null;

			Vec3 localStart = subLevel.logicalPose().transformPositionInverse(rayStart);
			Vec3 localEnd = subLevel.logicalPose().transformPositionInverse(rayEnd);
			if (!ImpulseMath.isFinite(localStart) || !ImpulseMath.isFinite(localEnd)) return null;

			return this.subLevelBlockHitByVoxelWalk(level, subLevel, localStart, localEnd, rayStart, rayEnd, strictSolidOcclusion);
		}

		private SurfaceHit subLevelBlockHitByVoxelWalk(ServerLevel level, SubLevelAccess subLevel, Vec3 localStart, Vec3 localEnd, Vec3 globalRayStart, Vec3 globalRayEnd, boolean strictSolidOcclusion) {
			Vec3 localDelta = localEnd.subtract(localStart);
			double localLength = localDelta.length();
			if (!Double.isFinite(localLength) || localLength <= RAY_EPSILON) return null;

			int x = floorToBlock(localStart.x);
			int y = floorToBlock(localStart.y);
			int z = floorToBlock(localStart.z);
			int endX = floorToBlock(localEnd.x);
			int endY = floorToBlock(localEnd.y);
			int endZ = floorToBlock(localEnd.z);

			int stepX = Integer.compare(endX, x);
			int stepY = Integer.compare(endY, y);
			int stepZ = Integer.compare(endZ, z);

			double tMaxX = initialRayStepT(localStart.x, localDelta.x, x, stepX);
			double tMaxY = initialRayStepT(localStart.y, localDelta.y, y, stepY);
			double tMaxZ = initialRayStepT(localStart.z, localDelta.z, z, stepZ);
			double tDeltaX = rayStepDeltaT(localDelta.x);
			double tDeltaY = rayStepDeltaT(localDelta.y);
			double tDeltaZ = rayStepDeltaT(localDelta.z);

			int maxSteps = Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z) + 4;
			BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
			for (int step = 0; step < maxSteps; step++) {
				mutablePos.set(x, y, z);

				SurfaceHit hit = this.subLevelBlockCollisionAt(level, subLevel, mutablePos, localStart, localEnd, globalRayStart, globalRayEnd, strictSolidOcclusion);
				if (hit != null) return hit;

				if (x == endX && y == endY && z == endZ) break;

				if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
					x += stepX;
					tMaxX += tDeltaX;
				} else if (tMaxY <= tMaxZ) {
					y += stepY;
					tMaxY += tDeltaY;
				} else {
					z += stepZ;
					tMaxZ += tDeltaZ;
				}
			}

			return null;
		}

		private SurfaceHit subLevelBlockCollisionAt(ServerLevel level, SubLevelAccess subLevel, BlockPos pos, Vec3 localStart, Vec3 localEnd, Vec3 globalRayStart, Vec3 globalRayEnd, boolean strictSolidOcclusion) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) return null;

			VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
			if (shape.isEmpty()) return null;

			SubLevelAccess hitSubLevel = SableCompanion.INSTANCE.getContaining(level, pos);
			if (hitSubLevel == null || !subLevel.getUniqueId().equals(hitSubLevel.getUniqueId())) return null;

			BlockHitResult hit = shape.clip(localStart, localEnd, pos);
			if (hit == null || hit.getType() == HitResult.Type.MISS) return null;

			Vec3 globalHit = subLevel.logicalPose().transformPosition(hit.getLocation());
			if (!ImpulseMath.isFinite(globalHit)) return null;

			double muzzleDistance = globalHit.distanceTo(this.muzzlePosition);
			double travelDistance = globalHit.distanceTo(globalRayStart);
			if (!Double.isFinite(muzzleDistance) || muzzleDistance > this.maximumOcclusionRange()) return null;
			if (!strictSolidOcclusion && muzzleDistance <= RAY_EPSILON) return null;
			if (!Double.isFinite(travelDistance)) return null;
			if (!strictSolidOcclusion && travelDistance <= RAY_EPSILON) return null;

			Vec3 normal = subLevel.logicalPose().transformNormal(blockNormal(hit));
			if (!ImpulseMath.isUsefulVector(normal)) normal = globalRayStart.subtract(globalRayEnd).normalize();
			return new SurfaceHit(SurfaceKind.SUBLEVEL, globalHit, globalHit, travelDistance, muzzleDistance, null, subLevel, 0.0D, 0.0D, 0.0D, normal);
		}

		private double maximumOcclusionRange() {
			double shockRange = Config.muzzleShockRange();
			if (!Double.isFinite(shockRange) || shockRange <= 0.0D) return this.range;
			return Math.max(this.range, shockRange);
		}

		private void pushEntity(Entity entity, Vec3 impulseNs) {
			if (isBlastForceExempt(entity)) return;
			if (!ImpulseMath.isUsefulVector(impulseNs)) return;
			double massKg = estimateEntityMassKg(entity);
			if (!Double.isFinite(massKg) || massKg <= 0.0D) return;

			Vec3 deltaVelocityMps = impulseNs.scale(1.0D / massKg);
			if (!ImpulseMath.isUsefulVector(deltaVelocityMps)) return;

			Vec3 deltaMovementBpt = deltaVelocityMps.scale(SECONDS_PER_TICK);
			if (!ImpulseMath.isUsefulVector(deltaMovementBpt)) return;

			entity.setDeltaMovement(entity.getDeltaMovement().add(deltaMovementBpt));
			entity.hurtMarked = true;
		}

		private double mechanicalDamage(Entity entity, double impulseNs, double effectiveArea) {
			if (!Double.isFinite(impulseNs) || impulseNs <= 0.0D) return 0.0D;
			if (!Double.isFinite(effectiveArea) || effectiveArea <= 0.0D) return 0.0D;

			double damage = 0.0D;

			double forceDosePerDamage = Config.muzzleBlastForceDosePerDamage();
			if (Double.isFinite(forceDosePerDamage) && forceDosePerDamage > 0.0D) damage += impulseNs / forceDosePerDamage;

			double pressureImpulsePerDamage = Config.muzzleBlastPressureImpulsePerDamage();
			if (Double.isFinite(pressureImpulsePerDamage) && pressureImpulsePerDamage > 0.0D) {
				double pressureImpulse = impulseNs / effectiveArea;
				if (Double.isFinite(pressureImpulse) && pressureImpulse > 0.0D) damage += pressureImpulse / pressureImpulsePerDamage;
			}

			double kineticEnergyPerDamage = Config.muzzleBlastKineticEnergyPerDamage();
			if (Double.isFinite(kineticEnergyPerDamage) && kineticEnergyPerDamage > 0.0D) {
				double massKg = estimateEntityMassKg(entity);
				if (Double.isFinite(massKg) && massKg > 0.0D) {
					double kineticEnergyJ = impulseNs * impulseNs / (2.0D * massKg);
					if (Double.isFinite(kineticEnergyJ) && kineticEnergyJ > 0.0D) damage += kineticEnergyJ / kineticEnergyPerDamage;
				}
			}

			return Double.isFinite(damage) && damage > 0.0D ? damage : 0.0D;
		}

		private double thermalDamage(double heatDosePerSquareMeter) {
			if (!Double.isFinite(heatDosePerSquareMeter) || heatDosePerSquareMeter <= 0.0D) return 0.0D;
			double thermalDosePerDamage = Config.muzzleBlastThermalDosePerDamage();
			if (!Double.isFinite(thermalDosePerDamage) || thermalDosePerDamage <= 0.0D) return 0.0D;
			double damage = heatDosePerSquareMeter / thermalDosePerDamage;
			return Double.isFinite(damage) && damage > 0.0D ? damage : 0.0D;
		}

		private void damageEntity(ServerLevel level, Entity entity, double damage, BlastDamageKind kind) {
			if (!Double.isFinite(damage) || damage <= 0.0D) return;
			float damageAmount = finitePositiveFloat(damage);
			if (damageAmount <= 0.0F) return;

			if (kind == BlastDamageKind.BURN) {
				entity.hurt(EODamageTypes.cannonBlastBurn(level), damageAmount);
			} else if (kind == BlastDamageKind.SHOCK) {
				entity.hurt(EODamageTypes.cannonBlastShock(level), damageAmount);
			} else {
				entity.hurt(EODamageTypes.cannonBlastGas(level), damageAmount);
			}
		}

		private HeatSample heatDoseThisStep(BlastSample sample, double thermalGasKg, double effectiveArea, double absorbedFraction) {
			if (!Double.isFinite(thermalGasKg) || thermalGasKg <= 0.0D) return null;
			if (!Double.isFinite(effectiveArea) || effectiveArea <= 0.0D) return null;
			if (!Double.isFinite(absorbedFraction) || absorbedFraction <= 0.0D) return null;

			double localTemperatureK = this.localTemperature(sample.jetRadius());
			if (!Double.isFinite(localTemperatureK)) return null;

			double bodyTemperatureK = Config.muzzleBlastBodyTemperature();
			if (!Double.isFinite(bodyTemperatureK)) return null;

			double deltaTemperature = localTemperatureK - bodyTemperatureK;
			if (!Double.isFinite(deltaTemperature) || deltaTemperature <= 0.0D) return null;

			double gasCp = Config.muzzleBlastGasCp();
			double heatTransferEfficiency = Config.muzzleBlastHeatTransferEfficiency();
			if (!Double.isFinite(gasCp) || gasCp <= 0.0D || !Double.isFinite(heatTransferEfficiency) || heatTransferEfficiency <= 0.0D) return null;

			double gasMassPerAreaThisStep = thermalGasKg / effectiveArea;
			if (!Double.isFinite(gasMassPerAreaThisStep) || gasMassPerAreaThisStep <= 0.0D) return null;

			double heatDoseThisStep = heatTransferEfficiency * absorbedFraction * gasMassPerAreaThisStep * gasCp * deltaTemperature;
			if (!Double.isFinite(heatDoseThisStep) || heatDoseThisStep <= 0.0D) return null;
			return new HeatSample(heatDoseThisStep, localTemperatureK);
		}

		private void heatEntity(Entity entity, double heatDoseThisStep) {
			if (!Double.isFinite(heatDoseThisStep) || heatDoseThisStep <= 0.0D) return;

			ThermalState state = this.state.thermalStates.computeIfAbsent(entity.getId(), ignored -> new ThermalState());
			double previousDose = state.heatDosePerSquareMeter;
			double newDose = previousDose + heatDoseThisStep;
			if (!Double.isFinite(newDose)) return;
			state.heatDosePerSquareMeter = newDose;

			double ignitionDose = Config.muzzleBlastIgnitionDose();
			double burnDosePerSecond = Config.muzzleBlastBurnDosePerSecond();
			if (!Double.isFinite(ignitionDose) || ignitionDose < 0.0D || !Double.isFinite(burnDosePerSecond) || burnDosePerSecond <= 0.0D) return;
			if (newDose <= ignitionDose) return;

			double newlyBurningDose = previousDose <= ignitionDose ? newDose - ignitionDose : newDose - previousDose;
			if (!Double.isFinite(newlyBurningDose) || newlyBurningDose <= 0.0D) return;

			double burnTicksExact = newlyBurningDose * 20.0D / burnDosePerSecond + state.partialBurnTicks;
			if (!Double.isFinite(burnTicksExact) || burnTicksExact <= 0.0D) return;

			int addedTicks;
			if (burnTicksExact >= Integer.MAX_VALUE) {
				addedTicks = Integer.MAX_VALUE;
				state.partialBurnTicks = 0.0D;
			} else {
				addedTicks = (int) Math.floor(burnTicksExact);
				state.partialBurnTicks = burnTicksExact - addedTicks;
			}

			if (addedTicks <= 0) return;
			long totalFireTicks = (long) entity.getRemainingFireTicks() + addedTicks;
			entity.setRemainingFireTicks(totalFireTicks >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalFireTicks);
		}

		private double localTemperature(double jetRadius) {
			if (!Double.isFinite(this.gasMassKg) || this.gasMassKg <= 0.0D) return Double.NaN;
			double ambientTemperatureK = Config.muzzleBlastAmbientTemperature();
			double gasCv = Config.muzzleBlastGasCv();
			double specificEnergy = Config.muzzleBlastPowderSpecificEnergy();
			double thermalEfficiency = Config.muzzleBlastThermalEfficiency();
			if (!Double.isFinite(ambientTemperatureK) || !Double.isFinite(gasCv) || gasCv <= 0.0D || !Double.isFinite(specificEnergy) || !Double.isFinite(thermalEfficiency)) return Double.NaN;

			double thermalEnergyJ = this.gasMassKg * specificEnergy * thermalEfficiency;
			if (!Double.isFinite(thermalEnergyJ) || thermalEnergyJ <= 0.0D) return Double.NaN;

			double gasTemperatureK = ambientTemperatureK + thermalEnergyJ / (this.gasMassKg * gasCv);
			if (!Double.isFinite(gasTemperatureK)) return Double.NaN;

			double boreArea = Math.PI * this.boreRadius * this.boreRadius;
			double jetArea = Math.PI * jetRadius * jetRadius;
			if (!Double.isFinite(boreArea) || boreArea <= 0.0D || !Double.isFinite(jetArea) || jetArea <= 0.0D) return Double.NaN;

			double mixingExponent = Config.muzzleBlastTemperatureMixingExponent();
			if (!Double.isFinite(mixingExponent) || mixingExponent < 0.0D) return Double.NaN;

			return ambientTemperatureK + (gasTemperatureK - ambientTemperatureK) * Math.pow(boreArea / jetArea, mixingExponent);
		}

		private double solidShockReflectionFraction(double normalFactor) {
			double coefficient = Config.muzzleShockSolidReflectionCoefficient();
			if (!Double.isFinite(coefficient) || coefficient <= 0.0D) return 0.0D;
			if (!Double.isFinite(normalFactor) || normalFactor <= 0.0D) return 0.0D;
			return 1.0D - Math.exp(-coefficient * normalFactor);
		}

		private boolean cannonBlastDamageEnabled(ServerLevel level) {
			return level.getGameRules().getBoolean(EOGameRules.CANNON_BLAST_DAMAGE);
		}
	}

	private record EntityCandidate(Entity entity, SubLevelAccess subLevelHint) { }

	private record Target(Entity entity, AABB localBox, SubLevelAccess trackingSubLevel, Vec3 center, double radius, double projectedArea, double volume) {
		private double projectedAreaFor(Vec3 globalDirection) {
			Vec3 localDirection = this.trackingSubLevel == null ? globalDirection : this.trackingSubLevel.logicalPose().transformNormalInverse(globalDirection);
			return projectedBoxArea(localDirection, this.localBox);
		}
	}

	private record BlastSample(double axialDistance, double jetRadius, double profileWeight, Vec3 flowDirection) { }
	private record HeatSample(double heatDosePerSquareMeter, double localTemperatureK) { }
	private record ShockRayDef(Vec3 direction, double energyFraction) { }
	private record GasJetRayDef(Vec3 direction, double areaFraction) { }
	private record ShockLoading(double pressureImpulsePaS, double peakPressurePa) { }
	private record ImageShockSource(int id, Vec3 virtualSource, Vec3 reflectionPoint, double startDistance, double energyFraction) { }
	private record ShockSurfaceContact(UUID subLevelId, SubLevelAccess subLevel, Vec3 hitPoint, Vec3 normal, Vec3 incomingDirection, double pathDistance, double energyFraction) { }
	private record GasSurfaceContact(UUID subLevelId, SubLevelAccess subLevel, Vec3 hitPoint, Vec3 normal, Vec3 flowDirection, double areaFraction) { }

	private enum SurfaceKind {
		WORLD_BLOCK,
		SUBLEVEL
	}

	private enum BlastDamageKind {
		GAS,
		BURN,
		SHOCK
	}

	private record SurfaceHit(SurfaceKind kind, Vec3 globalPoint, Vec3 globalExitPoint, double travelDistance, double muzzleDistance, Entity entity, SubLevelAccess subLevel, double pathLength, double projectedArea, double characteristicWidth, Vec3 globalNormal) { }

	private static final class BlastState {
		private final Map<Integer, ThermalState> thermalStates = new HashMap<>();
		private final Set<Integer> positiveShockEntities = new HashSet<>();
		private final Set<Integer> negativeShockEntities = new HashSet<>();
		private final List<ImageShockSource> reflectionSources = new ArrayList<>();
		private final List<ShockSurfaceContact> shockSurfaceContacts = new ArrayList<>();
		private final List<GasSurfaceContact> gasSurfaceContacts = new ArrayList<>();
		private final Map<Long, Double> visibilityCache = new HashMap<>();
		private boolean reflectionSourcesBuilt;
		private boolean gasSurfaceContactsBuilt;
	}

	private static final class ThermalState {
		private double heatDosePerSquareMeter;
		private double partialBurnTicks;
	}

	private static final class EntityLoad {
		private final Entity entity;
		private final SubLevelAccess trackingSubLevel;
		private Vec3 globalImpulse = Vec3.ZERO;
		private double gasDamage;
		private double burnDamage;
		private double shockDamage;
		private double maxIgnitionHeatDoseThisTick;

		private EntityLoad(Entity entity, SubLevelAccess trackingSubLevel) {
			this.entity = entity;
			this.trackingSubLevel = trackingSubLevel;
		}

		private void addImpulse(Vec3 impulse) {
			this.globalImpulse = this.globalImpulse.add(impulse);
		}

		private void addGasDamage(double damage) {
			if (Double.isFinite(damage) && damage > 0.0D) this.gasDamage += damage;
		}

		private void addBurnDamage(double damage) {
			if (Double.isFinite(damage) && damage > 0.0D) this.burnDamage += damage;
		}

		private void addShockDamage(double damage) {
			if (Double.isFinite(damage) && damage > 0.0D) this.shockDamage += damage;
		}

		private double totalDamage() {
			double total = this.gasDamage + this.burnDamage + this.shockDamage;
			return Double.isFinite(total) && total > 0.0D ? total : 0.0D;
		}

		private BlastDamageKind primaryDamageKind() {
			if (this.burnDamage >= this.gasDamage && this.burnDamage >= this.shockDamage && this.burnDamage > 0.0D) return BlastDamageKind.BURN;
			if (this.shockDamage >= this.gasDamage && this.shockDamage > 0.0D) return BlastDamageKind.SHOCK;
			return BlastDamageKind.GAS;
		}

		private void addIgnitionHeatDose(double heatDose) {
			if (heatDose > this.maxIgnitionHeatDoseThisTick) this.maxIgnitionHeatDoseThisTick = heatDose;
		}
	}

	private static final class SubLevelLoad {
		private final SubLevelAccess subLevel;
		private Vec3 globalImpulse = Vec3.ZERO;
		private Vec3 impulseWeightedPoint = Vec3.ZERO;
		private double scalarImpulseNs;

		private SubLevelLoad(SubLevelAccess subLevel) {
			this.subLevel = subLevel;
		}

		private void add(Vec3 globalPoint, Vec3 impulse, double impulseMagnitudeNs) {
			this.globalImpulse = this.globalImpulse.add(impulse);
			this.impulseWeightedPoint = this.impulseWeightedPoint.add(globalPoint.scale(impulseMagnitudeNs));
			this.scalarImpulseNs += impulseMagnitudeNs;
		}
	}

	private static ShockRayDef[] buildShockRayDefs(Vec3 forward) {
		int rayCount = Config.muzzleShockRayCount();
		if (rayCount <= 0 || !ImpulseMath.isUsefulVector(forward)) return new ShockRayDef[0];
		Vec3 normalizedForward = forward.normalize();
		double bias = Config.muzzleShockForwardBias();
		if (!Double.isFinite(bias) || bias < 0.0D) bias = 0.0D;

		Vec3[] directions = new Vec3[rayCount];
		double[] weights = new double[rayCount];
		double totalWeight = 0.0D;
		double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
		for (int i = 0; i < rayCount; i++) {
			double y = 1.0D - 2.0D * ((double) i + 0.5D) / (double) rayCount;
			double radius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
			double theta = goldenAngle * i;
			Vec3 direction = new Vec3(Math.cos(theta) * radius, y, Math.sin(theta) * radius);
			if (!ImpulseMath.isUsefulVector(direction)) direction = normalizedForward;
			direction = direction.normalize();
			double weight = 1.0D + bias * Math.max(0.0D, direction.dot(normalizedForward));
			if (!Double.isFinite(weight) || weight <= 0.0D) weight = 1.0D;
			directions[i] = direction;
			weights[i] = weight;
			totalWeight += weight;
		}

		if (!Double.isFinite(totalWeight) || totalWeight <= 0.0D) return new ShockRayDef[0];
		ShockRayDef[] defs = new ShockRayDef[rayCount];
		for (int i = 0; i < rayCount; i++) {
			defs[i] = new ShockRayDef(directions[i], weights[i] / totalWeight);
		}
		return defs;
	}

	private static GasJetRayDef[] buildGasJetRayDefs(Vec3 forward, double coneTan) {
		if (!ImpulseMath.isUsefulVector(forward) || !Double.isFinite(coneTan) || coneTan <= 0.0D) return new GasJetRayDef[0];
		Vec3 normalizedForward = forward.normalize();
		Vec3 basisA = perpendicular(normalizedForward);
		if (!ImpulseMath.isUsefulVector(basisA)) return new GasJetRayDef[] { new GasJetRayDef(normalizedForward, 1.0D) };
		Vec3 basisB = normalizedForward.cross(basisA);
		if (!ImpulseMath.isUsefulVector(basisB)) return new GasJetRayDef[] { new GasJetRayDef(normalizedForward, 1.0D) };
		basisB = basisB.normalize();

		List<Vec3> directions = new ArrayList<>(13);
		directions.add(normalizedForward);
		addGasJetRingDirections(directions, normalizedForward, basisA, basisB, coneTan * 0.5D, 4);
		addGasJetRingDirections(directions, normalizedForward, basisA, basisB, coneTan * 0.9D, 8);

		double areaFraction = 1.0D / directions.size();
		GasJetRayDef[] defs = new GasJetRayDef[directions.size()];
		for (int i = 0; i < directions.size(); i++) {
			defs[i] = new GasJetRayDef(directions.get(i), areaFraction);
		}
		return defs;
	}

	private static void addGasJetRingDirections(List<Vec3> directions, Vec3 forward, Vec3 basisA, Vec3 basisB, double slope, int count) {
		if (directions == null || count <= 0 || !Double.isFinite(slope) || slope <= 0.0D) return;
		for (int i = 0; i < count; i++) {
			double angle = 2.0D * Math.PI * i / count;
			Vec3 radial = basisA.scale(Math.cos(angle) * slope).add(basisB.scale(Math.sin(angle) * slope));
			Vec3 direction = forward.add(radial);
			if (ImpulseMath.isUsefulVector(direction)) directions.add(direction.normalize());
		}
	}

	private static Vec3 perpendicular(Vec3 direction) {
		if (!ImpulseMath.isUsefulVector(direction)) return Vec3.ZERO;
		Vec3 normalized = direction.normalize();
		Vec3 candidate;
		if (Math.abs(normalized.x) > Math.abs(normalized.z)) {
			candidate = new Vec3(-normalized.y, normalized.x, 0.0D);
		} else {
			candidate = new Vec3(0.0D, -normalized.z, normalized.y);
		}
		return ImpulseMath.isUsefulVector(candidate) ? candidate.normalize() : Vec3.ZERO;
	}

	private static Vec3 boxCenter(AABB box) {
		return new Vec3((box.minX + box.maxX) * 0.5D, (box.minY + box.maxY) * 0.5D, (box.minZ + box.maxZ) * 0.5D);
	}

	private static double boxRadius(AABB box) {
		double dx = box.getXsize();
		double dy = box.getYsize();
		double dz = box.getZsize();
		return 0.5D * Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static int floorToBlock(double value) {
		return (int) Math.floor(value);
	}

	private static double initialRayStepT(double origin, double delta, int block, int step) {
		if (step == 0 || !Double.isFinite(delta) || Math.abs(delta) <= RAY_EPSILON) return Double.POSITIVE_INFINITY;
		double nextBoundary = step > 0 ? block + 1.0D : block;
		double t = (nextBoundary - origin) / delta;
		return Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
	}

	private static double rayStepDeltaT(double delta) {
		if (!Double.isFinite(delta) || Math.abs(delta) <= RAY_EPSILON) return Double.POSITIVE_INFINITY;
		double t = Math.abs(1.0D / delta);
		return Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
	}

	private static double projectedBoxArea(Vec3 direction, AABB box) {
		if (!ImpulseMath.isUsefulVector(direction)) return 0.0D;
		Vec3 dir = direction.normalize();
		double dx = box.getXsize();
		double dy = box.getYsize();
		double dz = box.getZsize();
		if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz) || dx <= 0.0D || dy <= 0.0D || dz <= 0.0D) return 0.0D;
		return Math.abs(dir.x) * dy * dz + Math.abs(dir.y) * dx * dz + Math.abs(dir.z) * dx * dy;
	}

	private static Vec3 blockNormal(BlockHitResult hit) {
		if (hit == null) return Vec3.ZERO;
		return new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
	}

	private static Vec3 orientedSurfaceNormal(SurfaceHit hit, Vec3 rayDirection) {
		Vec3 normal = hit.globalNormal();
		if (!ImpulseMath.isUsefulVector(normal)) return Vec3.ZERO;
		normal = normal.normalize();
		if (ImpulseMath.isUsefulVector(rayDirection) && rayDirection.dot(normal) > 0.0D) normal = normal.scale(-1.0D);
		return normal;
	}

	private static Double rayBoxIntersectionDistance(Vec3 from, Vec3 to, AABB box) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (!Double.isFinite(length) || length <= 0.0D) return null;

		double tMin = 0.0D;
		double tMax = 1.0D;

		double[] origins = { from.x, from.y, from.z };
		double[] directions = { delta.x, delta.y, delta.z };
		double[] mins = { box.minX, box.minY, box.minZ };
		double[] maxs = { box.maxX, box.maxY, box.maxZ };

		for (int axis = 0; axis < 3; axis++) {
			double origin = origins[axis];
			double direction = directions[axis];
			double min = mins[axis];
			double max = maxs[axis];

			if (Math.abs(direction) <= RAY_EPSILON) {
				if (origin < min || origin > max) return null;
				continue;
			}

			double inv = 1.0D / direction;
			double t1 = (min - origin) * inv;
			double t2 = (max - origin) * inv;
			if (t1 > t2) {
				double swap = t1;
				t1 = t2;
				t2 = swap;
			}

			if (t1 > tMin) tMin = t1;
			if (t2 < tMax) tMax = t2;
			if (tMax < tMin) return null;
		}

		if (!Double.isFinite(tMin)) return null;
		return tMin * length;
	}

	private static Vec3 mirrorAcrossPlane(Vec3 point, Vec3 planePoint, Vec3 planeNormal) {
		if (!ImpulseMath.isFinite(point) || !ImpulseMath.isFinite(planePoint) || !ImpulseMath.isUsefulVector(planeNormal)) return Vec3.ZERO;
		Vec3 normal = planeNormal.normalize();
		double signedDistance = point.subtract(planePoint).dot(normal);
		return point.subtract(normal.scale(2.0D * signedDistance));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(Math.min(value, max), min);
	}

	private static boolean isBlastForceExempt(Entity entity) {
		if (entity == null) return true;
		if (entity.isSpectator()) return true;
		return entity instanceof Player player && player.getAbilities().flying;
	}

	private static boolean canReceiveBlastDamage(Entity entity) {
		if (entity == null) return false;
		if (isBlastForceExempt(entity)) return false;
		return !(entity instanceof ItemEntity);
	}

	private static double estimateEntityMassKg(Entity entity) {
		AABB box = entity.getBoundingBox();
		double volume = box.getXsize() * box.getYsize() * box.getZsize();
		if (!Double.isFinite(volume) || volume <= 0.0D) return 0.0D;

		double density = Config.muzzleBlastEntityDensity();
		if (!Double.isFinite(density) || density <= 0.0D) return 0.0D;

		double mass = volume * density;
		return Double.isFinite(mass) && mass > 0.0D ? mass : 0.0D;
	}

	private static float finitePositiveFloat(double value) {
		if (!Double.isFinite(value) || value <= 0.0D) return 0.0F;
		if (value >= Float.MAX_VALUE) return Float.MAX_VALUE;
		return (float) value;
	}
}
