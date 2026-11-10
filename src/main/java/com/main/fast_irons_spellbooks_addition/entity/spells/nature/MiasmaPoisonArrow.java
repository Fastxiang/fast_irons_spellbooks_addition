package com.main.fast_irons_spellbooks_addition.entity.spells.nature;

import com.main.fast_irons_spellbooks_addition.event.MiasmaBurstEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 毒箭射击-瘴爆 的投射物。
 *
 * <p>复用 ISS {@link PoisonArrow} 的飞行、插地、音效，但把「落地生成毒云」换成一次性的瘴爆：</p>
 * <ul>
 *   <li><b>箭体不再打直伤</b>：{@link PoisonArrow} 的 {@code onHitEntity} 会先用 {@code getDamage()}
 *       结算一次、再引爆，同一目标吃两段；这里整个覆写掉，一支箭对一个目标只结算一次</li>
 *   <li>命中点 4 格内的目标逐个走 {@link MiasmaBurstEvent}：KubeJS 检查并消耗烈毒（{@code fast:toxic}）、
 *       把消耗掉的层数回填，Java 再按「基础自然伤害 + 每层增伤 × 层数」打出唯一一发</li>
 *   <li>伤害类型是本法术的自然系伤害（{@code SpellDamageSource}），吃自然系抗性与法术伤害事件</li>
 * </ul>
 */
public class MiasmaPoisonArrow extends PoisonArrow {

    /** 爆开半径 */
    private static final double BURST_RADIUS = 4.0;

    /** 法术等级（供事件使用） */
    private int spellLevel = 1;
    /** 引爆层数上限 */
    private int maxLayers = 25;

    public MiasmaPoisonArrow(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MiasmaPoisonArrow(Level level, LivingEntity shooter) {
        this(FastEntityRegistry.MIASMA_POISON_ARROW.get(), level);
        setOwner(shooter);
    }

    public void setSpellLevel(int spellLevel) {
        this.spellLevel = spellLevel;
    }

    public void setMaxLayers(int maxLayers) {
        this.maxLayers = maxLayers;
    }

    /**
     * 整个覆写：父类会先用 {@code getDamage()} 打一次箭体直伤，再调 {@link #createPoisonCloud(Vec3)}，
     * 于是同一个目标会吃「直伤 + 引爆」两次。这里只引爆，不再单独结算直伤。
     */
    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        if (this.level().isClientSide) {
            return;
        }
        if (this.hasEmittedPoison) {
            discard();
            return;
        }

        createPoisonCloud(entityHitResult.getEntity().position());
        this.consumeEntityImpact(entityHitResult, true);
    }

    @Override
    public void createPoisonCloud(Vec3 location) {
        if (this.level().isClientSide || this.hasEmittedPoison) {
            return;
        }
        this.hasEmittedPoison = true;

        // 必须是可变的 ArrayList：Rhino（KubeJS）反射调 size()/get() 时，
        // Stream#toList()/List.of() 返回的 ImmutableCollections$ListN 是包私有实现，会抛
        // IllegalAccessException 让整个监听静默失效。
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(location, location).inflate(BURST_RADIUS),
                        e -> e.isAlive()
                                && e.isPickable()
                                && !e.isSpectator()
                                && e != getOwner()
                                && canHitEntity(e)
                ).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(location)))
                .collect(Collectors.toCollection(ArrayList::new));

        if (getOwner() instanceof LivingEntity caster) {
            var damageSource = FastSpellRegistry.POISON_ARROW_MIASMA_BURST.get()
                    .getDamageSource(this, caster);

            for (LivingEntity target : targets) {
                // 事件里由 KubeJS 负责：检查烈毒 → 消耗 → setConsumedLayers(层数)
                MiasmaBurstEvent event = new MiasmaBurstEvent(
                        caster,
                        this.spellLevel,
                        location,
                        target,
                        this.getDamage(),
                        this.getAoeDamage(),
                        this.maxLayers
                );
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    continue;
                }

                float finalDamage = event.getDamage();
                if (finalDamage <= 0) {
                    continue;
                }

                // 和旧 KubeJS 的 simpleAttackEntity 保持一致：清零无敌帧，保证这一发一定落地
                int previousInvulnerableTime = target.invulnerableTime;
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, finalDamage, damageSource);
                target.invulnerableTime = previousInvulnerableTime;
            }
        }

        // 视觉：绿色酸雾爆开
        MagicManager.spawnParticles(this.level(), ParticleHelper.ACID, location.x, location.y + 0.5, location.z, 40, .3, .2, .3, .15, true);
        MagicManager.spawnParticles(this.level(), ParticleHelper.ACID, location.x, location.y + 0.5, location.z, 20, .6, .4, .6, .08, false);
        this.level().playSound(
                null,
                location.x, location.y, location.z,
                SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS,
                1.4f,
                0.7f + this.random.nextFloat() * 0.2f
        );
    }
}
