package com.main.fast_irons_spellbooks_addition;

import com.main.fast_irons_spellbooks_addition.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;


@Mod(FastIronsSpellbooksAddition.MODID)
public class FastIronsSpellbooksAddition {
    public static final String MODID = "fast_irons_spellbooks_addition";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FastIronsSpellbooksAddition(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        
        FastEntityRegistry.register(bus);
        FastSchoolRegistry.register(bus);
        FastSpellRegistry.register(bus);
        FastEffectRegistry.register(bus);
        FastSoundRegistry.register(bus);
        FastAttributeRegistry.register(bus);
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    public static ResourceLocation id(@NotNull String path) {
        return ResourceLocation.fromNamespaceAndPath(FastIronsSpellbooksAddition.MODID, path);
    }
}
