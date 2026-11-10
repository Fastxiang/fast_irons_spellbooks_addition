package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法箭-移动龙息 的投射物
 * 基于原版 MagicArrowProjectile：
 * - 移动速度极慢（0.1 格/tick，原版 2.7），因此整段飞行过程中持续喷吐龙息
 * - 龙息以箭的自身中心为起点，朝「附近最近的敌人」方向喷出锥形
 *   锥形尺寸对齐 ISS 法术龙息（DragonBreathProjectile 的 ConePart）：射程约 7 格，
 *   半径随距离从 0.5 线性扩张到约 1.5~2.0 格，命中范围基本一致
 * - 每 0.5 秒结算一段，伤害来源为施法者（玩家）本人
 * - 命中判定的伤害归属本分支法术，不再是原版 magic_arrow
 */
public class MagicArrowDragonBreathProjectile extends MagicArrowProjectile implements NoKnockbackProjectile {

    /** 飞行速度：极慢（原版 2.7 的一半再减半，0.1 格/tick） */
    private static final float SPEED = 0.1f;
    /** 存活时长（tick）= 10 秒（原 5 秒，翻倍） */
    private static final int LIFETIME = 20 * 10;
    /** 龙息结算间隔（tick）：每 0.5 秒一段 */
    private static final int BREATH_INTERVAL = 10;
    /** 龙息喷吐距离（方块）：对齐法术龙息锥形（最远 ConePart 5.5 + 半宽 1.5） */
    private static final float BREATH_RANGE = 7f;
    /** 龙息锥形根部半径：对齐法术龙息最内侧 ConePart（宽度 1.0） */
    private static final float BREATH_BASE_RADIUS = 0.5f;
    /** 龙息锥形每延伸一格扩张的半径：对齐法术龙息 0.5 → 1.5 的扩张斜率 */
    private static final float BREATH_RADIUS_PER_BLOCK = 0.22f;
    /** 索敌半径：比龙息射程略大，保证射程内的敌人都能被瞄准 */
    private static final float TARGET_SEARCH_RADIUS = BREATH_RANGE + 1.5f;
    /** 龙息音效间隔（tick） */
    private static final int BREATH_SOUND_INTERVAL = 20;

    /** 龙息每段伤害，由施法时的法术等级/魔攻决定 */
    private float breathDamage;

    public MagicArrowDragonBreathProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MagicArrowDragonBreathProjectile(Level level, LivingEntity shooter) {
        this(FastEntityRegistry.MAGIC_ARROW_DRAGON_BREATH.get(), level);
        this.setOwner(shooter);
    }

    public void setBreathDamage(float breathDamage) {
        this.breathDamage = breathDamage;
    }

    public float getBreathDamage() {
        return breathDamage;
    }

