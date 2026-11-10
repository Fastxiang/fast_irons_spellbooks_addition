package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * 巨型火球-裂变 主爆结算时触发。
 *
 * <p>专属节点事件：Java 侧只负责「主爆 + 分裂小火球」的通用逻辑，
 * KubeJS 侧可以在这里读取施法者属性、末影饰品信息，追加实例特有的效果。</p>
 */
public class MagicFireballFissionEvent extends Event {

    private final LivingEntity caster;
    private final int spellLevel;
    private final Vec3 position;
    private final float mainDamage;
    private final float smallDamage;
    private final int smallCount;

    public MagicFireballFissionEvent(
            LivingEntity caster,
            int spellLevel,
            Vec3 position,
            float mainDamage,
            float smallDamage,
            int smallCount
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.position = position;
        this.mainDamage = mainDamage;
        this.smallDamage = smallDamage;
        this.smallCount = smallCount;
    }

    /** 施法者（可能为 null，例如发射后被反魔法打断） */
    public LivingEntity getCaster() {
        return caster;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    /** 主爆中心 */
    public Vec3 getPosition() {
        return position;
    }

    /** 主爆满伤（爆心值） */
    public float getMainDamage() {
        return mainDamage;
    }

    /** 单颗小火球满伤 */
    public float getSmallDamage() {
        return smallDamage;
    }

    /** 分裂出的小火球数量 */
    public int getSmallCount() {
        return smallCount;
    }
}
