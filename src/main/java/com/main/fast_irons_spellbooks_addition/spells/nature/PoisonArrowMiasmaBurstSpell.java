package com.main.fast_irons_spellbooks_addition.spells.nature;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.nature.MiasmaPoisonArrow;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

/**
 * 毒箭射击-瘴爆（第二分支 · 自然 · A 方案）
 *
 * <p>第一分支「毒性激发」是往地上铺毒雾、把瘴毒增伤拉满；
 * 这一条<b>不铺雾</b>：箭命中后只结算<b>一次自然伤害</b>，伤害 = 基础伤害 + 每消耗一层烈毒
 * 追加的增伤。命中点 4 格内的目标一起吃，每个目标只吃一发（不再是「箭体直伤 + 引爆」两段）。</p>
 *
 * <p>分工：Java 负责自然伤害的数值与结算，KubeJS 负责在 {@code MiasmaBurstEvent} 里
 * 检查并消耗烈毒（{@code fast:toxic}），把消耗掉的层数回填给 Java。</p>
 */
public class PoisonArrowMiasmaBurstSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("poison_arrow_miasma_burst");

    /** 基础自然伤害系数：10 级 ≈ 190% 魔法攻击 */
    private static final float ARROW_COEFFICIENT = 1f;
    /** 每消耗一层烈毒追加的伤害系数：10 级 ≈ 80% 魔法攻击 / 层 */
    private static final float PER_LAYER_COEFFICIENT = 0.42f;
    /** 引爆层数上限 */
    private static final int MAX_LAYERS = 25;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public PoisonArrowMiasmaBurstSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.fast_irons_spellbooks_addition.miasma_base_damage", Utils.stringTruncation(getArrowDamage(spellLevel, caster), 1)),
                Component.translatable("ui.fast_irons_spellbooks_addition.miasma_per_layer", Utils.stringTruncation(getDamagePerLayer(spellLevel, caster), 2)),
                Component.translatable("ui.fast_irons_spellbooks_addition.miasma_max_layers", MAX_LAYERS)
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
        return Optional.of(SoundRegistry.POISON_ARROW_CHARGE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.POISON_ARROW_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        MiasmaPoisonArrow arrow = new MiasmaPoisonArrow(level, entity);
        arrow.setPos(entity.position()
                .add(0, entity.getEyeHeight() - arrow.getBoundingBox().getYsize() * .5f, 0)
                .add(entity.getForward()));
        arrow.shoot(entity.getLookAngle());
        arrow.setDamage(getArrowDamage(spellLevel, entity));
        arrow.setAoeDamage(getDamagePerLayer(spellLevel, entity));
        arrow.setSpellLevel(spellLevel);
        arrow.setMaxLayers(MAX_LAYERS);
        level.addFreshEntity(arrow);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 不含层数的基础自然伤害 */
    private float getArrowDamage(int spellLevel, LivingEntity caster) {
        return ARROW_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    /** 每消耗一层烈毒追加的伤害 */
    private float getDamagePerLayer(int spellLevel, LivingEntity caster) {
        return PER_LAYER_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }
}
