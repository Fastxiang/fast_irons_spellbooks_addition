package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把 {@link PhantomCantor} 打出的法术伤害<b>改归属给它的主人</b>。
 *
 * <h2>为什么需要这一步</h2>
 * <p>随从是"以自己为施法者"释放法术的（这样投射物才会从它身上、朝它瞄准的方向飞出去）。
 * 代价是 {@code SpellDamageSource} 的归属实体变成了随从本身 —— 击杀不算主人、
 * 吸血回随从、所有「造成魔法伤害时」类饰品也不会触发。</p>
 *
 * <h2>做法</h2>
 * <p>在 {@link SpellDamageEvent} 的<b>最早时机</b>（{@code HIGHEST}）拦截：
 * 取消这一发，然后用<b>主人的</b> {@code SpellDamageSource}（直伤实体仍保留原投射物，
 * 所以击退 / 免击退 / 命中特效都不变）重放一次。</p>
 *
 * <p>因为取消发生在其他监听器之前（Forge 默认不会把 canceled 事件继续派发），
 * 重放时整条链路（血债记账、KJS 监听、抗性计算、hurt 事件）只会对"玩家打的那一发"跑一次，
 * <b>不存在双倍加成</b>。重放的事件里归属是玩家，不会再次进入本方法，无递归风险。</p>
 */
@Mod.EventBusSubscriber(modid = FastIronsSpellbooksAddition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CantorDamageRedirect {

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        SpellDamageSource source = event.getSpellDamageSource();
        if (!(source.getEntity() instanceof PhantomCantor cantor)) {
            return;
        }

        LivingEntity owner = cantor.getOwnerEntity();
        if (owner == null
                || owner == cantor
                || owner instanceof PhantomCantor
                || !owner.isAlive()
                || owner.isSpectator()
                || owner.level() != cantor.level()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target == null || !target.isAlive() || target == owner) {
            return;
        }

        float amount = event.getAmount();
        if (amount <= 0) {
            return;
        }

        AbstractSpell spell = source.spell();
        Entity direct = source.getDirectEntity();

        // 先掐掉"随从打的那一发"，再以主人的名义重放
        event.setCanceled(true);

        SpellDamageSource redirected = SpellDamageSource.source(
                direct == null ? owner : direct,
                owner,
                spell
        );
        // 把后处理属性原样搬过去（吸血 / 点燃 / 冻结 / 无敌帧），否则法术自带效果会丢
        if (source.getLifestealPercent() > 0) {
            redirected.setLifestealPercent(source.getLifestealPercent());
        }
        if (source.getFireTime() > 0) {
            redirected.setFireTicks(source.getFireTime());
        }
        if (source.getFreezeTicks() > 0) {
            redirected.setFreezeTicks(source.getFreezeTicks());
        }
        if (source.getIFrames() >= 0) {
            redirected.setIFrames(source.getIFrames());
        }
        if (source.isIndirect()) {
            redirected.indirect();
        }

        // 重放出来的这一发本质上是侍灵打的：打上标记，
        // 别让 CantorTargetMemory 把它记成"主人主动攻击了这只"，
        // 否则侍灵会把自己上一发的目标一直喂成最高优先级，永远不换目标
        CantorTargetMemory.beginCantorDamage();
        try {
            DamageSources.applyDamage(target, amount, redirected);
        } finally {
            CantorTargetMemory.endCantorDamage();
        }
    }
}
