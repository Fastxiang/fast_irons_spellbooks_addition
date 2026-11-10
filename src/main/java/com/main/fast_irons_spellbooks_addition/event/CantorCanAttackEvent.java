package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * 幻咏侍灵索敌时，为每个候选目标触发一次。
 *
 * <p>Java 侧只做「活着 / 同维度 / 不是主人 / 不是同队」这类硬判断，
 * 敌我关系（友军、宠物、女仆、其他召唤物）交给监听方决定：
 * 在监听器里调用 {@link #setCanAttack(boolean)} 就能把这只从候选里剔掉。</p>
 *
 * <p>被否决后侍灵会继续找下一个候选；一个都通过不了，这一轮就不指定目标
 * （法术类的 {@code checkPreCastConditions} 会因此失败，等下一次冷却再试）。</p>
 *
 * <p>只在服务端触发（索敌跑在 {@code PhantomCantor#serverTick}）。</p>
 */
public class CantorCanAttackEvent extends Event {

    private final PhantomCantor cantor;
    private final LivingEntity owner;
    private final LivingEntity target;

    private final boolean originalCanAttack;
    private boolean canAttack;

    public CantorCanAttackEvent(
            PhantomCantor cantor,
            LivingEntity owner,
            LivingEntity target,
            boolean originalCanAttack
    ) {
        this.cantor = cantor;
        this.owner = owner;
        this.target = target;
        this.originalCanAttack = originalCanAttack;
        this.canAttack = originalCanAttack;
    }

    /**
     * 发起索敌的侍灵
     */
    public PhantomCantor getCantor() {
        return cantor;
    }

    /**
     * 侍灵的主人
     */
    public LivingEntity getOwner() {
        return owner;
    }

    /**
     * 本次被判断的候选目标
     */
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Java 侧原本的判断结果（true = 基础条件都满足）
     */
    public boolean isOriginalCanAttack() {
        return originalCanAttack;
    }

    /**
     * 最终结果。初始为 originalCanAttack
     */
    public boolean isCanAttack() {
        return canAttack;
    }

    /**
     * 修改最终结果。设为 false 即把这只从候选里剔除
     */
    public void setCanAttack(boolean canAttack) {
        this.canAttack = canAttack;
    }
}
