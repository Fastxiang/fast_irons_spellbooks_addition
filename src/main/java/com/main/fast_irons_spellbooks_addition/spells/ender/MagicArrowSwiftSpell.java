package com.main.fast_irons_spellbooks_addition.spells.ender;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
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
 * 魔法箭-迅捷
 * 基于原版 MagicArrowSpell 的分支变体：
 * - 吟唱时间：0 秒（原为 1.5 秒）
 * - 冷却时间：0 秒（原为 5 秒）
 * - 法力消耗：400（原为 200，两倍）
 * - 无蓄力/释放音效
 */
public class MagicArrowSwiftSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("magic_arrow_swift");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(0)
            .build();

    public MagicArrowSwiftSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;            // 0 秒吟唱（瞬发）
        this.baseManaCost = 400;      // 原版 200 的两倍
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
        return Optional.empty();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        MagicArrowProjectile magicArrow = new MagicArrowProjectile(level, entity);
        magicArrow.setPos(entity.position().add(0, entity.getEyeHeight() - magicArrow.getBoundingBox().getYsize() * .5f, 0).add(entity.getForward()));
        magicArrow.shoot(entity.getLookAngle());
        magicArrow.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(magicArrow);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 5f * getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f));
    }
}
