package com.main.fast_irons_spellbooks_addition.entity.spells.fireball;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 巨型火球-裂变 分裂出的小火球。
 *
 * <p>定位是「爆开后向外抛撒的流火」：<b>完全不做追踪</b>，被赋予一个朝外的初速，
 * 之后完全交给重力，飞出一条向外张开的抛物弧线，落向地面（撞到方块或实体就炸）。
 * 所以它和 {@code SmallMagicFireball} 的直线追踪弹道是两回事。</p>
 *
 * <p>与父类的其余差别：
 * <ul>
 *   <li><b>开重力</b>：父类构造里是 {@code setNoGravity(true)}，这里显式关掉，
 *       并覆写 {@link #getDefaultGravity()} 让弧线更舒展</li>
 *   <li>命中后打一个小范围（半径 2）的爆，而不是只打单体</li>
 *   <li><b>结算前把目标的无敌帧清零</b>，这样 4 颗小火球可以先后打中同一个怪，
 *       不会被 20 tick 的受击硬直吞掉伤害</li>
 *   <li>兜底寿命：万一抛到水里/空中飘太久，{@link #MAX_LIFE} tick 后原地炸掉</li>
 * </ul>
 */
public class FissionSmallFireball extends SmallMagicFireball {

    /** 小爆半径 */
    private static final float RADIUS = 2f;
    /** 出膛速度：不能太快，否则弧线被拉平成直线（0.5 大概落在 5~6 格外） */
    private static final float SPEED = 0.5f;
    /** 自定义重力（父类默认 0.05），略缓一点让下坠弧更明显 */
    private static final double GRAVITY = 0.045;
    /** 兜底寿命（tick） */
    private static final int MAX_LIFE = 80;

    public FissionSmallFireball(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        // 父类构造里写死了无重力，这里必须关掉，否则小火球只会直线飞出去
        this.setNoGravity(false);
    }

    public FissionSmallFireball(Level pLevel, LivingEntity pShooter) {
        this(FastEntityRegistry.FISSION_SMALL_FIREBALL.get(), pLevel);
        this.setOwner(pShooter);
    }

    @Override
    public float getSpeed() {
        return SPEED;
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.isRemoved() && this.tickCount > MAX_LIFE) {
            explode(this.position());
        }
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        if (this.level().isClientSide) {
            return;
        }
        explode(hitResult.getLocation());
    }

    /** 落点小范围爆炸：命中点越近伤害越高，并清掉无敌帧保证多颗都能结算 */
    private void explode(Vec3 impact) {
        if (this.isRemoved()) {
            return;
        }
        float radiusSqr = RADIUS * RADIUS;
        Vec3 losPoint = Utils.raycastForBlock(
                this.level(),
                this.position(),
                this.position().add(0, 1, 0),
                ClipContext.Fluid.NONE
        ).getLocation();

        var entities = this.level().getEntities(this, new AABB(impact, impact).inflate(RADIUS));
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            double distanceSqr = living.distanceToSqr(impact);
            if (distanceSqr >= radiusSqr || !canHitEntity(living)) {
                continue;
            }
            if (!Utils.hasLineOfSight(this.level(), losPoint, living.getBoundingBox().getCenter(), true)) {
                continue;
            }
            // 关键：无视无敌帧，保证 4 颗都能结算
            living.invulnerableTime = 0;
            double p = 1 - distanceSqr / radiusSqr;
            DamageSources.applyDamage(
                    living,
                    (float) (this.damage * p),
                    FastSpellRegistry.MAGIC_FIREBALL_FISSION.get().getDamageSource(this, getOwner())
            );
        }

        impactParticles(impact.x, impact.y, impact.z);
        playSound(SoundRegistry.FIRE_IMPACT.get(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        discard();
    }
}
