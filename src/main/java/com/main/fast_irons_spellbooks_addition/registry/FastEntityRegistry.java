package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ArrowRainEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FastEntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FastIronsSpellbooksAddition.MODID);
            
    public static final RegistryObject<EntityType<TripleMagicMissileProjectile>>
            TRIPLE_MAGIC_MISSILE_PROJECTILE = ENTITY_TYPES.register(
            "triple_magic_missile",
            () -> EntityType.Builder.<TripleMagicMissileProjectile>of(
                    TripleMagicMissileProjectile::new,
                    MobCategory.MISC
            )
            .sized(0.5f, 0.5f)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("triple_magic_missile")
    );

    public static final RegistryObject<EntityType<ArrowRainEntity>>
            ARROW_RAIN = ENTITY_TYPES.register(
            "arrow_rain",
            () -> EntityType.Builder.<ArrowRainEntity>of(
                            ArrowRainEntity::new,
                            MobCategory.MISC
                    )
                    .sized(1f, 1f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("arrow_rain")
    );


    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
