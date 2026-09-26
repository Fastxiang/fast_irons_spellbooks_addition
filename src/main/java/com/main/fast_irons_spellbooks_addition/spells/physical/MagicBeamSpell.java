package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.spells.CastingMobAimingData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 魔法光束
 * 复刻原版铁魔法 Ray of Siphoning（持续射线）：
 * - 持续施法，每 tick 造成法术伤害
 * - 接入勇者调律：伤害类型随元素自动切换为对应学派伤害类型
 */
public class MagicBeamSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("magic_beam");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(10)
            .setCooldownSeconds(0)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getTickDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel), 1)));
    }

    public MagicBeamSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 1;
        this.castTime = 1000;
        this.baseManaCost = 0;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
    public ICastDataSerializable getEmptyCastData() {
        return new CastingMobAimingData();
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        if (playerMagicData.getAdditionalCastData() instanceof CastingMobAimingData aimData && entity instanceof Mob mob) {
            var target = mob.getTarget();
            if (target != null) {
                aimData.updateAim(target, .15f);
            }
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 forward = entity.getForward();
        if (playerMagicData.getAdditionalCastData() instanceof CastingMobAimingData aimData && entity instanceof Mob mob) {
            forward = aimData.getForward(entity);
        }
        var hitResult = Utils.raycastForEntity(level, entity, entity.getEyePosition(), entity.getEyePosition().add(forward.scale(getRange(spellLevel))), true, .15f, Utils::canHitWithRaycast);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            if (target.canBeHitByProjectile()) {
                DamageSources.applyDamage(target, getTickDamage(spellLevel, entity), getElementDamageSource(getCastingElement(entity), entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * 勇者调律：读取施法者当前元素，默认 FIRE
     */
    private TripleMagicMissileProjectile.ElementType getCastingElement(LivingEntity entity) {
        TripleMagicMissileProjectile.ElementType element = TripleMagicMissileProjectile.ElementType.FIRE;
        if (entity.getPersistentData().contains("hero_element")) {
            element = TripleMagicMissileProjectile.ElementType.valueOf(entity.getPersistentData().getString("hero_element"));
        }
        return element;
    }

    /**
     * 勇者调律：按元素返回对应的学派伤害类型（与 TripleMagicMissileProjectile 一致）
     */
    private SpellDamageSource getElementDamageSource(TripleMagicMissileProjectile.ElementType element, LivingEntity attacker) {
        return switch (element) {
            case FIRE -> SpellRegistry.FIREBALL_SPELL.get().getDamageSource(attacker, attacker);
            case LIGHTNING -> SpellRegistry.BALL_LIGHTNING_SPELL.get().getDamageSource(attacker, attacker);
            case BLOOD -> SpellRegistry.BLOOD_SLASH_SPELL.get()
                    .getDamageSource(attacker, attacker)
                    .setLifestealPercent(0f); // 取消猩红伤害自带的吸血
            case HOLY -> SpellRegistry.WISP_SPELL.get().getDamageSource(attacker, attacker);
            case EVOCATION -> SpellRegistry.ARROW_VOLLEY_SPELL.get().getDamageSource(attacker, attacker);
            case NATURE -> SpellRegistry.EARTHQUAKE_SPELL.get().getDamageSource(attacker, attacker);
            case ICE -> SpellRegistry.ICICLE_SPELL.get().getDamageSource(attacker, attacker);
            case ENDER -> SpellRegistry.MAGIC_MISSILE_SPELL.get().getDamageSource(attacker, attacker);
        };
    }

    public static float getRange(int level) {
        return 12;
    }

    private float getTickDamage(int spellLevel, LivingEntity caster) {
        return FastAttributeUtil.getMagicAttack(caster) * 2 * FastAttributeUtil.getBasicMagicDamage(caster);
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > (getRange(spellLevel) * getRange(spellLevel)) * 1.2;
    }
}
