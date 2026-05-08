package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ArrowRainEntity;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArrowRainSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("arrow_rain");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(20)
            .setAllowCrafting(false)
            .build();

    public ArrowRainSpell() {
        this.baseManaCost = 150;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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

    public int getDamage(int spellLevel, LivingEntity entity) {
        float Attack = FastAttributeUtil.getPhysicalAttack(entity);
        double multiplier = (1 * (1 + (spellLevel - 1) * 0.1));
        return (int) (Attack * multiplier);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        Utils.preCastTargetHelper(level, entity, playerMagicData, this, 48, .25f, false);
        return true;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        var Event = new FastSpellOnCastEvent(caster, spellLevel, getSpellId(), this);
        if (!MinecraftForge.EVENT_BUS.post(Event)) {
            Vec3 targetLocation = null;
            if (magicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                targetLocation = castTargetingData.getTargetPosition((ServerLevel) world);
            }
            if (targetLocation == null) {
                targetLocation = Utils.raycastForEntity(world, caster, 100, true).getLocation();
            }

                ArrowRainEntity rain = new ArrowRainEntity(FastEntityRegistry.ARROW_RAIN.get(), world);

                rain.setDamage(getDamage(spellLevel, caster));

                rain.setPos(targetLocation);
                rain.setOwner(caster);
                rain.setArrowsPerRow(10);
                rain.setRows(16);

                world.addFreshEntity(rain);
        }

        super.onCast(world, spellLevel, caster, castSource, magicData);
      }
}
