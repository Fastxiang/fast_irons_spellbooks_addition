package com.main.fast_irons_spellbooks_addition.spells.fire;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.fireball.FissionMagicFireball;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

/**
 * 巨型火球-裂变（第二分支 · 火）
 *
 * <p>第一分支「追踪」是单点必中的一次爆发；这一条把追踪完全去掉：
 * 本体就是一发<b>普通直线飞行</b>的巨型火球，炸开后向四周甩出 4 颗小火球，
 * 小火球被朝外上方抛出、靠重力飞成抛物弧线，最终落向地面炸开（不追踪任何目标）。</p>
 *
 * <p><b>不锁定</b>：不调用 {@code Utils.preCastTargetHelper}，所以既不会发
 * {@code SyncTargetingDataPacket}（客户端不会画出锁定准星），也不会弹「锁定成功」的动作栏提示，
 * 纯按视线方向直接打出去。</p>
 *
 * <p>数值按 10 级对齐第一分支（9.5M）：主爆 6M；单颗小火球 = <b>主爆伤害的 70%</b>（10 级约 4.25M / 颗），
 * 数量随等级成长（1 级 4 颗、每升 1 级多 1 颗，10 级 13 颗）。理论满命中极高，
 * 但小火球是抛物弧散落、不受控，实战能吃到的颗数远低于上限。</p>
 */
public class MagicFireballFissionSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("magic_fireball_fission");

    /** 主爆系数：10 级 ≈ 600% 魔法攻击 */
    private static final float MAIN_COEFFICIENT = 3.2f;
    /** 单颗小火球 = 主爆伤害的 70%：10 级 ≈ 425% 魔法攻击 / 颗 */
    private static final float SMALL_COEFFICIENT = MAIN_COEFFICIENT * 0.7f;
    /** 1 级时的分裂数量，之后每升 1 级多 1 颗 */
    private static final int SMALL_COUNT_BASE = 4;
    /** 主爆半径 */
    private static final int MAIN_RADIUS = 4;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(6)
            .build();

    public MagicFireballFissionSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getMainDamage(spellLevel, caster), 2)),
                Component.translatable("ui.fast_irons_spellbooks_addition.fission_small_damage", Utils.stringTruncation(getSmallDamage(spellLevel, caster), 2)),
                Component.translatable("ui.fast_irons_spellbooks_addition.fission_small_count", getSmallCount(spellLevel)),
                Component.translatable("ui.irons_spellbooks.radius", MAIN_RADIUS)
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        Vec3 origin = entity.getEyePosition();

        FissionMagicFireball fireball = new FissionMagicFireball(world, entity);
        fireball.setDamage(getMainDamage(spellLevel, entity));
        fireball.setSmallDamage(getSmallDamage(spellLevel, entity));
        fireball.setSmallCount(getSmallCount(spellLevel));
        fireball.setExplosionRadius(MAIN_RADIUS);
        fireball.setSpellLevel(spellLevel);
        fireball.setPos(origin.add(entity.getForward()).subtract(0, fireball.getBbHeight() / 2, 0));
        // 本体就是普通火球：直线飞行，没有任何追踪
        fireball.shoot(entity.getLookAngle());

        world.addFreshEntity(fireball);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /** 主爆满伤 */
    public float getMainDamage(int spellLevel, LivingEntity caster) {
        return MAIN_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    /** 单颗小火球满伤（主爆伤害的 70%） */
    public float getSmallDamage(int spellLevel, LivingEntity caster) {
        return SMALL_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    /** 分裂数量：1 级 4 颗，每升 1 级多 1 颗 */
    public int getSmallCount(int spellLevel) {
        return SMALL_COUNT_BASE + (spellLevel - 1);
    }
}
