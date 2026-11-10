package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackHitEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackMissEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackPreEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
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
import net.minecraftforge.common.MinecraftForge;
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

    /** 起手蓄力时长（0.5 秒），期间不造成任何伤害与普攻事件 */
    private static final int STARTUP_TICKS = 10;

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
        // 起手 0.5 秒内不造成伤害，避免连点器反复起手刷伤害；
        // 之后的间隔由原版 CONTINUOUS_CAST_TICK_INTERVAL（10 tick = 0.5 秒）固定保证
        int castDuration = playerMagicData.getCastDuration();
        if (castDuration > 0 && castDuration - playerMagicData.getCastDurationRemaining() < STARTUP_TICKS) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        Vec3 forward = entity.getForward();
        if (playerMagicData.getAdditionalCastData() instanceof CastingMobAimingData aimData && entity instanceof Mob mob) {
            forward = aimData.getForward(entity);
        }

        TripleMagicMissileProjectile.ElementType element = HeroResonanceSpell.getCurrentElement(entity);

        // 与 TripleMagicMissileSpell 一致：施法前发布普攻前置事件（可被取消或改写伤害/元素）
        BasicAttackPreEvent attackPreEvent = new BasicAttackPreEvent(
                entity,
                1,
                getTickDamage(spellLevel, entity),
                1.0,
                "magic"
        );

        if (!MinecraftForge.EVENT_BUS.post(attackPreEvent)) {
            float damage = (float) (attackPreEvent.getDamage() * attackPreEvent.getDamageMultiplier());
            if (attackPreEvent.getElementType() != null) {
                element = attackPreEvent.getElementType();
            }

            var hitResult = RaycastBuilder.begin(level, entity)
                    .start(entity.getEyePosition())
                    .end(entity.getEyePosition().add(forward.scale(getRange(spellLevel))))
                    .checkForBlocks(true)
                    .bbInflation(.15f)
                    .filter(Utils::canHitWithRaycast)
                    .build();
            boolean hit = false;

            if (hitResult.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hitResult).getEntity();
                if (target instanceof LivingEntity livingTarget && target.canBeHitByProjectile()) {
                    hit = true;
                    BasicAttackHitEvent attackHitEvent = new BasicAttackHitEvent(
                            entity,
                            List.of(livingTarget),
                            damage,
                            1,
                            element,
                            "magic",
                            getElementDamageSource(element, entity)
                    );
                    MinecraftForge.EVENT_BUS.post(attackHitEvent);

                    target.invulnerableTime = 0;
                    DamageSources.applyDamage(target, attackHitEvent.getDamage(), attackHitEvent.getDamageSource());
                    target.invulnerableTime = 20;
                }
            }

            if (!hit) {
                // 与 TripleMagicMissileProjectile.onHitBlock 一致：未命中实体时发布普攻未命中事件
                MinecraftForge.EVENT_BUS.post(new BasicAttackMissEvent(entity, 1, element, "magic"));
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
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
