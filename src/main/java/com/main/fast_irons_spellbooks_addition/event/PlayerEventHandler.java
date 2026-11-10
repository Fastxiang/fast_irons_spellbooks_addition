package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.spells.physical.HeroResonanceSpell;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.network.FastNetwork;
import com.main.fast_irons_spellbooks_addition.network.SyncHeroElementPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEventHandler {

    /**
     * 当玩家登录服务器/进入世界时触发，用于初始化 HeroResonanceSpell 的 elementIndexMap
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) {
            return;
        }

        TripleMagicMissileProjectile.ElementType stored = HeroResonanceSpell.getStoredElement(joining);
        HeroResonanceSpell.elementIndexMap.put(joining.getUUID(), stored != null ? stored.ordinal() : 0);

        // 登录时做一次全量同步：把自己广播给所有在线玩家，同时拉取所有在线玩家的元素，
        // 避免先登录的玩家与新玩家之间元素显示不一致
        MinecraftServer server = joining.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            FastNetwork.sendToPlayer(other, new SyncHeroElementPacket(
                    joining.getUUID(), HeroResonanceSpell.getCurrentElement(joining).ordinal()));
            if (other != joining) {
                FastNetwork.sendToPlayer(joining, new SyncHeroElementPacket(
                        other.getUUID(), HeroResonanceSpell.getCurrentElement(other).ordinal()));
            }
        }
    }
}