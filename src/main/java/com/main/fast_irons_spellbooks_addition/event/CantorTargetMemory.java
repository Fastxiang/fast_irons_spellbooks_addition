package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录「每个玩家最近主动打过谁」，供 {@link PhantomCantor} 索敌时优先锁定。
 *
 * <p>用 {@link LivingHurtEvent} 而不是 {@code getLastHurtMob()}：后者只覆盖近战，
 * 而这个事件对所有伤害来源都触发（近战 / 箭 / 枪械 / 法术），
 * 并且 {@code getSource().getEntity()} 就是发起攻击的实体。</p>
 *
 * <h2>为什么要排除"侍灵伤害"</h2>
 * <p>{@link CantorDamageRedirect} 会把侍灵打出的法术伤害<b>重放成主人的伤害</b>，
 * 重放链路同样会触发这个事件，攻击者看起来就是玩家本人。
 * 如果不做区分，侍灵每打一发就等于"主人主动攻击了这只"，
 * 于是它会把自己上一发的目标一直喂成最高优先级 —— 永远不换目标。
 * 所以重放期间由 {@link #beginCantorDamage()} / {@link #endCantorDamage()} 打标记，
 * 这期间的事件一律不记。</p>
 */
@Mod.EventBusSubscriber(modid = FastIronsSpellbooksAddition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CantorTargetMemory {

    private static final Map<UUID, Attack> ATTACKS = new ConcurrentHashMap<>();

    /**
     * 侍灵伤害重放的嵌套层数（服务端单线程，普通 int 足够）
     */
    private static int cantorDamageDepth = 0;

    private CantorTargetMemory() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (cantorDamageDepth > 0) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == player || target instanceof PhantomCantor) {
            return;
        }
        ATTACKS.put(player.getUUID(), new Attack(target, player.tickCount));
    }

    /**
     * 玩家最近 {@code memoryTicks} tick 内主动打过的、还活着的目标。
     *
     * @return 没有记录 / 已过期 / 目标已经死亡或被移除时返回 {@code null}
     */
    @Nullable
    public static LivingEntity getLastAttacked(LivingEntity player, int memoryTicks) {
        Attack attack = ATTACKS.get(player.getUUID());
        if (attack == null) {
            return null;
        }
        LivingEntity target = attack.target.get();
        if (target == null || target.isRemoved() || !target.isAlive()) {
            ATTACKS.remove(player.getUUID());
            return null;
        }
        if (player.tickCount - attack.tick > memoryTicks) {
            return null;
        }
        return target;
    }

    /**
     * 侍灵伤害开始重放：这期间产生的 hurt 事件不算玩家的主动攻击
     */
    public static void beginCantorDamage() {
        cantorDamageDepth++;
    }

    /**
     * 侍灵伤害重放结束，与 {@link #beginCantorDamage()} 配对
     */
    public static void endCantorDamage() {
        if (cantorDamageDepth > 0) {
            cantorDamageDepth--;
        }
    }

    /**
     * 一次攻击记录。目标用弱引用，避免玩家不再攻击后还把实体钉在内存里
     */
    private static final class Attack {
        private final WeakReference<LivingEntity> target;
        private final int tick;

        private Attack(LivingEntity target, int tick) {
            this.target = new WeakReference<>(target);
            this.tick = tick;
        }
    }
}
