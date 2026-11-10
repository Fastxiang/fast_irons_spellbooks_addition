package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * 烟火四射-双生 施放成功后触发。
 *
 * <p>专属节点事件：「下一个非唤魔技能冷却 -10 秒」由 Java 侧的
 * {@code TwinSoulTracker} 统一记账；KubeJS 可以在这里拿到窗口时长与缩短量，
 * 做实例特有的追加效果。</p>
 */
public class FirecrackerTwinSoulEvent extends Event {

    private final LivingEntity caster;
    private final int spellLevel;
    private final int durationTicks;
    private final int cooldownReductionTicks;

    public FirecrackerTwinSoulEvent(
            LivingEntity caster,
            int spellLevel,
            int durationTicks,
            int cooldownReductionTicks
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.durationTicks = durationTicks;
        this.cooldownReductionTicks = cooldownReductionTicks;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    /** 双生窗口时长（tick） */
    public int getDurationTicks() {
        return durationTicks;
    }

    /** 下一个非唤魔技能减少的冷却（tick） */
    public int getCooldownReductionTicks() {
        return cooldownReductionTicks;
    }
}
