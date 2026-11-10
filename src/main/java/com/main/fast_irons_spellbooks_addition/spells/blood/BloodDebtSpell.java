package com.main.fast_irons_spellbooks_addition.spells.blood;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.BloodDebtCastEvent;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.util.BloodDebtManager;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * 猩红刺狱-血债（第二分支 · 猩红 · B 方案）
 *
 * <p>给目标挂一枚【血债】10 秒：期间你对它造成的<b>猩红伤害</b>会被记账，
 * 到期按累计量的 40% 一次性打出。期间只要你被治疗（猩红自带吸血也算），
 * 血债就会缩短 1 秒——吸得越猛，炸得越早。</p>
 *
 * <p>血债以负面 Effect 的形式挂在目标身上，玩家可以在 HUD 直接看到剩余时间；
 * <b>重复施放不会刷新时长、也不会被覆盖</b>，已有债时后续伤害只是继续记账。</p>
 *
 * <p>施放时本身也会打一发 300% 魔法攻击的猩红伤害（10 级约 3M），
 * 并且这一发同样会被记账（先挂债再结算），所以<b>只带这一条分支也能玩</b>：
 * 单发 = 3M 立刻 + 1.2M 到期，共约 4.2M；配合其它猩红技能才是它的上限。</p>
 *
 * <p>注意：不消耗生命（献祭已由「血祭之匕」占用），走的是「吸血」这一侧。</p>
 */
public class BloodDebtSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("blood_debt");

    /** 初始伤害系数：10 级 ≈ 300% 魔法攻击 */
    private static final float INITIAL_COEFFICIENT = 1.6f;
    /** 血债时长 */
    private static final int DURATION_TICKS = 200;
    /** 记账比例 */
    private static final float RATIO = 0.4f;
    /** 索敌距离 */
    private static final int RANGE = 20;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(20)
            .build();

    public BloodDebtSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getInitialDamage(spellLevel, caster), 2)),
                Component.translatable("ui.fast_irons_spellbooks_addition.blood_debt_ratio", (int) (RATIO * 100)),
                Component.translatable("ui.irons_spellbooks.effect_duration", Utils.timeFromTicks(DURATION_TICKS, 1))
        );
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
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .2f, false);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        LivingEntity target = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData
                && world instanceof ServerLevel serverLevel) {
            target = targetData.getTarget(serverLevel);
        }

        if (target != null && target.isAlive()) {
            // 专属事件节点：KubeJS 可以在这里改时长/比例
            var debtEvent = new BloodDebtCastEvent(entity, target, spellLevel, DURATION_TICKS, RATIO);
            MinecraftForge.EVENT_BUS.post(debtEvent);

            int duration = Math.max(20, debtEvent.getDurationTicks());
            float ratio = Math.max(0f, debtEvent.getRatio());

            // 已有血债：不刷新、不覆盖，只继续记账
            boolean armed = target.hasEffect(FastEffectRegistry.BLOOD_DEBT.get())
                    || target.addEffect(new MobEffectInstance(
                    FastEffectRegistry.BLOOD_DEBT.get(),
                    duration,
                    0,
                    false,
                    true,
                    true
            ));
            if (armed) {
                BloodDebtManager.apply(entity, target, ratio);
            }

            DamageSources.applyDamage(
                    target,
                    getInitialDamage(spellLevel, entity),
                    getDamageSource(entity)
            );

            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.9f, 0.7f);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /** 初始伤害：300% 魔法攻击 */
    private float getInitialDamage(int spellLevel, LivingEntity caster) {
        return INITIAL_COEFFICIENT
                * FastAttributeUtil.getMagicAttack(caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }
}
