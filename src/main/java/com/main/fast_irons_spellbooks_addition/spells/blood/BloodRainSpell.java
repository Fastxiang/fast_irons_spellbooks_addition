package com.main.fast_irons_spellbooks_addition.spells.blood;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.BloodRainEntity;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 猩红刺狱-血雨
 * 基于原版 AcupunctureSpell 的分支变体：
 * - 调用原版 BloodNeedle 投射物（而非箭）
 * - 范围 20 格，持续 60 秒
 * - 1 秒吟唱时间，180 秒冷却
 * - 伤害为 50% 魔法攻击
 */
public class BloodRainSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("blood_rain");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(180)
            .build();

    public BloodRainSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 20;          // 1 秒吟唱（20 tick）
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", 20),
                Component.translatable("ui.irons_spellbooks.effect_duration", Utils.timeFromTicks(20 * 60, 1))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return true;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .15f, false);
        return true;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        Vec3 targetLocation = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
            targetLocation = castTargetingData.getTargetPosition((ServerLevel) world);
        }
        if (targetLocation == null) {
            targetLocation = Utils.raycastForEntity(world, entity, 100, true).getLocation();
        }

        BloodRainEntity rain = new BloodRainEntity(FastEntityRegistry.BLOOD_RAIN.get(), world);
        rain.setDamage(getDamage(spellLevel, entity));
        rain.setPos(targetLocation.x, targetLocation.y, targetLocation.z);
        rain.setOwner(entity);
        world.addFreshEntity(rain);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
    /**
     * 伤害为 50% 魔法攻击
     */
    private float getDamage(int spellLevel, LivingEntity caster) {
        return 0.5f * FastAttributeUtil.getMagicAttack(caster) * (1f + ((spellLevel - 1) * 0.1f));
    }
}
