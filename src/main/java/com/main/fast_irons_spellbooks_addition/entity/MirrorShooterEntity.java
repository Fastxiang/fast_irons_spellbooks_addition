package com.main.fast_irons_spellbooks_addition.entity;

import com.main.fast_irons_spellbooks_addition.event.MirrorShooterAttackEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MirrorShooterEntity extends Mob {

    private static final EntityDataAccessor<String> SKIN_TEXTURE =
            SynchedEntityData.defineId(MirrorShooterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> REMAINING_LIFE =
            SynchedEntityData.defineId(MirrorShooterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(MirrorShooterEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> ATTACK_ANIM =
            SynchedEntityData.defineId(MirrorShooterEntity.class, EntityDataSerializers.FLOAT);

    private int attackAnimTimer = 0;

    private int attackCooldown = 0;
    private int bowChargeTicks = 0;
    private boolean bowCharging = false;
    private float damage = 1.0f; // 新增字段，用于近战及弓箭伤害

    @Nullable
    private UUID ownerUUID;

    public MirrorShooterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(false);
    }

    // 新增构造函数，可直接传入伤害值
    public MirrorShooterEntity(Level level, @Nullable LivingEntity owner, float damage) {
        this(FastEntityRegistry.MIRROR_SHOOTER.get(), level);
        this.setInvulnerable(true);
        this.setNoAi(false);
        this.setOwner(owner);
        this.damage = damage;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ShooterAttackGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_TEXTURE, "");
        this.entityData.define(REMAINING_LIFE, 1200);
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ATTACK_ANIM, 0.0F);
    }

    public float getAttackAnimProgress() {
        return this.entityData.get(ATTACK_ANIM);
    }

    // ---------- Owner (召唤者) ----------
    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.entityData.set(OWNER_UUID, Optional.ofNullable(this.ownerUUID));
    }

    @Nullable
    public UUID getOwnerUUID() {
        Optional<UUID> uuid = this.entityData.get(OWNER_UUID);
        return uuid.orElse(null);
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel) this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public static List<MirrorShooterEntity> getActiveForOwner(@Nullable UUID ownerUUID, Level level) {
        List<MirrorShooterEntity> list = new ArrayList<>();
        if (ownerUUID == null || level.isClientSide) return list;
        for (Entity entity : ((ServerLevel) level).getAllEntities()) {
            if (entity instanceof MirrorShooterEntity mirror && ownerUUID.equals(mirror.getOwnerUUID())) {
                list.add(mirror);
            }
        }
        return list;
    }

    @Nullable
    public static MirrorShooterEntity tryCreateForOwner(Level level, @Nullable LivingEntity owner, float damage, int max) {
        if (!level.isClientSide && owner != null) {
            List<MirrorShooterEntity> existing = getActiveForOwner(owner.getUUID(), level);
            while (existing.size() >= max) {
                // 逐个移除直到数量小于 max
                MirrorShooterEntity toRemove = existing.remove(0);
                toRemove.discard();
            }
        }
        MirrorShooterEntity newEntity = new MirrorShooterEntity(level, owner, damage);
        return newEntity;
    }

    // ---------- Skin ----------
    public void setSkinTexture(ResourceLocation location) {
        this.entityData.set(SKIN_TEXTURE, location != null ? location.toString() : "");
    }

    public ResourceLocation getSkinTexture() {
        if (this.level().isClientSide) {
            UUID ownerUuid = getOwnerUUID();
            if (ownerUuid != null) {
                Player player = Minecraft.getInstance().level.getPlayerByUUID(ownerUuid);
                if (player != null) {
                    return ((net.minecraft.client.player.AbstractClientPlayer) player).getSkinTextureLocation();
                }
            }
        }
        String s = this.entityData.get(SKIN_TEXTURE);
        if (s != null && !s.isEmpty()) {
            return new ResourceLocation(s);
        }
        return new ResourceLocation("textures/entity/steve.png");
    }

    // ---------- Life ----------
    public int getRemainingLife() {
        return this.entityData.get(REMAINING_LIFE);
    }

    public void setRemainingLife(int ticks) {
        this.entityData.set(REMAINING_LIFE, ticks);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setYya(0);
        this.xxa = 0;
        this.zza = 0;

        if (attackAnimTimer > 0) {
            attackAnimTimer--;
            this.entityData.set(ATTACK_ANIM, attackAnimTimer / 5.0F);
        } else {
            this.entityData.set(ATTACK_ANIM, 0.0F);
        }

        if (!this.level().isClientSide) {
            int life = this.getRemainingLife();
            if (life > 0) {
                this.setRemainingLife(life - 1);
            } else {
                this.discard();
            }
        }
    }

    public void triggerAttackAnimation() {
        this.attackAnimTimer = 5;   // 动画持续 5 tick，可自行调整
        this.entityData.set(ATTACK_ANIM, 1.0F);
    }

    // ---------- 不受伤害，不吸引仇恨 ----------
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------- 伤害字段 get / set ----------
    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    // ---------- 伤害逻辑（归属给召唤者） ----------
    protected void dealDamageTo(LivingEntity target) {
        float damageAmount = this.getDamage();
        LivingEntity owner = getOwner();
        DamageSource source;
        if (owner != null) {
            if (owner instanceof Player player) {
            source = this.damageSources().playerAttack(player);
            } else {
            source = this.damageSources().mobAttack(owner);
            }
        } else {
            source = this.damageSources().mobAttack(this);
        }

        // 创建并发布事件
        MirrorShooterAttackEvent event = new MirrorShooterAttackEvent(this, target, source, damageAmount);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            target.hurt(event.getDamageSource(), event.getDamage());
        }
    }

    // ---------- AI Goal：每 0.5 秒锁定最近怪物 ----------
    private class ShooterAttackGoal extends Goal {
        private final double followRange = 16.0;
        private final int meleeAttackRange = 3;
        private final int bowAttackRange = 8;
        private int scanCooldown = 0;

        public ShooterAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            bowCharging = false;
            bowChargeTicks = 0;
            scanCooldown = 0;
        }

        @Override
        public void stop() {
            if (bowCharging && MirrorShooterEntity.this.isUsingItem()) {
                MirrorShooterEntity.this.stopUsingItem();
            }
            bowCharging = false;
        }

        @Override
        public void tick() {
            if (scanCooldown <= 0) {
                LivingEntity currentTarget = MirrorShooterEntity.this.getTarget();
                LivingEntity nearest = findNearestMonster();

                if (nearest != currentTarget) {
                    if (bowCharging && MirrorShooterEntity.this.isUsingItem()) {
                        MirrorShooterEntity.this.stopUsingItem();
                        bowCharging = false;
                        bowChargeTicks = 0;
                    }
                    MirrorShooterEntity.this.setTarget(nearest);
                    currentTarget = nearest;
                }
                scanCooldown = 10;
            }
            scanCooldown--;

            LivingEntity currentTarget = MirrorShooterEntity.this.getTarget();
            if (currentTarget == null) {
                return;
            }

            ItemStack heldItem = MirrorShooterEntity.this.getMainHandItem();
            MirrorShooterEntity.this.getLookControl().setLookAt(currentTarget, 30.0f, 30.0f);

            if (heldItem.getItem() instanceof BowItem) {
                double distSq = MirrorShooterEntity.this.distanceToSqr(currentTarget);
                if (distSq <= bowAttackRange * bowAttackRange && MirrorShooterEntity.this.hasLineOfSight(currentTarget)) {
                    if (!bowCharging) {
                        MirrorShooterEntity.this.startUsingItem(InteractionHand.MAIN_HAND);
                        bowCharging = true;
                        bowChargeTicks = 0;
                    }
                    if (bowCharging) {
                        bowChargeTicks++;
                        if (bowChargeTicks >= 20) {
                            performBowAttack(currentTarget);
                            bowCharging = false;
                            bowChargeTicks = 0;
                            MirrorShooterEntity.this.stopUsingItem();
                        }
                    }
                } else {
                    if (bowCharging) {
                        MirrorShooterEntity.this.stopUsingItem();
                        bowCharging = false;
                    }
                }
            } else {
                double distSq = MirrorShooterEntity.this.distanceToSqr(currentTarget);
                if (distSq <= meleeAttackRange * meleeAttackRange) {
                    if (attackCooldown <= 0) {
                        MirrorShooterEntity.this.triggerAttackAnimation();
                        dealDamageTo(currentTarget);
                        attackCooldown = 20;
                    }
                }
            }

            if (attackCooldown > 0) attackCooldown--;
        }

        @Nullable
        private LivingEntity findNearestMonster() {
            Vec3 pos = MirrorShooterEntity.this.position();
            AABB aabb = new AABB(pos.x - followRange, pos.y - followRange, pos.z - followRange,
                    pos.x + followRange, pos.y + followRange, pos.z + followRange);
            List<Monster> monsters = MirrorShooterEntity.this.level().getEntitiesOfClass(
                    Monster.class, aabb,
                    e -> e.isAlive() && MirrorShooterEntity.this.hasLineOfSight(e));
            if (monsters.isEmpty()) return null;

            LivingEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Monster m : monsters) {
                double d = MirrorShooterEntity.this.distanceToSqr(m);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = m;
                }
            }
            return nearest;
        }

        private void performBowAttack(LivingEntity target) {
            if (target == null) return;
            ItemStack bow = MirrorShooterEntity.this.getMainHandItem();
            ItemStack arrowItem = MirrorShooterEntity.this.getProjectile(bow);
            if (arrowItem.isEmpty()) arrowItem = new ItemStack(Items.ARROW);

            AbstractArrow arrow = ProjectileUtil.getMobArrow(MirrorShooterEntity.this, arrowItem, 1.0F);

            if (getOwner() != null) {
                arrow.setOwner(getOwner());
            }

            if (bow.getItem() instanceof BowItem bowItem) {
                arrow = bowItem.customArrow(arrow);
            }

            // 使用 damage 字段设定箭矢基础伤害
            arrow.setBaseDamage(MirrorShooterEntity.this.getDamage());

            double dx = target.getX() - MirrorShooterEntity.this.getX();
            double dy = target.getY(0.3333) - arrow.getY();
            double dz = target.getZ() - MirrorShooterEntity.this.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + horiz * 0.2, dz, 1.6F, (float)(14 - MirrorShooterEntity.this.level().getDifficulty().getId() * 4));

            MirrorShooterEntity.this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (MirrorShooterEntity.this.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
            MirrorShooterEntity.this.level().addFreshEntity(arrow);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RemainingLife", this.getRemainingLife());
        ResourceLocation skin = this.getSkinTexture();
        if (skin != null) {
            tag.putString("SkinTexture", skin.toString());
        }
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RemainingLife")) {
            this.setRemainingLife(tag.getInt("RemainingLife"));
        }
        if (tag.contains("SkinTexture")) {
            this.setSkinTexture(new ResourceLocation(tag.getString("SkinTexture")));
        }
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
            this.entityData.set(OWNER_UUID, Optional.of(this.ownerUUID));
        }
    }
}