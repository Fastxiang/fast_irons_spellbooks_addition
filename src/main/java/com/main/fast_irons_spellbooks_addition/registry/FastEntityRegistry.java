package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import com.main.fast_irons_spellbooks_addition.entity.SkillArrowEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ArrowRainEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.SmallExplosionProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Arrow;
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

    public static final RegistryObject<EntityType<SmallExplosionProjectile>> SMALL_EXPLOSION_PROJECTILE =
            ENTITY_TYPES.register("small_explosion_projectile", () -> EntityType.Builder.<SmallExplosionProjectile>of(SmallExplosionProjectile::new, MobCategory.MISC)
                    .sized(.5f, .5f)
                    .clientTrackingRange(64)
                    .build("small_explosion_projectile")
    );

    public static final RegistryObject<EntityType<ExplosionProjectile>> EXPLOSION_PROJECTILE =
            ENTITY_TYPES.register("explosion_projectile", () -> EntityType.Builder.<ExplosionProjectile>of(ExplosionProjectile::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .clientTrackingRange(4)
                    .build("explosion_projectile")
            );

    public static final RegistryObject<EntityType<SkillArrowEntity>> SKILL_ARROW =
            ENTITY_TYPES.register("skill_arrow",
                    () -> EntityType.Builder.<SkillArrowEntity>of(SkillArrowEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("skill_arrow")
            );

    public static final RegistryObject<EntityType<MirrorShooterEntity>> MIRROR_SHOOTER =
            ENTITY_TYPES.register("mirror_shooter",
                    () -> EntityType.Builder.<MirrorShooterEntity>of(MirrorShooterEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)                // 玩家尺寸
                            .clientTrackingRange(64)           // 较远的追踪范围，便于客户端看到攻击
                            .updateInterval(2)                 // 更新频率高一些，保证攻击流畅
                            .build("mirror_shooter")
            );

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
