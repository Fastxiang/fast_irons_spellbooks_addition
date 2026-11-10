package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.spells.physical.GreatswordArmamentSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 【大剑武装】状态期间屏蔽原版单体攻击。
 *
 * <h2>这里只做一件事：cancel</h2>
 * <p>伤害、判定、命中事件全部在 {@code GreatswordSlashSpell} 里，和物理三连击同一套；
 * 本类不碰数值，只负责把"原版那一刀"掐掉，避免一次左键吃两份伤害。</p>
 *
 * <p>为什么需要它：整合包的 {@code player_attack.js} 只在手持带
 * {@code BasicAttackData} 标记的物品时才会在客户端取消攻击。拿着别的武器进入武装状态时，
 * 原版单体攻击会照常发出，和法术的扇形横扫叠在一起。服务端这一层是无条件兜底。</p>
 *
 * <p>触发顺序：客户端 {@code LeftClickTick} → KubeJS 施放 {@code greatsword_slash}；
 * 同一 tick 的原版攻击在这里被取消。两者互不依赖，任一入口失效都还有另一条路。</p>
 */
@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GreatswordArmamentGuard {

    private GreatswordArmamentGuard() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) return;

        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) return;

        if (magicData.getPlayerRecasts().hasRecastForSpell(GreatswordArmamentSpell.getSpellResourceId().toString())) {
            event.setCanceled(true);
        }
    }
}
