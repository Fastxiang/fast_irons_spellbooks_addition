package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.effect.ArmorPenetrationEffect;
import com.main.fast_irons_spellbooks_addition.effect.WarCryEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FastEffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FastIronsSpellbooksAddition.MODID);

    public static final RegistryObject<MobEffect> WAR_CRY =
            EFFECTS.register("war_cry", WarCryEffect::new);

    public static final RegistryObject<MobEffect> ARMOR_PENETRATION =
            EFFECTS.register("armor_penetration", ArmorPenetrationEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}