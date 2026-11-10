package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 【弱点标记】物理系 · 指向目标的标记法术。
 *
 * <p>对视线指向的单个目标挂【弱点标记】10 秒：这 10 秒内<b>下一次</b>对它造成的
 * 枪械爆头伤害翻倍，打完立刻消耗掉标记。翻倍本身在 KubeJS 侧做
 * （监听 TACZ 的 {@code EntityHurtByGunEvent$Pre}，判 {@code isHeadShot()}），
 * Java 这边只负责「选中目标 + 上标记」。</p>
 *
 * <p>为什么不在这里自己算伤害：枪械伤害由 TACZ 结算，走的是它自己的命中链路，
 * Java 侧插不进这一发；放在 KJS 的事件里改 {@code baseAmount} 才是唯一能
 * 让「这一枪本身」翻倍的位置。</p>
 *
 * <p>重复施放只刷新时长，不叠层：标记的语义是「下一次爆头」，
 * 叠两层也只有一次爆头可以消耗，多出来的没有意义。</p>
 */
public class WeakPointMarkSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("weak_point_mark");

    /** 标记持续时间：10 秒 */
    private static final int DURATION_TICKS = 200;
    /** 索敌距离 */
    private static final int RANGE = 24;
    /** 枪械爆头伤害倍率（KJS 侧读取同一个语义，这里只用于面板显示） */
    private static final int HEADSHOT_PERCENT = 200;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(5)
            .setAllowCrafting(false)
            .build();

    public WeakPointMarkSpell() {
        this.baseManaCost = 200;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
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
        return CastType.INSTANT;
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
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_duration", Utils.timeFromTicks(DURATION_TICKS, 1)),
                Component.translatable("ui.fast_irons_spellbooks_addition.weak_point_mark_multiplier", HEADSHOT_PERCENT)
        );
    }

    /** 瞬发技能同样要先选目标：没指到人就不施法、不扣蓝 */
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .2f, true);
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        var event = new FastSpellOnCastEvent(caster, spellLevel, getSpellId(), this);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            LivingEntity target = null;
            if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData
                    && world instanceof ServerLevel serverLevel) {
                target = targetData.getTarget(serverLevel);
            }

            if (target != null && target.isAlive() && target != caster) {
                // 已有标记时 addEffect 会刷新到新时长，amplifier 始终 0，不叠层
                target.addEffect(new MobEffectInstance(
                        FastEffectRegistry.WEAK_POINT_MARK.get(),
                        DURATION_TICKS,
                        0,
                        false,
                        true,
                        true
                ));

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.9f, 1.6f);
            }
        }

        super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}
