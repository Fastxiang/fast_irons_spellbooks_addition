package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public class MagicFireballTrackingTargetEvent extends Event {

    private final LivingEntity caster;
    private LivingEntity target;
    private final double range;

    public MagicFireballTrackingTargetEvent(
            LivingEntity caster,
            LivingEntity target,
            double range
    ) {
        this.caster = caster;
        this.target = target;
        this.range = range;
    }

    /**
     * 施法者
     */
    public LivingEntity getCaster() {
        return caster;
    }

    /**
     * 当前自动选择的追踪目标。
     *
     * 可以通过 setTarget() 修改。
     * 设置为 null 则取消追踪。
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * 修改追踪目标。
     *
     * 设置为 null 可以让火球不追踪任何目标。
     */
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    /**
     * 自动寻找目标的最大范围。
     */
    public double getRange() {
        return range;
    }
}