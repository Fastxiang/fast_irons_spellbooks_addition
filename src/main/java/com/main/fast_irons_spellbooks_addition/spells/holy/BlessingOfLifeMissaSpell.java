package com.main.fast_irons_spellbooks_addition.spells.holy;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.BlessingOfLifeMissaEvent;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命祝福-弥撒（第二分支 · 神圣 · A 方案）
 *
 * <p>第一分支「慈悲」是无差别群体治疗（连敌人一起奶）；这一条把治疗<b>只给友军</b>，
 * 自己完全吃不到任何效果，并加上两件神圣一直缺的东西：<b>团队回蓝</b> 和
 * <b>「按覆盖人数产生信仰」</b>。</p>
 *
 * <p>信仰是整合包 KubeJS 侧的资源，Java 读不到，所以这里只负责：
 * 治疗友军 → 给友方玩家回蓝 → 抛出 {@link BlessingOfLifeMissaEvent}，
 * 由 KubeJS 侧按覆盖人数发放信仰值（<b>施法者本人不算在内</b>，所以单人施放拿不到信仰）。</p>
 */
public class BlessingOfLifeMissaSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("blessing_of_life_missa");

    /** 覆盖半径 */
    private static final double RANGE = 10.0;
    /** 每个友方玩家回复的法力 */
    private static final int MANA_PER_ALLY = 500;
    /** 治疗系数：10 级 ≈ 494% 魔法攻击 */
    private static final float HEAL_COEFFICIENT = 2.6f;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public BlessingOfLifeMissaSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
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
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RANGE, 0)),
                Component.translatable("ui.fast_irons_spellbooks_addition.missa_mana", MANA_PER_ALLY)
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        if (!level.isClientSide) {
            float healAmount = getHealAmount(spellLevel, entity);
            AABB aabb = entity.getBoundingBox().inflate(RANGE, RANGE * 0.5, RANGE);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aabb);

            List<LivingEntity> allies = new ArrayList<>();

            for (LivingEntity target : targets) {
                if (target == null || !target.isAlive()) {
                    continue;
                }
                // 只给友军，自己完全不吃（治疗、回蓝、信仰都不给自己）
                if (target == entity || !Utils.shouldHealEntity(entity, target)) {
                    continue;
                }

                MinecraftForge.EVENT_BUS.post(new SpellHealEvent(entity, target, healAmount, getSchoolType()));
                target.heal(healAmount);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new HealParticlesPacket(target.position()));
                allies.add(target);

                // 团队回蓝
                if (target instanceof ServerPlayer player) {
                    MagicData data = MagicData.getPlayerMagicData(player);
                    if (data != null) {
                        data.addMana(MANA_PER_ALLY);
                        PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
                    }
                }
            }

            // 专属事件节点：信仰由 KubeJS 侧按覆盖人数发放（不含施法者本人）
            MinecraftForge.EVENT_BUS.post(new BlessingOfLifeMissaEvent(
                    entity,
                    spellLevel,
                    healAmount,
                    MANA_PER_ALLY,
                    allies
            ));
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getHealAmount(int spellLevel, LivingEntity caster) {
        return HEAL_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(1f, .85f, .3f);
    }
}
