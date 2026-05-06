package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MirrorShooterSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            FastIronsSpellbooksAddition.id("mirror_shooter");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public MirrorShooterSpell() {
        this.baseManaCost = 400;
        this.manaCostPerLevel = 0;
        this.castTime = 60;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", getDamageText(spellLevel, caster))
        );
    }

    public float getDamage(int spellLevel, LivingEntity entity) {
        float Attack = FastAttributeUtil.getPhysicalAttack(entity);
        float matk = FastAttributeUtil.getMagicAttack(entity);
        return (float) ((Attack * 0.5 + matk * 0.5) * (1 + (spellLevel - 1) * 0.1));
    }

    private float getDamageText(int spellLevel, LivingEntity entity) {
        if (entity != null) {
            return getDamage(spellLevel, entity);
        }
        return 0;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity caster,
                       CastSource castSource, MagicData magicData) {
        if (!world.isClientSide) {
            float damage = getDamage(spellLevel, caster);
            int max = 1;
            int lifetime = 1200;
            MirrorShooterEntity entity = MirrorShooterEntity.tryCreateForOwner(world, caster, damage, max);
            if (entity != null) {
                entity.setPos(caster.getX(), caster.getY(), caster.getZ());
                // 复制手持物品
                entity.setItemInHand(InteractionHand.MAIN_HAND, caster.getMainHandItem().copy());

                entity.setOwner(caster);

                // 设置持续时间为60秒 = 1200 ticks
                entity.setRemainingLife(lifetime);

                world.addFreshEntity(entity);
            }
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}