package com.main.fast_irons_spellbooks_addition.effect;

import io.redspace.ironsspellbooks.effect.ISyncedMobEffect;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 【双生】烟火四射-双生 挂在施法者身上的增益效果。
 *
 * <p>原来这套「5 秒窗口」只存在 {@code TwinSoulTracker} 的 HashMap 里，玩家完全看不见，
 * 不知道自己还有没有窗口、还剩多久。改成 Effect 之后：
 * <ul>
 *   <li>HUD 上直接显示出图标与剩余秒数</li>
 *   <li>剩余时间就是窗口时长，过期即失效，不需要额外的计时器</li>
 *   <li>减免一旦生效就 {@code removeEffect} 把图标吃掉，玩家能直观看到「已经被用掉了」</li>
 * </ul>
 *
 * <p>真正改写冷却值的逻辑仍在 {@code TwinSoulTracker}（挂 {@code SpellCooldownAddedEvent.Post}），
 * 这个 Effect 只负责「可视 + 计时 + 被消费」。</p>
 *
 * <p>继承 {@link MagicMobEffect} 是为了让反制法术（Counterspell）也能顺手打掉这层状态。</p>
 */
public class TwinSoulEffect extends MagicMobEffect implements ISyncedMobEffect {

    /** HUD 图标底色：烟火橙红 */
    public static final int COLOR = 0xFF8A3C;

    public TwinSoulEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    @Override
    public void clientTick(LivingEntity entity, MobEffectInstance instance) {
        var level = entity.level();
        for (int i = 0; i < 2; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 1.2;
            double y = entity.getY() + entity.getBbHeight() * (0.15 + entity.getRandom().nextDouble() * 0.8);
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 1.2;
            Vec3 motion = new Vec3(
                    Mth.nextDouble(entity.getRandom(), -0.02, 0.02),
                    Mth.nextDouble(entity.getRandom(), 0.01, 0.04),
                    Mth.nextDouble(entity.getRandom(), -0.02, 0.02)
            );
            level.addParticle(ParticleHelper.EMBERS, x, y, z, motion.x, motion.y, motion.z);
        }
    }
}
