package com.main.fast_irons_spellbooks_addition.spells.ice;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.util.RayOfFrostPursuitTracker;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
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
 * 冰霜射线-追击（冰霜射线的分支变体）
 *
 * <p>这一支<b>施法那一下不打伤害</b>：1.5 秒吟唱只换来 30 秒的「冰霜追击」窗口。
 * 窗口内你造成任意伤害时，{@link RayOfFrostPursuitTracker} 会在你身边随机一处
 * 补一条冰霜射线追打同一目标，<b>伤害按冰霜射线本体结算</b>（{@link #getRayDamage}）、
 * 命中回复 50 点法力，内部冷却 1 秒。</p>
 *
 * <p>和第一分支「协奏」的区别：协奏是<b>施法那一下</b>额外打 2 发飞弹，
 * 只吃施法瞬间的收益；追击把收益摊到 30 秒里，任何伤害来源（近战 / 弓 / 枪 / 法术）
 * 都能触发，定位是「冰系的持续回蓝引擎」。</p>
 */
public class RayOfFrostPursuitSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("ray_of_frost_pursuit");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60)
            .build();

    public RayOfFrostPursuitSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;          // 1.5 秒吟唱（30 tick）
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.effect_duration", Utils.timeFromTicks(RayOfFrostPursuitTracker.DEFAULT_DURATION, 0)),
                Component.translatable("ui.fast_irons_spellbooks_addition.ray_of_frost_pursuit_mana",
                        (int) RayOfFrostPursuitTracker.MANA_REFUND)
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
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.RAY_OF_FROST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        // 这一支施法那一下不打伤害，只开窗：30 秒内造成任何伤害都会追加一条追击射线
        if (!level.isClientSide) {
            RayOfFrostPursuitTracker.apply(entity, RayOfFrostPursuitTracker.DEFAULT_DURATION, spellLevel);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getRayDamage(this, spellLevel, caster);
    }

    /**
     * 冰霜射线本体（以及追击补的那一条）共用的伤害公式。
     * <p>追击触发时按 buff 里记下的施法等级、用施法者<b>当前</b>的法术强度 / 冰霜强度重算，
     * 与直接读原版伤害的区别只是「窗口期间加成变了会跟着变」。</p>
     */
    public static float getRayDamage(AbstractSpell spell, int spellLevel, LivingEntity caster) {
        return 5f * spell.getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f));
    }
}
