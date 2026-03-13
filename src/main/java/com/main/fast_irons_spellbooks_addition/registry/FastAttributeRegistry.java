package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class FastAttributeRegistry {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, FastIronsSpellbooksAddition.MODID);

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }

    public static final RegistryObject<Attribute> PHYSICAL_POWER =
            ATTRIBUTES.register("physical_power", () ->
                    new MagicPercentAttribute(
                            "attribute.fast_irons_spellbooks_addition.physical_power",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    public static final RegistryObject<Attribute> PHYSICAL_RESIST =
            ATTRIBUTES.register("physical_resist", () ->
                    new MagicPercentAttribute(
                            "attribute.fast_irons_spellbooks_addition.physical_resist",
                            1.0D,
                            -100.0D,
                            100.0D
                    ).setSyncable(true)
            );

    public static final RegistryObject<Attribute> MAGIC_ATTACK =
            ATTRIBUTES.register("magic_attack", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.magic_attack",
                            1.0D,
                            0.0D,
                            23333.0D
                    )
            );
            
    public static final RegistryObject<Attribute> BASIC_PHYSICAL_DAMAGE =
            ATTRIBUTES.register("basic_physical_damage", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.basic_physical_damage",
                            1.0D,
                            -100.0D,
                            100.0D
                    )
            );
            
    public static final RegistryObject<Attribute> BASIC_MAGIC_DAMAGE =
            ATTRIBUTES.register("basic_magic_damage", () ->
                    new RangedAttribute(
                            "attribute.fast_irons_spellbooks_addition.basic_magic_damage",
                            1.0D,
                            -100.0D,
                            100.0D
                    )
            );

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entity -> {
            event.add(entity, PHYSICAL_POWER.get());
            event.add(entity, PHYSICAL_RESIST.get());
            event.add(entity, MAGIC_ATTACK.get());
            event.add(entity, BASIC_PHYSICAL_DAMAGE.get());
            event.add(entity, BASIC_MAGIC_DAMAGE.get());
        });
    }
}
