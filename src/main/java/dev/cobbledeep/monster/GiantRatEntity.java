package dev.cobbledeep.monster;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A placed Cobbledeep creature whose corpse and remaining loot survive world saves. */
public final class GiantRatEntity extends PathfinderMob {
    public static final int DEATH_ANIMATION_TICKS = 8;
    private static final String CORPSE_TAG = "CobbledeepGiantRatCorpse";
    private static final String LOOT_CREATED_TAG = "CobbledeepGiantRatLootCreated";
    private static final String LOOT_ITEMS_TAG = "CobbledeepGiantRatLoot";
    private static final EntityDataAccessor<Integer> DEATH_TICKS =
            SynchedEntityData.defineId(GiantRatEntity.class, EntityDataSerializers.INT);

    // A separate, server-owned inventory for each corpse. The ordinary chest menu
    // gives us working item transfer without changing Cobbledeep's player inventory.
    private final SimpleContainer corpseLoot = new SimpleContainer(27) {
        @Override
        public boolean stillValid(Player player) {
            return GiantRatEntity.this.isCorpse() && !GiantRatEntity.this.isRemoved()
                    && player.distanceToSqr(GiantRatEntity.this) <= 64.0;
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

    private void createCorpseLoot() {
        if (lootCreated) return;
        lootCreated = true;
        // Temporary test loot. Replace this with creature-specific loot tables later.
        corpseLoot.setItem(0, new ItemStack(Items.BONE));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isCorpse()) return super.mobInteract(player, hand);
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            createCorpseLoot(); // Also handles corpses saved before loot support existed.
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, viewer) ->
                            ChestMenu.threeRows(containerId, inventory, corpseLoot),
                    Component.literal("Giant Rat — Remains")));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void die(DamageSource source) {
        if (!dead && !level().isClientSide) {
            getNavigation().stop();
            setNoAi(true);
            setDeltaMovement(Vec3.ZERO);
            entityData.set(DEATH_TICKS, 0);
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
        // Dead rats must remain raycastable, so the player can open their loot.
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
        tag.put(LOOT_ITEMS_TAG, corpseLoot.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lootCreated = tag.getBoolean(LOOT_CREATED_TAG);
        corpseLoot.clearContent();
        if (tag.contains(LOOT_ITEMS_TAG, 9)) {
            corpseLoot.fromTag(tag.getList(LOOT_ITEMS_TAG, 10), registryAccess());
        }
        if (tag.getBoolean(CORPSE_TAG)) {
            // Reloading a killed rat restores its final death pose and saved loot.
            setHealth(0.0f);
            setNoAi(true);
            deathTime = DEATH_ANIMATION_TICKS;
            entityData.set(DEATH_TICKS, DEATH_ANIMATION_TICKS);
            createCorpseLoot();
        }
    }
}
