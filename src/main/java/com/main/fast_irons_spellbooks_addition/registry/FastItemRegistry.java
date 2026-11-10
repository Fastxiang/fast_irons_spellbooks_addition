package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册。
 *
 * <p>目前只有一个物品：{@link #GREATSWORD}。它<b>不是能拿到的道具</b>，
 * 只是给「大剑武装」状态提供一份可渲染的模型 —— 状态期间
 * {@code ItemInHandLayer} / {@code ItemInHandRenderer} 会把玩家手里的物品换成这个 ItemStack，
 * 于是不管原本拿的是剑、法杖还是面包，看上去都是同一把大剑。</p>
 *
 * <p>模型在 {@code models/item/greatsword.json}，贴图在 {@code textures/item/greatsword.png}。</p>
 */
public final class FastItemRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FastIronsSpellbooksAddition.MODID);

    public static final RegistryObject<Item> GREATSWORD =
            ITEMS.register("greatsword", () -> new Item(new Item.Properties().stacksTo(1)));

    private FastItemRegistry() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
