package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public class FastSoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FastIronsSpellbooksAddition.MODID);

    public static final RegistryObject<SoundEvent> EMPTY =
            SOUND_EVENTS.register("empty", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(FastIronsSpellbooksAddition.MODID, "empty")
                    )
            );

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}