package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.spells.blood.BloodDebtSpell;
import com.main.fast_irons_spellbooks_addition.spells.blood.BloodRainSpell;
import com.main.fast_irons_spellbooks_addition.spells.ender.MagicArrowMovingDragonBreathSpell;
import com.main.fast_irons_spellbooks_addition.spells.ender.MagicArrowSwiftSpell;
import com.main.fast_irons_spellbooks_addition.spells.evocation.FirecrackerTwinSoulSpell;
import com.main.fast_irons_spellbooks_addition.spells.fire.MagicFireballFissionSpell;
import com.main.fast_irons_spellbooks_addition.spells.fire.MagicFireballTrackingSpell;
import com.main.fast_irons_spellbooks_addition.spells.holy.BlessingOfLifeCompassionSpell;
import com.main.fast_irons_spellbooks_addition.spells.holy.BlessingOfLifeMissaSpell;
import com.main.fast_irons_spellbooks_addition.spells.ice.RayOfFrostConcertoSpell;
import com.main.fast_irons_spellbooks_addition.spells.ice.RayOfFrostPursuitSpell;
import com.main.fast_irons_spellbooks_addition.spells.lightning.VoltStrikeRefreshSpell;
import com.main.fast_irons_spellbooks_addition.spells.lightning.VoltStrikeThunderArraySpell;
import com.main.fast_irons_spellbooks_addition.spells.nature.PoisonArrowMiasmaBurstSpell;
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

    public static final RegistryObject<AbstractSpell> TRIPLE_SLASH =
            registerSpell(new TripleSlashSpell());

    public static final RegistryObject<AbstractSpell> SIX_STRIKE =
            registerSpell(new SixStrikeSpell());

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

    /** 弱点标记：给目标挂 10 秒标记，下一次枪械爆头伤害翻倍（依赖 tacz） */
    public static final RegistryObject<AbstractSpell> WEAK_POINT_MARK =
            registerIfLoaded("tacz", new WeakPointMarkSpell());

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

    public static final RegistryObject<AbstractSpell> GREATSWORD_ARMAMENT =
            registerSpell(new GreatswordArmamentSpell());

    /** 大剑武装状态下左键打出的那一刀，不可获取，只能由状态触发 */
    public static final RegistryObject<AbstractSpell> GREATSWORD_SLASH =
            registerSpell(new GreatswordSlashSpell());

    public static final RegistryObject<AbstractSpell> HERO_RESONANCE =
            registerSpell(new HeroResonanceSpell());

    public static final RegistryObject<AbstractSpell> EXPLOSION =
            registerSpell(new ExplosionSpell());

    public static final RegistryObject<AbstractSpell> MIRROR_SHOOTER =
            registerSpell(new MirrorShooterSpell());

    public static final RegistryObject<AbstractSpell> VOLT_STRIKE_REFRESH =
            registerSpell(new VoltStrikeRefreshSpell());

    public static final RegistryObject<AbstractSpell> RAY_OF_FROST_CONCERTO =
            registerSpell(new RayOfFrostConcertoSpell());

    public static final RegistryObject<AbstractSpell> BLOOD_RAIN =
            registerSpell(new BloodRainSpell());

    public static final RegistryObject<AbstractSpell> BLESSING_OF_LIFE_COMPASSION =
            registerSpell(new BlessingOfLifeCompassionSpell());

    public static final RegistryObject<AbstractSpell> MAGIC_ARROW_SWIFT =
            registerSpell(new MagicArrowSwiftSpell());

    public static final RegistryObject<AbstractSpell> MAGIC_ARROW_MOVING_DRAGON_BREATH =
            registerSpell(new MagicArrowMovingDragonBreathSpell());

    public static final RegistryObject<AbstractSpell> MAGIC_FIREBALL_TRACKING =
            registerSpell(new MagicFireballTrackingSpell());

    public static final RegistryObject<AbstractSpell> MAGIC_BEAM =
            registerSpell(new MagicBeamSpell());

    // ===== 第二分支 =====

    /** 巨型火球-裂变（火） */
    public static final RegistryObject<AbstractSpell> MAGIC_FIREBALL_FISSION =
            registerSpell(new MagicFireballFissionSpell());

    /** 伏特打击-雷阵（雷） */
    public static final RegistryObject<AbstractSpell> VOLT_STRIKE_THUNDER_ARRAY =
            registerSpell(new VoltStrikeThunderArraySpell());

    /** 猩红刺狱-血债（猩红） */
    public static final RegistryObject<AbstractSpell> BLOOD_DEBT =
            registerSpell(new BloodDebtSpell());

    /** 生命祝福-弥撒（神圣） */
    public static final RegistryObject<AbstractSpell> BLESSING_OF_LIFE_MISSA =
            registerSpell(new BlessingOfLifeMissaSpell());

    /** 烟火四射-双生（唤魔） */
    public static final RegistryObject<AbstractSpell> FIRECRACKER_TWIN_SOUL =
            registerSpell(new FirecrackerTwinSoulSpell());

    /** 毒箭射击-瘴爆（自然） */
    public static final RegistryObject<AbstractSpell> POISON_ARROW_MIASMA_BURST =
            registerSpell(new PoisonArrowMiasmaBurstSpell());

    /** 冰霜射线-追击（冰霜射线的分支）：吟唱 1.5 秒开一个 30 秒追击窗口 */
    public static final RegistryObject<AbstractSpell> RAY_OF_FROST_PURSUIT =
            registerSpell(new RayOfFrostPursuitSpell());

    private static RegistryObject<AbstractSpell> registerIfLoaded(String modid, AbstractSpell spell) {
        if (ModList.get().isLoaded(modid)) {
            return registerSpell(spell);
        }
        return null;
    }
}
