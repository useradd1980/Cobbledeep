package dev.cobbledeep.monster;

import dev.cobbledeep.combat.GiantRatCombatRounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A placed, hostile Cobbledeep creature whose corpse and loot survive world saves. */
public final class GiantRatEntity extends PathfinderMob {
    public static final int DEATH_ANIMATION_TICKS = 8;
    // Durations correspond to the non-looping detection, attack and hit clips
    // exported from the Giant Rat Blockbench project (20 game ticks per second).
    public static final int DETECTION_ANIMATION_TICKS = 9;
    public static final int ATTACK_ANIMATION_TICKS = 13;
    public static final int HIT_ANIMATION_TICKS = 9;

    private static final String CORPSE_TAG = "CobbledeepGiantRatCorpse";
    private static final String LOOT_CREATED_TAG = "CobbledeepGiantRatLootCreated";
    private static final String LOOT_ITEMS_TAG = "CobbledeepGiantRatLoot";
    private static final EntityDataAccessor<Integer> DEATH_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DETECTION_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);
    // Client rendering/auto-attack reads these as hints; the server alone spends actions.
    private static final EntityDataAccessor<Integer> COMBAT_PLAYER_ID =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEXT_PLAYER_ATTACK_AT =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);

    private final GiantRatCombatRounds combatRounds = new GiantRatCombatRounds();

    // Each corpse owns its own server-authoritative, persistent inventory.
    private final SimpleContainer corpseLoot = new SimpleContainer(27) {
        @Override
        public boolean stillValid(Player player) {
            return GiantRatEntity.this.canLootFrom(player);
        }
    };
    private boolean lootCreated;

    public GiantRatEntity(EntityType<? extends GiantRatEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEATH_TICKS, 0);
        builder.define(DETECTION_TICKS, 0);
        builder.define(ATTACK_TICKS, 0);
        builder.define(HIT_TICKS, 0);
        builder.define(COMBAT_PLAYER_ID, 0);
        builder.define(NEXT_PLAYER_ATTACK_AT, 0);
    }

    public int getDeathAnimationTicks() {
        return entityData.get(DEATH_TICKS);
    }

    public int getDetectionAnimationTicks() {
        return entityData.get(DETECTION_TICKS);
    }

    public int getAttackAnimationTicks() {
        return entityData.get(ATTACK_TICKS);
    }

    public int getHitAnimationTicks() {
        return entityData.get(HIT_TICKS);
    }

    /** Server-only update of the active opponent and their next attack opportunity. */
    public void setCombatPlayerWindow(int playerId, int earliestTick) {
        if (level().isClientSide) return;
        entityData.set(COMBAT_PLAYER_ID, playerId);
        entityData.set(NEXT_PLAYER_ATTACK_AT, earliestTick);
    }

    /** Cosmetic client-side gate: do not swing repeatedly while waiting for the round. */
    public boolean playerAttackWindowOpen(Player player) {
        int opponentId = entityData.get(COMBAT_PLAYER_ID);
        if (opponentId == 0) return true; // First contact creates the encounter on the server.
        if (player == null || opponentId != player.getId()) return false;
        int due = entityData.get(NEXT_PLAYER_ATTACK_AT);
        return due != Integer.MAX_VALUE && level().getGameTime() >= (long) due;
    }

    /** Server-owned action gate; invoked before the THAC0 hit roll, including misses. */
    public boolean tryPlayerMeleeAttack(ServerPlayer attacker) {
        return combatRounds.tryPlayerAttack(this, attacker);
    }

    public boolean isCorpse() {
        return isDeadOrDying() || getDeathAnimationTicks() > 0;
    }

    /** Loot is allowed only from the corpse's block or a neighbouring block.
     * Both the client approach controller and server use this exact rule.
     */
    public boolean canLootFrom(Player player) {
        if (player == null || player.level() != level() || !player.isAlive()
                || !isCorpse() || isRemoved()) return false;
        var playerBlock = player.blockPosition();
        var corpseBlock = blockPosition();
        return Math.abs(playerBlock.getX() - corpseBlock.getX()) <= 1
                && Math.abs(playerBlock.getZ() - corpseBlock.getZ()) <= 1
                && Math.abs(playerBlock.getY() - corpseBlock.getY()) <= 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Acquiring a target triggers a short detection animation before pursuit.
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
            @Override
            public boolean canUse() {
                return getDetectionAnimationTicks() == 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getDetectionAnimationTicks() == 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.65, 45) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getTarget() == null && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Vanilla's target goal handles line-of-sight and excludes untargetable
        // players; FOLLOW_RANGE controls how far the rat can notice a player.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity previous = getTarget();
        super.setTarget(target);
        if (!level().isClientSide && !isCorpse()) {
            if (target != null && target != previous) {
                entityData.set(DETECTION_TICKS, DETECTION_ANIMATION_TICKS);
                entityData.set(ATTACK_TICKS, 0);
                getNavigation().stop();
            } else if (target == null) {
                entityData.set(DETECTION_TICKS, 0);
            }
            if (target instanceof Player player) combatRounds.begin(this, player);
            else if (target != previous) combatRounds.clear(this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isCorpse()) return;
        combatRounds.tick(this);
        // A single server-owned countdown is synchronized to every client, so
        // keyframe animations do not restart when the entity renderer is rebuilt.
        decrement(DETECTION_TICKS);
        decrement(ATTACK_TICKS);
        decrement(HIT_TICKS);
        if (getDetectionAnimationTicks() > 0) {
            getNavigation().stop();
            // Stop horizontal wandering during the alert pose; leave gravity alone.
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0, movement.y, 0.0);
        }
    }

    private void decrement(EntityDataAccessor<Integer> accessor) {
        int remaining = entityData.get(accessor);
        if (remaining > 0) entityData.set(accessor, remaining - 1);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isCorpse() || !combatRounds.tryRatAttack(this, target)) return false;
        boolean damaged = super.doHurtTarget(target);
        if (damaged && !level().isClientSide) {
            // One synchronized attack animation per permitted melee attempt.
            entityData.set(ATTACK_TICKS, ATTACK_ANIMATION_TICKS);
            entityData.set(DETECTION_TICKS, 0);
        }
        return damaged;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        boolean hurt = super.hurt(source, damage);
        if (hurt && !level().isClientSide && !isCorpse()) {
            entityData.set(HIT_TICKS, HIT_ANIMATION_TICKS);
        }
        return hurt;
    }

    private void createCorpseLoot() {
        if (lootCreated) return;
        lootCreated = true;
        // Temporary test loot. Replace this with creature-specific loot tables later.
        corpseLoot.setItem(0, new ItemStack(Items.BONE));
    }

    /** Called on the server. A distant player cannot open a corpse through a packet. */
    public void openCorpseLoot(ServerPlayer player) {
        if (level().isClientSide || !canLootFrom(player)) return;
        createCorpseLoot(); // Also handles corpses saved before loot support existed.
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, viewer) ->
                        ChestMenu.threeRows(containerId, inventory, corpseLoot),
                Component.literal("Giant Rat — Remains")));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isCorpse()) return super.mobInteract(player, hand);
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            openCorpseLoot(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void die(DamageSource source) {
        if (!dead && !level().isClientSide) {
            combatRounds.clear(this);
            getNavigation().stop();
            setNoAi(true);
            setDeltaMovement(Vec3.ZERO);
            entityData.set(DEATH_TICKS, 0);
            entityData.set(DETECTION_TICKS, 0);
            entityData.set(ATTACK_TICKS, 0);
            entityData.set(HIT_TICKS, 0);
            createCorpseLoot();
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        // Keep the entity instead of vanilla's 20-tick removal.
        ++deathTime;
        if (!level().isClientSide && entityData.get(DEATH_TICKS) < DEATH_ANIMATION_TICKS) {
            entityData.set(DEATH_TICKS, Math.min(deathTime, DEATH_ANIMATION_TICKS));
        }
    }

    @Override
    public boolean isPickable() {
        // Dead rats remain selectable while lying on the ground.
        return isCorpse() || super.isPickable();
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
        tag.putBoolean(LOOT_CREATED_TAG, lootCreated);
        tag.put(LOOT_ITEMS_TAG, corpseLoot.createTag(level().registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lootCreated = tag.getBoolean(LOOT_CREATED_TAG);
        corpseLoot.clearContent();
        if (tag.contains(LOOT_ITEMS_TAG, 9)) {
            corpseLoot.fromTag(tag.getList(LOOT_ITEMS_TAG, 10), level().registryAccess());
        }
        if (tag.getBoolean(CORPSE_TAG)) {
            // Reloading a killed rat restores its final death pose and saved loot.
            setHealth(0.0f);
            setNoAi(true);
            deathTime = DEATH_ANIMATION_TICKS;
            entityData.set(DEATH_TICKS, DEATH_ANIMATION_TICKS);
            entityData.set(DETECTION_TICKS, 0);
            entityData.set(ATTACK_TICKS, 0);
            entityData.set(HIT_TICKS, 0);
            createCorpseLoot();
        }
    }
}
