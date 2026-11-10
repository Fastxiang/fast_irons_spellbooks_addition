package com.main.fast_irons_spellbooks_addition.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * 连段类法术（物理三连 / 六连 / 三连斩 / 魔法三连 / 巨剑斩）。
 *
 * <p>{@code triple_strike} 这一族法术内部用一张 {@code comboMap} 记「当前打到第几段」，
 * 段号决定伤害倍率（第 3 段 1.4 倍）与吟唱时长。原版只在<b>玩家</b>开段时把它清零
 * （{@code if (caster instanceof Player) comboMap.put(uuid, 0)}），
 * 怪物 / 女仆这类非玩家施法者永远不会归零，于是段号会一直往上漂：
 * 明明是新一套的第一下，日志里显示 {@code combo=2}，重击也就落到了错误的段上。</p>
 *
 * <p>所以把「开段清零」暴露成一个接口，由外部（女仆战斗 AI 的 {@code startCombo}）在起手前调用，
 * 和玩家路径保持同一套段号语义。</p>
 */
public interface ComboSpell {

    /**
     * 把该施法者的段号归零，下一次 {@code onCast} 会从第 1 段开始。
     *
     * @param caster 施法者，null 时忽略
     */
    void resetCombo(LivingEntity caster);
}
