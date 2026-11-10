package com.main.fast_irons_spellbooks_addition.effect;

import com.main.fast_irons_spellbooks_addition.util.BloodDebtManager;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.effect.IMobEffectEndCallback;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 【血债】猩红刺狱-血债 挂在目标身上的负面效果。
 *
 * <p>做成负面 Effect 的目的只有三个：
 * <ol>
 *   <li>玩家能在 HUD 上直接看到剩余时间，不用猜</li>
 *   <li>「不能重复上、不会被覆盖」由 <code>addEffect</code> 的语义 + 施法时的显式判断保证</li>
 *   <li>到期结算挂在 {@link #onEffectRemoved} 上，时间到了自然炸，不依赖额外的 tick 记时器</li>
 * </ol>
 *
 * <p>累计伤害存在 {@link BloodDebtManager} 里（Effect 实例存不了 float），
 * 这个 Effect 只负责「计时 + 可视 + 到期结算」。</p>
 *
 * <p>{@link IMobEffectEndCallback} 是 ISS 提供的效果结束回调（服务端专属），
 * 比自己在 tick 里数时间更可靠：无论是自然到期还是被主动移除都会走到。</p>
 */
public class BloodDebtEffect extends MobEffect implements IMobEffectEndCallback {

    /** HUD 图标底色：暗猩红 */
    public static final int COLOR = 0x7A0C10;

    public BloodDebtEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 20 tick 掉一次血粒子
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        MagicManager.spawnParticles(
                entity.level(),
                ParticleHelper.BLOOD,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                2, .25, .25, .25, .01, false
        );
    }

    @Override
    public void onEffectRemoved(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        // 到期（或被动移除）时统一结算；死亡 / 主动清账的情况由 Manager 内部过滤
        BloodDebtManager.settle(entity);
    }
}
