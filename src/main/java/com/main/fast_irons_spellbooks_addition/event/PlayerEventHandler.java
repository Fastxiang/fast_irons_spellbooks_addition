package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.spells.physical.HeroResonanceSpell;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import net.minecraft.world.entity.player.Player;
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
        Player player = event.getEntity();

        String elementName = player.getPersistentData().getString("hero_element");
        int index = 0;

        if (!elementName.isEmpty()) {
                TripleMagicMissileProjectile.ElementType element =
                        TripleMagicMissileProjectile.ElementType.valueOf(elementName);
                index = element.ordinal();
        }
        HeroResonanceSpell.elementIndexMap.put(player.getUUID(), index);

    }
}