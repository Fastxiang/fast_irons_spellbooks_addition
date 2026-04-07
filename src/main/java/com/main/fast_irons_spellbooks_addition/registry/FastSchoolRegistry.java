package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import com.main.fast_irons_spellbooks_addition.util.FastModTags;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.network.chat.Style;


@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class FastSchoolRegistry {
    
    private static final DeferredRegister<SchoolType> SCHOOLS =
    DeferredRegister.create(
        SchoolRegistry.SCHOOL_REGISTRY_KEY,
        FastIronsSpellbooksAddition.MODID
    );
    
    public static void register(IEventBus bus) { SCHOOLS.register(bus); }
    
    public static final ResourceLocation PHYSICAL_ID =
            FastIronsSpellbooksAddition.id("physical");
            
    public static final RegistryObject<SchoolType> PHYSICAL =
            SCHOOLS.register("physical", () ->
                    new SchoolType(
        PHYSICAL_ID,
        FastModTags.PHYSICAL_FOCUS,
        Component
        .translatable("school.fast_irons_spellbooks_addition.physical")
        .withStyle(Style.EMPTY.withColor(0xFFFFFF)),
        FastAttributeRegistry.PHYSICAL_POWER::get,
        FastAttributeRegistry.PHYSICAL_RESIST::get,
        FastSoundRegistry.EMPTY::get,
        DamageTypes.PLAYER_ATTACK
            )
            );
}
