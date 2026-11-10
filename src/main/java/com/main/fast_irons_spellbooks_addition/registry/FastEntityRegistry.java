package com.main.fast_irons_spellbooks_addition.registry;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import com.main.fast_irons_spellbooks_addition.entity.SkillArrowEntity;
import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import com.main.fast_irons_spellbooks_addition.entity.spells.fireball.FissionMagicFireball;
import com.main.fast_irons_spellbooks_addition.entity.spells.fireball.FissionSmallFireball;
import com.main.fast_irons_spellbooks_addition.entity.spells.fireball.MagicFireballTracking;
import com.main.fast_irons_spellbooks_addition.entity.spells.lightning.ThunderArrayEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ArrowRainEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.BloodRainEntity;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.MagicArrowDragonBreathProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionBurst;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.SmallExplosionProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.SwordEnergyProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.entity.spells.nature.MiasmaPoisonArrow;
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

    public static final RegistryObject<EntityType<SwordEnergyProjectile>>
            SWORD_ENERGY_PROJECTILE = ENTITY_TYPES.register(
            "sword_energy",
            () -> EntityType.Builder.<SwordEnergyProjectile>of(
                    SwordEnergyProjectile::new,
                    MobCategory.MISC
            )
            .sized(2f, 0.5f)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build("sword_energy")
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

    public static final RegistryObject<EntityType<BloodRainEntity>>
            BLOOD_RAIN = ENTITY_TYPES.register(
            "blood_rain",
            () -> EntityType.Builder.<BloodRainEntity>of(
                            BloodRainEntity::new,
                            MobCategory.MISC
                    )
                    .sized(1f, 1f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("blood_rain")
    );

    public static final RegistryObject<EntityType<MagicFireballTracking>>
            MAGIC_FIREBALL_TRACKING = ENTITY_TYPES.register(
            "magic_fireball_tracking",
            () -> EntityType.Builder.<MagicFireballTracking>of(
                            MagicFireballTracking::new,
                            MobCategory.MISC
                    )
                    .sized(1f, 1f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build("magic_fireball_tracking")
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

    public static final RegistryObject<EntityType<MagicArrowDragonBreathProjectile>> MAGIC_ARROW_DRAGON_BREATH =
            ENTITY_TYPES.register("magic_arrow_dragon_breath",
                    () -> EntityType.Builder.<MagicArrowDragonBreathProjectile>of(MagicArrowDragonBreathProjectile::new, MobCategory.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("magic_arrow_dragon_breath")
            );

    // ===== 第二分支新增 =====

    public static final RegistryObject<EntityType<FissionMagicFireball>> FISSION_MAGIC_FIREBALL =
            ENTITY_TYPES.register("fission_magic_fireball",
                    () -> EntityType.Builder.<FissionMagicFireball>of(FissionMagicFireball::new, MobCategory.MISC)
                            .sized(1f, 1f)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build("fission_magic_fireball")
            );

    public static final RegistryObject<EntityType<FissionSmallFireball>> FISSION_SMALL_FIREBALL =
            ENTITY_TYPES.register("fission_small_fireball",
                    () -> EntityType.Builder.<FissionSmallFireball>of(FissionSmallFireball::new, MobCategory.MISC)
                            .sized(.5f, .5f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("fission_small_fireball")
            );

    public static final RegistryObject<EntityType<ThunderArrayEntity>> THUNDER_ARRAY =
            ENTITY_TYPES.register("thunder_array",
                    () -> EntityType.Builder.<ThunderArrayEntity>of(ThunderArrayEntity::new, MobCategory.MISC)
                            .sized(6f, 1.2f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("thunder_array")
            );

    public static final RegistryObject<EntityType<MiasmaPoisonArrow>> MIASMA_POISON_ARROW =
            ENTITY_TYPES.register("miasma_poison_arrow",
                    () -> EntityType.Builder.<MiasmaPoisonArrow>of(MiasmaPoisonArrow::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("miasma_poison_arrow")
            );


    /** 幻咏侍灵：悬浮在主人身侧、按间隔代替主人释放记录的法术 */
    public static final RegistryObject<EntityType<PhantomCantor>> PHANTOM_CANTOR =
            ENTITY_TYPES.register("phantom_cantor",
                    () -> EntityType.Builder.<PhantomCantor>of(PhantomCantor::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("phantom_cantor")
            );

    /** 爆裂魔法的命中特效（纯视觉，无伤害逻辑） */
    public static final RegistryObject<EntityType<ExplosionBurst>> EXPLOSION_BURST =
            ENTITY_TYPES.register("explosion_burst",
                    () -> EntityType.Builder.<ExplosionBurst>of(ExplosionBurst::new, MobCategory.MISC)
                            .sized(1f, 1f)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                            .build("explosion_burst")
            );

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
