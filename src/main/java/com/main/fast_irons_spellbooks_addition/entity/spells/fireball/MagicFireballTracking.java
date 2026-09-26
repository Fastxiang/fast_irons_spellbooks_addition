package com.main.fast_irons_spellbooks_addition.entity.spells.fireball;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * 巨型火球-追踪
 * 继承原版 {@link MagicFireball}，复用其爆炸/粒子/音效逻辑。
 * 追踪能力来自 {@code AbstractMagicProjectile} 内置的 homing 机制：
 * 法术在 {@code onCast} 中调用 {@link #setHomingTarget(LivingEntity)} 即可启用追踪，
 * 父类的 {@code handleEntityHoming()} 会在 tick 中自动调整弹道朝向目标。
 */
public class MagicFireballTracking extends MagicFireball {
    public MagicFireballTracking(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public MagicFireballTracking(Level pLevel, LivingEntity pShooter) {
        this(FastEntityRegistry.MAGIC_FIREBALL_TRACKING.get(), pLevel);
        this.setOwner(pShooter);
    }
}
