package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * 生命祝福-弥撒 结算时触发。
 *
 * <p>专属节点事件：治疗与队友回蓝是 Java 侧的通用逻辑；
 * 「信仰」是整合包 KubeJS 侧的资源，Java 无法直接读写，
 * 因此把覆盖到的友军交给 KubeJS，由 KubeJS 侧发放信仰值。</p>
 *
 * <p>注意：<b>施法者本人不在 {@link #getAllies()} 里</b>，弥撒对自己没有任何效果，
 * 所以单人施放（没有其他友军）时这个事件带的是空列表，不应产生信仰。</p>
 */
public class BlessingOfLifeMissaEvent extends Event {

    private final LivingEntity caster;
    private final int spellLevel;
    private final float healAmount;
    private final int manaRestored;
    private final List<LivingEntity> allies;

    public BlessingOfLifeMissaEvent(
            LivingEntity caster,
            int spellLevel,
            float healAmount,
            int manaRestored,
            List<LivingEntity> allies
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.healAmount = healAmount;
        this.manaRestored = manaRestored;
        this.allies = allies;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    /** 每个友军收到的治疗量 */
    public float getHealAmount() {
        return healAmount;
    }

    /** 每个友方玩家回复的法力值 */
    public int getManaRestored() {
        return manaRestored;
    }

    /** 被覆盖到的友军（<b>不含施法者本人</b>） */
    public List<LivingEntity> getAllies() {
        return allies;
    }

    /** 被覆盖到的友军数量（不含施法者本人，可能为 0） */
    public int getAllyCount() {
        return allies.size();
    }
}
