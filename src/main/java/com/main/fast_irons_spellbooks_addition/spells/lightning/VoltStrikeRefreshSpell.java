package com.main.fast_irons_spellbooks_addition.spells.lightning;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class VoltStrikeRefreshSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("volt_strike_refresh");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", getDamage(spellLevel, caster)));
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(10)
            .build();

    public VoltStrikeRefreshSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 100;
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
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        entity.hasImpulse = true;
        float multiplier = 20 / 20f;

        Vec3 forward = entity.getLookAngle();

        //Create Dashing Movement Impulse
        var upwardness = forward.dot(new Vec3(0, 1, 0));
        var remap = 1 - (Math.max(0, upwardness) * 0.6f);
        var impulse = forward.scale(3 * multiplier).multiply(1, remap, 1);
        //Start Spin Attack
        if (entity.onGround()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(serverPlayer.getX(), serverPlayer.getY() + 1, serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
            } else {
                entity.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
            }
            impulse.add(0, 0.5, 0);
        } else {
            impulse.add(0, 0.25, 0);
        }
        entity.setDeltaMovement(new Vec3(
                Mth.lerp(.75f, entity.getDeltaMovement().x, impulse.x),
                Mth.lerp(.75f, entity.getDeltaMovement().y, impulse.y),
                Mth.lerp(.75f, entity.getDeltaMovement().z, impulse.z)
        ));
        entity.hurtMarked = true;

        entity.addEffect(new MobEffectInstance(FastEffectRegistry.VOLT_STRIKE_REFRESH.get(), 10, getDamage(spellLevel, entity), false, false, false));
        entity.invulnerableTime = 20;
        playerMagicData.getSyncedData().setSpinAttackType(SpinAttackType.LIGHTNING);
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private int getDamage(int spellLevel, LivingEntity caster) {
        return (int) (5f * getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f)));
    }
}
