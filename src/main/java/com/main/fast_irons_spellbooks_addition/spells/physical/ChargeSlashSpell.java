package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.spells.PhysicalSpellAnimations;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.common.ForgeMod.ENTITY_REACH;

public class ChargeSlashSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("charge_slash");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public ChargeSlashSpell() {
        this.baseManaCost = 800;
        this.manaCostPerLevel = 0;
        this.castTime = 80;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalSpellAnimations.CHARGE_SLASH;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
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

    public int getDamage(int spellLevel, LivingEntity entity) {
        float Attack = FastAttributeUtil.getPhysicalAttack(entity);
        double multiplier = (12 * (1 + (spellLevel - 1) * 0.1));
        return (int) (Attack * multiplier);
    }

    private DamageSource getDamageSource(int spellLevel, LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.damageSources().playerAttack(player);
        } else {
            return entity.damageSources().mobAttack(entity);
        }
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
            DamageSource ds = getDamageSource(spellLevel, caster);
            double range = 3;

            var attr = caster.getAttribute(ENTITY_REACH.get());
            if (attr != null) {
                double value = attr.getValue();
                if (value > 0) {
                    range = value;
                }
            }
            double halfAngle = Math.toRadians(60);

            Vec3 lookVec = caster.getLookAngle();
            Vec3 casterPos = caster.position().add(0, caster.getEyeHeight(), 0);

            List<LivingEntity> hitTargets = new ArrayList<>();

            for (LivingEntity e : world.getEntitiesOfClass(
                    LivingEntity.class,
                    caster.getBoundingBox().inflate(range),
                    target -> target != caster
            )) {
                Vec3 targetPos = e.position().add(0, e.getEyeHeight() / 2, 0);
                Vec3 dirToTarget = targetPos.subtract(casterPos).normalize();

                double distance = casterPos.distanceTo(targetPos);
                double dot = lookVec.dot(dirToTarget);

                if (distance < 0.5 || dot >= Math.cos(halfAngle)) {
                    hitTargets.add(e);
                }
            }

            if (!hitTargets.isEmpty()) {
                for (LivingEntity e : hitTargets) {
                    e.invulnerableTime = 0;
                    e.hurt(ds, getDamage(spellLevel, caster));
                    e.invulnerableTime = 20;
                }
            }
        }

        super.onCast(world, spellLevel, caster, castSource, magicData);
      }
}
