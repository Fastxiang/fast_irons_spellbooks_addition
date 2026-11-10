package com.main.fast_irons_spellbooks_addition.spells.ender;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.MagicArrowDragonBreathProjectile;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
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
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

/**
 * 魔法箭-移动龙息
 * 基于原版 MagicArrowSpell 的分支变体：
 * - 投射物移动速度极慢（0.1 格/tick，原版 2.7），存活 10 秒，飞行期间持续喷吐龙息
 * - 龙息从箭的中心点朝附近最近的敌人方向喷出锥形，范围与法术龙息一致
 * - 龙息伤害来源为施法者本人，每段 200% 魔法攻击
 * - 冷却时间：5 秒
 * - 法力消耗：1000
 * - 龙息本身没有独立冷却与蓝耗，效果全部由 {@link MagicArrowDragonBreathProjectile} 实现
 */
public class MagicArrowMovingDragonBreathSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("magic_arrow_moving_dragon_breath");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public MagicArrowMovingDragonBreathSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;            // 瞬发
        this.baseManaCost = 1000;
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
        return Optional.of(SoundRegistry.MAGIC_ARROW_RELEASE.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.fast_irons_spellbooks_addition.dragon_breath_damage", Utils.stringTruncation(getBreathDamage(spellLevel, caster), 1))
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        MagicArrowDragonBreathProjectile magicArrow = new MagicArrowDragonBreathProjectile(level, entity);
        magicArrow.setPos(entity.position().add(0, entity.getEyeHeight() - magicArrow.getBoundingBox().getYsize() * .5f, 0).add(entity.getForward()));
        magicArrow.shoot(entity.getLookAngle());
        magicArrow.setDamage(getDamage(spellLevel, entity));
        magicArrow.setBreathDamage(getBreathDamage(spellLevel, entity));
        level.addFreshEntity(magicArrow);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 魔法箭本体的命中伤害，与原版魔法箭一致 */
    public float getDamage(int spellLevel, LivingEntity caster) {
        return 5f * getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f));
    }

    /** 龙息每段伤害：200% 魔法攻击 */
    public float getBreathDamage(int spellLevel, LivingEntity caster) {
        return 2f * FastAttributeUtil.getMagicAttack(caster) * (1f + ((spellLevel - 1) * 0.1f));
    }
}
