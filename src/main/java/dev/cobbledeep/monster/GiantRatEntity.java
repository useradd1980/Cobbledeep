package dev.cobbledeep.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A placed Cobbledeep creature: it does not despawn, and its corpse remains in the save. */
public final class GiantRatEntity extends PathfinderMob {
    // The authored death clip lasts 0.375 seconds, i.e. 7.5 Minecraft ticks.
    public static final int DEATH_ANIMATION_TICKS = 8;
    private static final String CORPSE_TAG = "CobbledeepGiantRatCorpse";
    private static final EntityDataAccessor<Integer> DEATH_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);

    public GiantRatEntity(EntityType<? extends GiantRatEntity> type, Level level) {
        super(type, level);
        // Explicitly placed creatures are unique: prevent the normal distance-based despawn.
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEATH_TICKS, 0);
    }

    public int getDeathAnimationTicks() {
        return entityData.get(DEATH_TICKS);
    }

    public boolean isCorpse() {
        return isDeadOrDying() || getDeathAnimationTicks() > 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.65, 45));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void die(DamageSource source) {
        if (isCorpse()) {
            // The normal death method guards duplicate drops; don't restart the animation.
            super.die(source);
            return;
        }
        if (!level().isClientSide) {
            getNavigation().stop();
            setNoAi(true);
            setDeltaMovement(Vec3.ZERO);
            entityData.set(DEATH_TICKS, 0);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        // Do NOT call super.tickDeath(): vanilla discards a dead mob after 20 ticks.
        ++deathTime;
        if (!level().isClientSide && entityData.get(DEATH_TICKS) < DEATH_ANIMATION_TICKS) {
            entityData.set(DEATH_TICKS, Math.min(deathTime, DEATH_ANIMATION_TICKS));
        }
    }

    @Override
    public boolean isPickable() {
        return !isCorpse() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !isCorpse() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(CORPSE_TAG, isCorpse());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean(CORPSE_TAG)) {
            // Reloading an already defeated rat must restore the final death pose, not
            // resurrect it or replay its death each time the chunk is opened.
            setHealth(0.0f);
            setNoAi(true);
            deathTime = DEATH_ANIMATION_TICKS;
            entityData.set(DEATH_TICKS, DEATH_ANIMATION_TICKS);
        }
    }
}