    @Override
    public float getSpeed() {
        return SPEED;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isRemoved()) {
            return;
        }

        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount % BREATH_INTERVAL == 0) {
            LivingEntity target = dealDragonBreathDamage();
            // 只有真的喷出去了才播音效，避免空放声音
            if (target != null && this.tickCount % BREATH_SOUND_INTERVAL == 0) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundRegistry.FIRE_BREATH_LOOP.get(),
                        SoundSource.NEUTRAL,
                        0.6f,
                        0.9f + Utils.random.nextFloat() * 0.2f
                );
            }
        }
    }

    /**
     * 从箭的中心点朝附近最近的敌人方向喷出一段龙息，结算锥形范围内的伤害。
     * 伤害来源为施法者本人，命中不产生击退。
     *
     * @return 本次瞄准的敌人；附近没有敌人时返回 null（不喷吐）
     */
    @Nullable
    private LivingEntity dealDragonBreathDamage() {
        if (!(this.getOwner() instanceof LivingEntity caster)) {
            return null;
        }
        LivingEntity target = findNearestTarget(caster, TARGET_SEARCH_RADIUS, true);
        if (target == null || this.breathDamage <= 0) {
            return target;
        }

        Vec3 origin = this.position();
        Vec3 towardsTarget = target.getBoundingBox().getCenter().subtract(origin);
        if (towardsTarget.lengthSqr() < 1e-6) {
            return target;
        }
        Vec3 direction = towardsTarget.normalize();

        SpellDamageSource damageSource = FastSpellRegistry.MAGIC_ARROW_MOVING_DRAGON_BREATH.get()
                .getDamageSource(this, caster);

        double maxRadius = BREATH_BASE_RADIUS + BREATH_RADIUS_PER_BLOCK * BREATH_RANGE;
        AABB area = new AABB(origin, origin).inflate(BREATH_RANGE + maxRadius);

        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim == caster || !victim.isAlive() || victim.isSpectator()) {
                continue;
            }
            if (!isInsideBreathCone(origin, direction, victim)) {
                continue;
            }
            if (DamageSources.isFriendlyFireBetween(caster, victim)) {
                continue;
            }
            if (!Utils.hasLineOfSight(this.level(), this, victim, true)) {
                continue;
            }
            DamageSources.applyDamage(victim, this.breathDamage, damageSource);
        }

        return target;
    }

    /**
     * 判断目标是否落在以 origin 为顶点、朝 direction 的锥形内。
     * 半径随距离线性扩张，与法术龙息的多段 ConePart 包围盒包络一致。
     */
    private static boolean isInsideBreathCone(Vec3 origin, Vec3 direction, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
        double along = toTarget.dot(direction);
        // 起点允许略微负偏移，贴脸的敌人也能吃到
        if (along < -0.5 || along > BREATH_RANGE) {
            return false;
        }
        double radial = toTarget.subtract(direction.scale(along)).length();
        double allowedRadius = BREATH_BASE_RADIUS + BREATH_RADIUS_PER_BLOCK * Math.max(along, 0);
        return radial <= allowedRadius;
    }

    /**
     * 在半径内寻找离箭最近的可攻击目标（排除施法者、友军、旁观者）
     */
    @Nullable
    private LivingEntity findNearestTarget(LivingEntity caster, double radius, boolean requireLineOfSight) {
        AABB area = this.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        double radiusSqr = radius * radius;

        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (candidate == caster || !candidate.isAlive() || candidate.isSpectator()) {
                continue;
            }
            if (DamageSources.isFriendlyFireBetween(caster, candidate)) {
                continue;
            }
            double distance = candidate.distanceToSqr(this);
            if (distance > radiusSqr || distance >= bestDistance) {
                continue;
            }
            if (requireLineOfSight && !Utils.hasLineOfSight(this.level(), this, candidate, true)) {
                continue;
            }
            best = candidate;
            bestDistance = distance;
        }

        return best;
    }

    /**
     * 客户端拖尾：保留原版魔法箭的末影粒子，并朝当前瞄准的敌人方向绘制龙息锥形粒子。
     * 与服务器端使用同一套索敌逻辑，视觉上看到的锥形就是实际结算的锥形。
     */
    @Override
    public void trailParticles() {
        super.trailParticles();

        if (!(this.getOwner() instanceof LivingEntity caster)) {
            return;
        }
        LivingEntity target = findNearestTarget(caster, TARGET_SEARCH_RADIUS, false);
        if (target == null) {
            return;
        }

        Vec3 origin = this.position();
        Vec3 towardsTarget = target.getBoundingBox().getCenter().subtract(origin);
        if (towardsTarget.lengthSqr() < 1e-6) {
            return;
        }
        Vec3 direction = towardsTarget.normalize();

        // 构建与喷吐方向正交的两个轴，用于在锥形截面上撒粒子
        Vec3 reference = Math.abs(direction.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();

        for (int i = 0; i < 12; i++) {
            double along = 0.4 + this.random.nextDouble() * BREATH_RANGE;
            double allowedRadius = BREATH_BASE_RADIUS + BREATH_RADIUS_PER_BLOCK * along;
            double radius = allowedRadius * this.random.nextDouble();
            double angle = this.random.nextDouble() * Math.PI * 2;
            Vec3 offset = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            Vec3 spawn = origin.add(direction.scale(along)).add(offset);
            double speed = this.random.nextDouble() * 0.25 + 0.15;
            this.level().addParticle(
                    ParticleTypes.DRAGON_BREATH,
                    spawn.x, spawn.y, spawn.z,
                    direction.x * speed, direction.y * speed, direction.z * speed
            );
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (owner != null && !victims.contains(entity.getUUID())) {
            DamageSources.applyDamage(
                    entity,
                    this.damage,
                    FastSpellRegistry.MAGIC_ARROW_MOVING_DRAGON_BREATH.get().getDamageSource(this, owner)
            );
            victims.add(entity.getUUID());
        }

        consumeEntityImpact(entityHitResult, true);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BreathDamage", this.breathDamage);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.breathDamage = tag.getFloat("BreathDamage");
    }
}
