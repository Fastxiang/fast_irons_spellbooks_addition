package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.spells.physical.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.spells.NoneSpell;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastSpellRegistry {
    
    public static final ResourceKey<Registry<AbstractSpell>> SPELL_REGISTRY_KEY =
            SpellRegistry.SPELL_REGISTRY_KEY;
            
    private static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SPELL_REGISTRY_KEY, FastIronsSpellbooksAddition.MODID);
            
    public static final IForgeRegistry<AbstractSpell> REGISTRY =
            SpellRegistry.REGISTRY.get();

    private static final NoneSpell NONE = new NoneSpell();

    private static final Map<SchoolType, List<AbstractSpell>> SCHOOLS_TO_SPELLS = new HashMap<>();

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }

    private static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void onConfigReload() {
        SCHOOLS_TO_SPELLS.clear();
    }
    
    public static final RegistryObject<AbstractSpell> TRIPLE_STRIKE =
            registerSpell(new TripleStrikeSpell());

    public static final RegistryObject<AbstractSpell> TRIPLE_MAGIC_MISSILE =
            registerSpell(new TripleMagicMissileSpell());

    public static final RegistryObject<AbstractSpell> TAUNT =
            registerSpell(new TauntSpell());

    public static final RegistryObject<AbstractSpell> WAR_CRY =
            registerSpell(new WarCrySpell());

    public static final RegistryObject<AbstractSpell> PHANTOM_STEP =
            registerSpell(new PhantomStepSpell());

    public static final RegistryObject<AbstractSpell> BRUTE_FURY =
            registerSpell(new BruteFurySpell());

    public static final RegistryObject<AbstractSpell> MANA_RECHARGE =
            registerSpell(new ManaRechargeSpell());

    public static final RegistryObject<AbstractSpell> AMMO_SUPPLY =
            registerIfLoaded("tacz", new AmmoSupplySpell());

    public static final RegistryObject<AbstractSpell> POWER_SHOT =
            registerSpell(new PowerShotSpell());

    public static final RegistryObject<AbstractSpell> PERFECT_GUARD =
            registerSpell(new PerfectGuardSpell());

    public static final RegistryObject<AbstractSpell> COUNTER =
            registerSpell(new CounterSpell());

    public static final RegistryObject<AbstractSpell> ARMOR_PENETRATION_COUNTER =
            registerSpell(new ArmorPenetrationCounterSpell());

    public static final RegistryObject<AbstractSpell> COUNTER_STANCE =
            registerSpell(new CounterStanceSpell());

    public static final RegistryObject<AbstractSpell> CHARGE_SLASH =
            registerSpell(new ChargeSlashSpell());

    public static final RegistryObject<AbstractSpell> BLADE_AWAKENING =
            registerSpell(new BladeAwakeningSpell());

    public static final RegistryObject<AbstractSpell> ARROW_RAIN =
            registerSpell(new ArrowRainSpell());

    public static final RegistryObject<AbstractSpell> RAGING_SLASH =
            registerSpell(new RagingSlashSpell());

    public static final RegistryObject<AbstractSpell> CRUSHING_STANCE =
            registerSpell(new CrushingStanceSpell());

    private static RegistryObject<AbstractSpell> registerIfLoaded(String modid, AbstractSpell spell) {
        if (ModList.get().isLoaded(modid)) {
            return registerSpell(spell);
        }
        return null;
    }
}
