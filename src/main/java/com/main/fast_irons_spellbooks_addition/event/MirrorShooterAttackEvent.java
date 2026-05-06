package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * 当镜像射手对目标造成近战伤害时触发。
 * 可以取消事件来阻止伤害，或者修改伤害值。
 */
@Cancelable
public class MirrorShooterAttackEvent extends LivingEvent {
    private final MirrorShooterEntity shooter;
    private final LivingEntity target;
    private final DamageSource damageSource;
    private float damage;

    public MirrorShooterAttackEvent(MirrorShooterEntity shooter, LivingEntity target, DamageSource source, float damage) {
        super(shooter);
        this.shooter = shooter;
        this.target = target;
        this.damageSource = source;
        this.damage = damage;
    }

    public MirrorShooterEntity getShooter() {
        return shooter;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }
}