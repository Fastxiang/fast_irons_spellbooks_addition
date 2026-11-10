package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.registry.FastItemRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 【大剑武装】渲染侧的共用判断。
 *
 * <p>状态读的是同步到客户端的 {@code PlayerRecasts}：法术施放时服务端写入
 * {@code RecastInstance}，客户端拿到同一份数据，{@code hasRecastForSpell} 内部还会判
 * {@code remainingTicks > 0}，所以状态一到期这里立刻返回 false。</p>
 *
 * <p>{@link #displayStack()} 返回的那个 ItemStack 只用于渲染，
 * 不参与任何游戏逻辑，也不该出现在玩家背包里。</p>
 */
public final class GreatswordArmamentClient {

    private static final String SPELL_ID = FastIronsSpellbooksAddition.MODID + ":greatsword_armament";

    private static ItemStack displayStack;

    private GreatswordArmamentClient() {
    }

    public static boolean isArmed(Player player) {
        if (player == null) return false;
        try {
            var recasts = ClientMagicData.getRecasts();
            return recasts != null && recasts.hasRecastForSpell(SPELL_ID);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ItemStack displayStack() {
        if (displayStack == null) {
            displayStack = new ItemStack(FastItemRegistry.GREATSWORD.get());
        }
        return displayStack;
    }
}
