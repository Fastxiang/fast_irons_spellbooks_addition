package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * 猩红刺狱-血债 到期结算时触发。
 *
 * <p>专属节点事件：结算伤害已经由 Java 侧打出，
 * KubeJS 可以在这里做「结算后」的追加效果（例如按结算量回血、叠层）。</p>
 */
public class BloodDebtSettleEvent extends Event {

    private final LivingEntity caster;
    private final LivingEntity target;
    private final float amount;

    public BloodDebtSettleEvent(LivingEntity caster, LivingEntity target, float amount) {
        this.caster = caster;
        this.target = target;
        this.amount = amount;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public LivingEntity getTarget() {
        return target;
    }

    /** 本次结算的伤害量（可能为 0，表示期间没有吃到猩红伤害） */
    public float getAmount() {
        return amount;
    }
}
