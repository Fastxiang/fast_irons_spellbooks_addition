package com.main.fast_irons_spellbooks_addition.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 【弱点标记】物理系「弱点标记」法术挂在目标身上的负面效果。
 *
 * <p>效果本身不做任何结算：伤害翻倍由 KubeJS 在
 * {@code com.tacz.guns.api.event.common.EntityHurtByGunEvent$Pre} 里完成 ——
 * 判到爆头且目标带这个 Effect 就把这次的基础伤害 ×2，随后把这个 Effect 吃掉。
 * Java 侧只负责「能被看到、能被判定、时间到自动消失」这三件事。</p>
 *
 * <p>做成可见 Effect 而不是记在 Map 里，原因是玩家得能知道
 * 「标记还挂着没有 / 还剩几秒」，而且重复施法靠 {@code addEffect} 的语义
 * 就自动处理成「刷新时长、不叠层」，不需要自己维护计时器。</p>
 */
public class WeakPointMarkEffect extends MobEffect {

    /** HUD 图标底色：准星红 */
    public static final int COLOR = 0xFF3B30;

    public WeakPointMarkEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 10 tick 在头部冒一次暴击粒子：弱点就在头上，视觉上也对得上
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.9,
                    entity.getZ(),
                    2, .18, .18, .18, .01
            );
        }
    }
}
