package com.main.fast_irons_spellbooks_addition.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * 毒箭射击-瘴爆 的<b>单目标</b>引爆事件。
 *
 * <p>分工：</p>
 * <ol>
 *   <li>Java 找到范围内的目标，按「基础自然伤害 + 每层增伤 × 层数」的公式准备好一次伤害</li>
 *   <li>发本事件 → KubeJS 检查目标身上的烈毒（{@code fast:toxic}）、就地消耗，
 *       把消耗掉的层数用 {@link #setConsumedLayers(int)} 回填给 Java</li>
 *   <li>事件结束后 Java 才真正打出这<b>唯一的一发</b>自然伤害
 *       （不再是「箭体直伤 + 引爆」两段，也不会每个目标吃两次）</li>
 * </ol>
 *
 * <p>KubeJS 侧写法：</p>
 * <pre>
 * NativeEvents.onEvent($MiasmaBurstEvent, event => {
 *     let target = event.getTarget()
 *     let effect = target.getEffect("fast:toxic")
 *     let layers = effect ? effect.getAmplifier() + 1 : 0
 *     if (effect) target.removeEffect("fast:toxic")
 *     event.setConsumedLayers(layers)
 *     // 想额外改伤害就再调 event.setDamage(...)
 * })
 * </pre>
 */
@Cancelable
public class MiasmaBurstEvent extends Event {

    private final LivingEntity caster;
    private final int spellLevel;
    private final Vec3 position;
    private final LivingEntity target;
    private final float baseDamage;
    private final float damagePerLayer;
    private final int maxLayers;

    /** KubeJS 回填：实际消耗掉的烈毒层数 */
    private int consumedLayers;
    /** 显式覆盖的最终伤害；null 表示按层数自动算 */
    private Float overrideDamage;
    /** KubeJS 是否已经回填过（用于区分「没处理」和「处理了但 0 层」） */
    private boolean layersReported;

    public MiasmaBurstEvent(
            LivingEntity caster,
            int spellLevel,
            Vec3 position,
            LivingEntity target,
            float baseDamage,
            float damagePerLayer,
            int maxLayers
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.position = position;
        this.target = target;
        this.baseDamage = baseDamage;
        this.damagePerLayer = damagePerLayer;
        this.maxLayers = maxLayers;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    /** 爆心 */
    public Vec3 getPosition() {
        return position;
    }

    /** 本次结算的目标 */
    public LivingEntity getTarget() {
        return target;
    }

    /** 不含层数的基础伤害（自然伤害） */
    public float getBaseDamage() {
        return baseDamage;
    }

    /** 每消耗一层烈毒增加的伤害 */
    public float getDamagePerLayer() {
        return damagePerLayer;
    }

    /** 层数上限，KubeJS 回填时会被夹在这个范围内 */
    public int getMaxLayers() {
        return maxLayers;
    }

    /** KubeJS 回填：本次从目标身上消耗掉的烈毒层数 */
    public void setConsumedLayers(int layers) {
        this.consumedLayers = Math.max(0, Math.min(layers, maxLayers));
        this.layersReported = true;
    }

    public int getConsumedLayers() {
        return consumedLayers;
    }

    /** KubeJS 是否回填过层数（没回填时 Java 认为没消耗烈毒） */
    public boolean isLayersReported() {
        return layersReported;
    }

    /** 最终伤害：有覆盖用覆盖，否则 = 基础伤害 + 每层增伤 × 消耗层数 */
    public float getDamage() {
        if (overrideDamage != null) {
            return overrideDamage;
        }
        return baseDamage + damagePerLayer * consumedLayers;
    }

    /** 直接覆盖最终伤害 */
    public void setDamage(float damage) {
        this.overrideDamage = damage;
    }

    /** 在当前最终伤害上乘算 */
    public void multiplyDamage(float multiplier) {
        this.overrideDamage = getDamage() * multiplier;
    }
}
