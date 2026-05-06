package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionProjectile;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExplosionSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("explosion");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE.getId())
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public ExplosionSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 200;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
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

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", getDamageText(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return 30 * FastAttributeUtil.getMagicAttack(entity) * getDamageUp(spellLevel, entity);
    }

    private float getDamageUp(int spellLevel, LivingEntity entity) {
        Level world = entity.level();
        MagicData magicData = MagicData.getPlayerMagicData(entity);
        float mana = magicData.getMana();
        if (world.isClientSide) {
            mana = ClientMagicData.getPlayerMana();
        }
        return 1.0f + (mana / 100.0f) * 0.01f;
    }

    private float getDamageText(int spellLevel, LivingEntity entity) {
        if (entity != null) {
            return getDamage(spellLevel, entity);
        }
        return 0;
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
            Vec3 start = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            Vec3 end = start.add(look.scale(64));

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    world,
                    caster,
                    start,
                    end,
                    caster.getBoundingBox().expandTowards(look.scale(64)).inflate(1.5),
                    (entity) -> !entity.isSpectator() && entity.isPickable() && entity != caster
            );

            HitResult blockHit = caster.pick(64, 0, false);

            Vec3 targetPos;

            if (entityHit != null) {

                Entity target = entityHit.getEntity();
                targetPos = target.getBoundingBox().getCenter();

            } else if (blockHit.getType() != HitResult.Type.MISS) {

                targetPos = blockHit.getLocation();

            } else {
                targetPos = end;
            }

            Vec3 spawnPos = targetPos.add(0, 20, 0);

            ExplosionProjectile fireball = new ExplosionProjectile(world, caster);

            fireball.setDamage(getDamage(spellLevel, caster));
            fireball.setExplosionRadius(32);

            fireball.setPos(
                    spawnPos.x,
                    spawnPos.y - fireball.getBbHeight() / 2,
                    spawnPos.z
            );

            Vec3 direction = targetPos.subtract(spawnPos).normalize();

            fireball.shoot(direction.scale(0.28));

            world.addFreshEntity(fireball);
            magicData.setMana(0);
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
      }
}
