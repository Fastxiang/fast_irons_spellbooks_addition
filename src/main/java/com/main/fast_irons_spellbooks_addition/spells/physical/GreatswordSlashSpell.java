package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackHitEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackMissEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackPreEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.common.ForgeMod.ENTITY_REACH;

/**
 * 【大剑横扫】大剑武装状态下左键打出的那一刀。
 *
 * <h2>伤害链路和物理三连击（{@link TripleStrikeSpell}）完全一致</h2>
 * <ul>
 *   <li>基数 {@code FastAttributeUtil.getPhysicalAttack(caster) * getBasicPhysicalDamage(caster)}</li>
 *   <li>先发 {@link BasicAttackPreEvent}，允许 KubeJS / 饰品改伤害与倍率，被取消就不结算</li>
 *   <li>再发 {@link BasicAttackHitEvent}，KubeJS 侧所有"物理普攻命中"类饰品的钩子挂在这上面</li>
 *   <li>未命中发 {@link BasicAttackMissEvent}</li>
 *   <li>命中时 {@code invulnerableTime = 0 → hurt → = 20}，保证一刀只吃一次伤害且不被无敌帧吞</li>
 *   <li>判定是水平扇形：半径取 {@code ENTITY_REACH} 属性，半角 60°，贴脸 0.5 格内必中</li>
 * </ul>
 *
 * <p>唯一的区别是没有连段：倍率固定 1.0，不写 recast，也不往 comboMap 里记东西。
 * 连段是"物理三连击"自己的玩法，大剑普攻就是一刀。</p>
 */
public class GreatswordSlashSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("greatsword_slash");

    /** 扇形半角（度） */
    private static final double HALF_ANGLE_DEGREE = 60.0D;
    /** 伤害倍率。物理三连击是 0.8/0.8/1.4，单刀取中段手感 */
    private static final double DAMAGE_MULTIPLIER = 1.0D;
    /** 判定半径兜底（ENTITY_REACH 拿不到时用） */
    private static final double FALLBACK_RANGE = 3.0D;
    /** 命中后给目标施加的无敌帧，避免同一刀被多段判定重复结算 */
    private static final int HIT_INVULNERABLE_TICKS = 20;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(5)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public GreatswordSlashSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 50;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    /**
     * 挥砍动画：{@code greatsword_slash}，物理三连击最后一击（attack4）的 2.5 秒版。
     * 同一份 json、同一种 Bedrock 1.8.0 结构，由 ISS 的 {@code AnimationHelper} 播。
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return new AnimationHolder(FastIronsSpellbooksAddition.id("greatsword_slash"), true);
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage",
                Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
    }

    public float getDamage(int spellLevel, LivingEntity entity) {
        if (entity == null) return 0;
        return FastAttributeUtil.getPhysicalAttack(entity) * FastAttributeUtil.getBasicPhysicalDamage(entity);
    }

    private DamageSource getDamageSource(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        return entity.damageSources().mobAttack(entity);
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        if (caster == null || world.isClientSide) {
            super.onCast(world, spellLevel, caster, castSource, magicData);
            return;
        }

        BasicAttackPreEvent preEvent = new BasicAttackPreEvent(
                caster,
                1,
                getDamage(spellLevel, caster),
                DAMAGE_MULTIPLIER,
                "physical"
        );

        if (!MinecraftForge.EVENT_BUS.post(preEvent)) {
            double finalDamage = preEvent.getDamage() * preEvent.getDamageMultiplier();
            DamageSource source = getDamageSource(caster);

            double range = FastAttributeUtil.getAttackRange(caster);

            List<LivingEntity> hitTargets = collectTargets(world, caster, range);

            if (!hitTargets.isEmpty()) {
                BasicAttackHitEvent hitEvent = new BasicAttackHitEvent(
                        caster,
                        hitTargets,
                        (float) finalDamage,
                        1,
                        null,
                        "physical",
                        source
                );
                MinecraftForge.EVENT_BUS.post(hitEvent);

                for (LivingEntity target : hitTargets) {
                    target.invulnerableTime = 0;
                    target.hurt(hitEvent.getDamageSource(), hitEvent.getDamage());
                    target.invulnerableTime = HIT_INVULNERABLE_TICKS;
                }
            } else {
                MinecraftForge.EVENT_BUS.post(new BasicAttackMissEvent(caster, 1, null, "physical"));
            }
        }

        caster.swing(InteractionHand.MAIN_HAND, true);
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    /**
     * 水平扇形判定，逻辑与 {@link TripleStrikeSpell} 一致：
     * AABB 只做粗筛，真正的边界是「水平距离 ≤ range」且「水平夹角 ≤ 60°」。
     */
    private List<LivingEntity> collectTargets(Level world, LivingEntity caster, double range) {
        Vec3 look = caster.getLookAngle();
        Vec3 origin = caster.position().add(0, caster.getEyeHeight(), 0);

        double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double lookX = lookHorizontal > 1.0E-6D ? look.x / lookHorizontal : look.x;
        double lookZ = lookHorizontal > 1.0E-6D ? look.z / lookHorizontal : look.z;

        double cosHalfAngle = Math.cos(Math.toRadians(HALF_ANGLE_DEGREE));
        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity target : world.getEntitiesOfClass(
                LivingEntity.class,
                caster.getBoundingBox().inflate(range + 1.0D),
                e -> e != caster && e.isAlive() && caster.canAttack(e)
        )) {
            Vec3 targetPos = target.position().add(0, target.getEyeHeight() / 2, 0);
            double dx = targetPos.x - origin.x;
            double dz = targetPos.z - origin.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            if (horizontalDistance > range) continue;

            if (horizontalDistance < 0.5) {
                result.add(target);
                continue;
            }

            double dot = lookX * (dx / horizontalDistance) + lookZ * (dz / horizontalDistance);
            if (dot >= cosHalfAngle) {
                result.add(target);
            }
        }
        return result;
    }
}
