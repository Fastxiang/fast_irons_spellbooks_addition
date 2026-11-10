package com.main.fast_irons_spellbooks_addition.entity.spells.fireball;

import com.main.fast_irons_spellbooks_addition.event.MagicFireballFissionEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

/**
 * 巨型火球-裂变 的主爆实体。
 *
 * <p>复用 {@link MagicFireball} 的飞行/粒子/音效，但把命中结算换成自己的：
 * <ul>
 *   <li>主爆使用本分支法术的伤害来源（而不是原版 fireball），便于 KubeJS 区分</li>
 *   <li><b>本体不追踪</b>，就是普通火球直线飞出去炸开</li>
 *   <li>主爆结束后向四周甩出小火球（数量由法术等级决定，1 级 4 颗、每级 +1）：
 *       它们被朝外上方抛出，靠重力飞出一条向外张开的抛物弧线，最终落向地面炸开，
 *       <b>不追踪任何目标</b></li>
 *   <li>爆点火焰粒子只播 0.3 倍半径，避免一大团火把小火球糊住看不见</li>
 * </ul>
 */
public class FissionMagicFireball extends MagicFireball {

    /** 分裂小火球默认数量（法术每次施放都会用等级算出的数量覆盖它） */
    private static final int DEFAULT_SMALL_COUNT = 4;
    /** 爆点火焰粒子的半径系数：伤害半径 4 → 视觉半径 1.2，别盖住甩出去的小火球 */
    private static final float EXPLOSION_PARTICLE_RADIUS_SCALE = 0.3f;

    private float smallDamage;
    private int smallCount = DEFAULT_SMALL_COUNT;
    private int spellLevel = 1;

    public FissionMagicFireball(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public FissionMagicFireball(Level pLevel, LivingEntity pShooter) {
        this(FastEntityRegistry.FISSION_MAGIC_FIREBALL.get(), pLevel);
        this.setOwner(pShooter);
    }

    public void setSmallDamage(float smallDamage) {
        this.smallDamage = smallDamage;
    }

    public float getSmallDamage() {
        return smallDamage;
    }

    public void setSmallCount(int smallCount) {
        this.smallCount = smallCount;
    }

    public int getSmallCount() {
        return smallCount;
    }

    public void setSpellLevel(int spellLevel) {
        this.spellLevel = spellLevel;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        if (this.level().isClientSide) {
            return;
        }

        Vec3 impact = hitResult.getLocation();
        float explosionRadius = getExplosionRadius();
        float explosionRadiusSqr = explosionRadius * explosionRadius;

        // ===== 主爆 =====
        var entities = this.level().getEntities(this, this.getBoundingBox().inflate(explosionRadius));
        Vec3 losPoint = Utils.raycastForBlock(
                this.level(),
                this.position(),
                this.position().add(0, 2, 0),
                ClipContext.Fluid.NONE
        ).getLocation();
        for (Entity entity : entities) {
            double distanceSqr = entity.distanceToSqr(impact);
            if (distanceSqr < explosionRadiusSqr
                    && canHitEntity(entity)
                    && Utils.hasLineOfSight(this.level(), losPoint, entity.getBoundingBox().getCenter(), true)) {
                double p = 1 - distanceSqr / explosionRadiusSqr;
                DamageSources.applyDamage(
                        entity,
                        (float) (this.damage * p),
                        FastSpellRegistry.MAGIC_FIREBALL_FISSION.get().getDamageSource(this, getOwner())
                );
            }
        }

        // ===== 专属事件节点 =====
        if (getOwner() instanceof LivingEntity owner) {
            MinecraftForge.EVENT_BUS.post(new MagicFireballFissionEvent(
                    owner,
                    this.spellLevel,
                    impact,
                    this.damage,
                    this.smallDamage,
                    this.smallCount
            ));
        }

        // ===== 分裂 =====
        spawnSmallFireballs(impact);

        // 爆点火焰粒子刻意压到很小：按半径 4 播会形成一大团火球把甩出去的小火球全糊住，
        // 这里只留一个 0.3 倍大小的核心火团当爆点提示，保证 4 颗小火球的弧线看得见
        PacketDistributor.sendToPlayersTrackingEntity(this, new FieryExplosionParticlesPacket(
                impact.subtract(getDeltaMovement().scale(0.5)),
                explosionRadius * EXPLOSION_PARTICLE_RADIUS_SCALE
        ));
        playSound(SoundEvents.GENERIC_EXPLODE, 4.0F, (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
        discardHelper(hitResult);
    }

    private void spawnSmallFireballs(Vec3 impact) {
        if (!(getOwner() instanceof LivingEntity owner) || this.smallDamage <= 0) {
            return;
        }

        for (int i = 0; i < this.smallCount; i++) {
            FissionSmallFireball small = new FissionSmallFireball(this.level(), owner);
            small.setDamage(this.smallDamage);

            // 4 颗按方位均匀散开（带一点随机抖动），从爆心略偏外、略偏上的位置起飞
            double angle = (Math.PI * 2 / Math.max(1, this.smallCount)) * i
                    + Math.toRadians((this.random.nextDouble() - 0.5) * 40.0);
            Vec3 outward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 spawn = impact.add(outward.scale(0.45)).add(0, 0.45, 0);
            small.setPos(spawn);

            // 朝外 + 朝上抛出，之后完全交给重力：
            // 形成向外张开、最终压向地面的抛物弧线（不追踪任何目标）
            Vec3 direction = outward
                    .add(0, 0.55 + this.random.nextDouble() * 0.2, 0)
                    .normalize();
            // AbstractMagicProjectile#shoot 内部会再乘一次 getSpeed()
            small.shoot(direction);
            this.level().addFreshEntity(small);
        }
    }
}
