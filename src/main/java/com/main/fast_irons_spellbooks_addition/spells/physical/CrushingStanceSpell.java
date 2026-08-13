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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CrushingStanceSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("crushing_stance");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public CrushingStanceSpell() {
        this.baseManaCost = 10;
        this.manaCostPerLevel = 0;
        this.castTime = 610;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalSpellAnimations.CRUSHING_STANCE;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
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
        return CastType.CONTINUOUS;
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
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
    }

    public int getDamage(int spellLevel, LivingEntity entity) {
        float Attack = FastAttributeUtil.getPhysicalAttack(entity);
        double multiplier = (2 * (1 + (spellLevel - 1) * 0.1));
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
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        var Event = new FastSpellOnCastEvent(caster, spellLevel, getSpellId(), this);
        if (!MinecraftForge.EVENT_BUS.post(Event)) {
            double radius = 4.0D;

            List<LivingEntity> targets = world.getEntitiesOfClass(
                    LivingEntity.class,
                    caster.getBoundingBox().inflate(radius),
                    e -> e != caster && e.isAlive()
            );

            float damage = getDamage(spellLevel, caster);

            for (LivingEntity target : targets) {
                target.hurt(
                        getDamageSource(spellLevel, caster),
                        damage
                );
            }
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}
