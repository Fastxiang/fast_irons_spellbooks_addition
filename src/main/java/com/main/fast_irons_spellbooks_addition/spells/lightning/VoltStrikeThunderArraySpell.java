package com.main.fast_irons_spellbooks_addition.spells.lightning;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.lightning.ThunderArrayEntity;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.event.ThunderArrayCastEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * 伏特打击-雷阵（第二分支 · 雷）
 *
 * <p>第一分支是「冲进去贴身转一圈」，这一条改成「隔空砸一片」：
 * 在落点生成雷阵，1.5 秒内落下 6 道雷，每道 160% 魔法攻击（10 级约 1.6M，合计 9.6M）。
 * 每道命中的目标都会被清零无敌帧，所以 6 道可以全部结算在同一个怪身上。</p>
 *
 * <p>每次落雷命中返还 1 秒冷却（最多 3 秒），保留雷系「刷新」的身份，
 * 但<b>不施加感电</b>——感电那套留给雷霆碎片自己用。</p>
 */
public class VoltStrikeThunderArraySpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("volt_strike_thunder_array");

    /** 单道落雷系数：10 级 ≈ 160% 魔法攻击 */
    private static final float DAMAGE_COEFFICIENT = 0.84f;
    /** 落点搜索距离 */
    private static final int RANGE = 30;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(10)
            .build();

    public VoltStrikeThunderArraySpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.fast_irons_spellbooks_addition.thunder_array_strikes", 6),
                Component.translatable("ui.irons_spellbooks.radius", 3)
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

        Vec3 center = null;
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData
                && world instanceof ServerLevel serverLevel) {
            center = targetData.getTargetPosition(serverLevel);
        }
        if (center == null) {
            center = RaycastBuilder.begin(world, entity)
                    .range(RANGE)
                    .checkForBlocks(true)
                    .build()
                    .getLocation();
        }
        // 雷阵站在地面上
        center = Utils.moveToRelativeGroundLevel(world, center, 4, 4);

        float damage = getDamage(spellLevel, entity);

        ThunderArrayEntity array = new ThunderArrayEntity(world, entity);
        array.setDamage(damage);
        array.setPos(center.x, center.y, center.z);
        world.addFreshEntity(array);

        MinecraftForge.EVENT_BUS.post(new ThunderArrayCastEvent(
                entity,
                spellLevel,
                center,
                6,
                damage,
                3f
        ));

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    /** 单道落雷伤害 */
    private float getDamage(int spellLevel, LivingEntity caster) {
        return DAMAGE_COEFFICIENT
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }
}
