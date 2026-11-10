package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * 猩红刺狱-血债 成功挂载时触发。
 *
 * <p>专属节点事件：血债期间累计的猩红伤害由 Java 侧统一记账，
 * KubeJS 可以在这里读取献祭/吸血饰品并调整比例、时长。</p>
 */
public class BloodDebtCastEvent extends Event {

    private final LivingEntity caster;
    private final LivingEntity target;
    private final int spellLevel;
    private int durationTicks;
    private float ratio;

    public BloodDebtCastEvent(
            LivingEntity caster,
            LivingEntity target,
            int spellLevel,
            int durationTicks,
            float ratio
    ) {
        this.caster = caster;
        this.target = target;
        this.spellLevel = spellLevel;
        this.durationTicks = durationTicks;
        this.ratio = ratio;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    /** 血债比例（默认 0.4） */
    public float getRatio() {
        return ratio;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }
}
