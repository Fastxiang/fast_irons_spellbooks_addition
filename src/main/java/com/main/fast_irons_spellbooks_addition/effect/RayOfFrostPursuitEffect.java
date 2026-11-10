package com.main.fast_irons_spellbooks_addition.effect;

import io.redspace.ironsspellbooks.effect.ISyncedMobEffect;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 【冰霜追击】冰霜射线-追击 挂在施法者身上的窗口效果。
 *
 * <p>30 秒内，施法者<b>造成任意伤害</b>时都会从身侧随机一处射出一条冰霜射线追打同一目标：
 * 伤害按冰霜射线本体结算、命中回复 {@link com.main.fast_irons_spellbooks_addition.util.RayOfFrostPursuitTracker#MANA_REFUND} 点法力，
 * 内部冷却 1 秒。</p>
 *
 * <p>amplifier 存的是<b>施法等级 - 1</b>（0~4），追击结算时按它还原等级、算本体伤害。</p>
 *
 * <p>这个 Effect 只负责「计时」：HUD 上能看到图标与剩余秒数，<b>不做任何粒子</b>
 * （曾经在 clientTick 里飘雪花，会糊在玩家眼前挡视野，已去掉）。
 * 真正的触发判定与结算在 {@code RayOfFrostPursuitTracker}（挂 {@code LivingHurtEvent}）。</p>
 *
 * <p>继承 {@link MagicMobEffect} 是为了让反制法术（Counterspell）也能顺手打掉这层状态。</p>
 */
public class RayOfFrostPursuitEffect extends MagicMobEffect implements ISyncedMobEffect {

    /** HUD 图标底色：冰蓝 */
    public static final int COLOR = 0x8CCBEE;

    public RayOfFrostPursuitEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }
}
