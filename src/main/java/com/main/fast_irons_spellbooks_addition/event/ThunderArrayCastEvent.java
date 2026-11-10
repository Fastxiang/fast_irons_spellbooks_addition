package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * 伏特打击-雷阵 落点确定后触发。
 *
 * <p>专属节点事件：雷阵的「6 道落雷」是 Java 侧的地面持续区域，
 * KubeJS 可以在这里接管层数、额外落雷、与感电/冷却系统的联动。</p>
 */
public class ThunderArrayCastEvent extends Event {

    private final LivingEntity caster;
    private final int spellLevel;
    private final Vec3 center;
    private final int strikes;
    private final float damagePerStrike;
    private final float radius;

    public ThunderArrayCastEvent(
            LivingEntity caster,
            int spellLevel,
            Vec3 center,
            int strikes,
            float damagePerStrike,
            float radius
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.center = center;
        this.strikes = strikes;
        this.damagePerStrike = damagePerStrike;
        this.radius = radius;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    /** 雷阵中心（已吸附地面） */
    public Vec3 getCenter() {
        return center;
    }

    /** 落雷次数 */
    public int getStrikes() {
        return strikes;
    }

    /** 单次落雷满伤 */
    public float getDamagePerStrike() {
        return damagePerStrike;
    }

    /** 雷阵半径 */
    public float getRadius() {
        return radius;
    }
}
