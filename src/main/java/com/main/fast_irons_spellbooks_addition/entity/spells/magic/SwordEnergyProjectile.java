package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.main.fast_irons_spellbooks_addition.event.BasicAttackHitEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 剑气：复刻原版铁魔法 Blood Slash（猩红斩击）的投射物
 * - 去除了全部粒子效果（原版为血色粒子）
 * - 渲染改为青白色（见 SwordEnergyRenderer）
 * - 伤害算作物理，并接入 BasicAttackHitEvent / BasicAttackMissEvent
 * - 支持排除近战已命中的目标，避免剑气和近战对同一目标重复生效
 */
public class SwordEnergyProjectile extends AbstractMagicProjectile {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(SwordEnergyProjectile.class, EntityDataSerializers.FLOAT);
    private static final float SPEED = 1f;
    public final int animationSeed;
    private final float maxRadius;
    public AABB oldBB;
    public int animationTime;
    private final List<Entity> victims;
    /** 近战已命中的目标，剑气不再对它们生效（剑气和近战只生效一个命中） */
    private final Set<UUID> excludedTargets;

    private int combo;
    private DamageSource damageSource;

    public SwordEnergyProjectile(EntityType<? extends SwordEnergyProjectile> entityType, Level level) {
        super(entityType, level);
        animationSeed = Utils.random.nextInt(9999);

        setRadius(.6f);
        maxRadius = 3;
        oldBB = getBoundingBox();
        victims = new ArrayList<>();
        excludedTargets = new HashSet<>();
        this.setNoGravity(true);
    }

    public SwordEnergyProjectile(EntityType<? extends SwordEnergyProjectile> entityType, Level levelIn, LivingEntity shooter) {
        this(entityType, levelIn);
        setOwner(shooter);
        setYRot(shooter.getYRot());
        setXRot(shooter.getXRot());
    }

    public SwordEnergyProjectile(Level levelIn, LivingEntity shooter) {
        this(FastEntityRegistry.SWORD_ENERGY_PROJECTILE.get(), levelIn, shooter);
    }

    public void setCombo(int combo) {
        this.combo = combo;
    }

    public int getCombo() {
        return combo;
    }

    public void setDamageSource(DamageSource damageSource) {
        this.damageSource = damageSource;
    }

    public DamageSource getStoredDamageSource() {
        if (this.damageSource != null) {
            return this.damageSource;
        }
        Entity owner = getOwner();
        if (owner instanceof Player player) {
            return player.damageSources().playerAttack(player);
        } else if (owner instanceof LivingEntity living) {
            return living.damageSources().mobAttack(living);
        }
        return this.damageSources().generic();
    }

    /** 排除近战已经命中的目标，避免同一目标被近战和剑气重复命中 */
    public void excludeTargets(Collection<? extends Entity> targets) {
        for (Entity target : targets) {
            if (target != null) {
                this.excludedTargets.add(target.getUUID());
            }
        }
    }

    @Override
    public void trailParticles() {
        // 删除粒子效果
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        // 删除粒子效果
    }

    @Override
    public float getSpeed() {
        return SPEED;
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RADIUS, 0.5F);
    }

    @Override
    public void handleHitDetection() {
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() == HitResult.Type.BLOCK) {
            onHitBlock((BlockHitResult) hitresult);
        }
        if (!this.level().isClientSide) {
            var entities = this.level().getEntities(this, this.getBoundingBox()).stream().filter(target -> canHitEntity(target)
                    && !victims.contains(target)
                    && !excludedTargets.contains(target.getUUID())).collect(Collectors.toSet());
            if (!entities.isEmpty()) {
                Entity last = null;
                for (Entity entity : entities) {
                    damageEntity(entity);
                    last = entity;
                    if (entity instanceof ShieldPart || entity instanceof AbstractShieldEntity) {
                        discard();
                        return;
                    }
                }
                tryRedirectFromEntityRicochet(new EntityHitResult(last));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        oldBB = getBoundingBox();
        setRadius(getRadius() + 0.12f);
    }

    public void setRadius(float newRadius) {
        if (newRadius <= maxRadius && !this.level().isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(newRadius, 0.0F, maxRadius));
        }
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public EntityDimensions getDimensions(Pose p_19721_) {
        this.getBoundingBox();
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> p_19729_) {
        if (DATA_RADIUS.equals(p_19729_)) {
            this.refreshDimensions();
        }

        super.onSyncedDataUpdated(p_19729_);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        // 剑气不发布普攻未命中事件（命中/未命中只由近战逻辑统一处理）
        discard();
    }

    private void damageEntity(Entity entity) {
        if (!(entity instanceof LivingEntity livingTarget) || victims.contains(entity)) {
            return;
        }
        victims.add(entity);

        BasicAttackHitEvent attackHitEvent = new BasicAttackHitEvent(
                getOwner(),
                List.of(livingTarget),
                getDamage(),
                getCombo(),
                null,
                "physical",
                getStoredDamageSource()
        );
        MinecraftForge.EVENT_BUS.post(attackHitEvent);

        livingTarget.invulnerableTime = 0;
        DamageSources.applyDamage(livingTarget, attackHitEvent.getDamage(), attackHitEvent.getDamageSource());
        livingTarget.invulnerableTime = 20;
    }
}
