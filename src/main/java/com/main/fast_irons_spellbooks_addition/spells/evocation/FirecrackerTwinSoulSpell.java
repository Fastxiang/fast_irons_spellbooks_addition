package com.main.fast_irons_spellbooks_addition.spells.evocation;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.event.FirecrackerTwinSoulEvent;
import com.main.fast_irons_spellbooks_addition.util.TwinSoulTracker;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.ExtendedFireworkRocket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Random;

/**
 * 烟火四射-双生（第二分支 · 唤魔 · A 方案）
 *
 * <p>唤魔的定位是「和其他学派发生关系」。第一分支「狂欢」是把自己的伤害按原样
 * 再打一次（放大自己）；这一条反过来：<b>施放后 5 秒内，你下一个非唤魔技能
 * 冷却 -10 秒</b>，把唤魔做成「给别人点火」的那只手。</p>
 *
 * <p>记账逻辑在 {@link TwinSoulTracker}：窗口是一层 HUD 可见的「双生」Effect，
 * 冷却缩短挂 {@code SpellCooldownAddedEvent.Post}，生效一次就把图标吃掉。</p>
 */
public class FirecrackerTwinSoulSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("firecracker_twin_soul");

    /** 爆炸伤害系数：10 级 ≈ 950% 魔法攻击 */
    private static final float DAMAGE_COEFFICIENT = 5f;
    /** 施放距离 */
    private static final int RANGE = 15;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public FirecrackerTwinSoulSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.fast_irons_spellbooks_addition.twin_soul_bonus",
                        TwinSoulTracker.COOLDOWN_REDUCTION / 20)
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        Vec3 shootAngle = entity.getLookAngle().normalize();
        Vec3 spawn = RaycastBuilder.begin(world, entity)
                .range(RANGE)
                .checkForBlocks(true)
                .build()
                .getLocation()
                .subtract(shootAngle.scale(.25f));

        ExtendedFireworkRocket firework = new ExtendedFireworkRocket(
                world,
                randomFireworkRocket(),
                entity,
                spawn.x, spawn.y, spawn.z,
                true,
                getDamage(spellLevel, entity)
        );
        world.addFreshEntity(firework);
        firework.shoot(shootAngle.x, shootAngle.y, shootAngle.z, 0, 0);

        // 开窗
        TwinSoulTracker.apply(entity, TwinSoulTracker.DEFAULT_DURATION);

        MinecraftForge.EVENT_BUS.post(new FirecrackerTwinSoulEvent(
                entity,
                spellLevel,
                TwinSoulTracker.DEFAULT_DURATION,
                TwinSoulTracker.COOLDOWN_REDUCTION
        ));

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return DAMAGE_COEFFICIENT
                * getSpellPower(spellLevel, entity)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    private ItemStack randomFireworkRocket() {
        Random random = new Random();
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag properties = new CompoundTag();
        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();

        byte type = (byte) (random.nextInt(3) * 2);
        if (random.nextFloat() < .08f) {
            type = 3;
        }
        explosion.putByte("Type", type);
        if (random.nextInt(3) == 0) {
            explosion.putByte("Trail", (byte) 1);
        }
        if (random.nextInt(3) == 0) {
            explosion.putByte("Flicker", (byte) 1);
        }
        explosion.putIntArray("Colors", randomColors());
        explosions.add(explosion);

        properties.put("Explosions", explosions);
        properties.putByte("Flight", (byte) -1);
        rocket.addTagElement("Fireworks", properties);
        return rocket;
    }

    private int[] randomColors() {
        int[] colors = new int[3];
        Random random = new Random();
        for (int i = 0; i < colors.length; i++) {
            colors[i] = DYE_COLORS[random.nextInt(DYE_COLORS.length)];
        }
        return colors;
    }

    private static final int[] DYE_COLORS = {
            11546150,
            6192150,
            3949738,
            8991416,
            1481884,
            15961002,
            8439583,
            16701501,
            3847130,
            13061821,
            16351261,
            16383998
    };
}
