package com.main.fast_irons_spellbooks_addition.spells.holy;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * 生命祝福-慈悲
 * 基于原版 BlessingOfLifeSpell 的分支变体：
 * - 不再锁定单个目标，改为治疗施法者周围 10 格范围内的所有 LivingEntity（无论敌我）
 * - 其他参数与原版一致：1.5秒吟唱、5秒CD、500蓝耗、稀有度COMMON
 */
public class BlessingOfLifeCompassionSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("blessing_of_life_compassion");

    /** 治疗半径（方块） */
    private static final double RANGE = 10.0;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public BlessingOfLifeCompassionSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;       // 1.5 秒吟唱（与原版一致）
        this.baseManaCost = 500;
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
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST_FINISH;
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
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.healing", Utils.stringTruncation(getHealAmount(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RANGE, 0))
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        if (!level.isClientSide) {
            float healAmount = getHealAmount(spellLevel, entity);
            // 10 格范围内所有 LivingEntity（无论敌我）
            AABB aabb = entity.getBoundingBox().inflate(RANGE, RANGE * 0.5, RANGE);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity target : targets) {
                if (target == null || !target.isAlive()) {
                    continue;
                }
                MinecraftForge.EVENT_BUS.post(new SpellHealEvent(entity, target, healAmount, getSchoolType()));
                target.heal(healAmount);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new HealParticlesPacket(target.position()));
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getHealAmount(int spellLevel, LivingEntity caster) {
        return 5f * getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f));
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(.85f, 0, 0);
    }
}
