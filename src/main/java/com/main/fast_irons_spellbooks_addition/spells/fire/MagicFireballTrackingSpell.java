package com.main.fast_irons_spellbooks_addition.spells.fire;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.fireball.MagicFireballTracking;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.event.MagicFireballTrackingTargetEvent;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

public class MagicFireballTrackingSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("magic_fireball_tracking");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(5)
            .build();

    public MagicFireballTrackingSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 40;
        this.baseManaCost = 225;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        getRadius(spellLevel, caster)
                )
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }


    @Override
    public boolean checkPreCastConditions(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData
    ) {
        Utils.preCastTargetHelper(
                level,
                entity,
                playerMagicData,
                this,
                32,
                .15f,
                false
        );
        return true;
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        var event = new FastSpellOnCastEvent(
                entity,
                spellLevel,
                getSpellId(),
                this
        );

        MinecraftForge.EVENT_BUS.post(event);

        // =========================
        // 和原版火球一样正常发射
        // =========================

        Vec3 origin = entity.getEyePosition();

        MagicFireballTracking fireball =
                new MagicFireballTracking(world, entity);

        fireball.setDamage(getDamage(spellLevel, entity));
        fireball.setExplosionRadius(getRadius(spellLevel, entity));

        fireball.setPos(
                origin.add(entity.getForward())
                        .subtract(0, fireball.getBbHeight() / 2, 0)
        );

        fireball.shoot(entity.getLookAngle());

        // =========================
        // 获取追踪目标
        // =========================

        LivingEntity target = null;

        /*
         * 第一优先级：
         * 使用 Irons Spellbooks 原本的锁定目标
         */
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            if (world instanceof ServerLevel serverLevel) {
                LivingEntity lockedTarget = targetData.getTarget(serverLevel);

                if (lockedTarget != null
                        && lockedTarget.isAlive()
                        && entity.distanceToSqr(lockedTarget) <= 32 * 32) {

                    target = lockedTarget;
                }
            }
        }

        /*
         * 第二优先级：
         * 没有有效锁定目标时，
         * 自动寻找施法者 40 格内最近的目标
         */
        if (target == null) {

            // 自动寻找最近目标
            target = findNearestTarget(world, entity, 40.0);

            // 触发巨型火球追踪目标事件
            MagicFireballTrackingTargetEvent TargetEvent =
                    new MagicFireballTrackingTargetEvent(
                            entity,
                            target,
                            40.0
                    );

            MinecraftForge.EVENT_BUS.post(TargetEvent);

            // 允许其他事件修改最终追踪目标
            target = TargetEvent.getTarget();
        }

        /*
         * 找到目标才开启追踪。
         *
         * 如果 40 格内也没有目标，
         * 就完全按照原版火球的直线方向飞行。
         */
        if (target != null) {
            fireball.setHomingTarget(target);
        }

        world.addFreshEntity(fireball);

        super.onCast(
                world,
                spellLevel,
                entity,
                castSource,
                playerMagicData
        );
    }

    /**
     * 寻找施法者附近最近的可攻击生物。
     */
    private LivingEntity findNearestTarget(
            Level level,
            LivingEntity caster,
            double range
    ) {
        double rangeSqr = range * range;

        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        caster.getBoundingBox().inflate(range),
                        target ->
                                target != caster
                                        && target.isAlive()
                                        && !target.isSpectator()
                                        && target.isPickable()
                ).stream()
                .filter(target ->
                        caster.distanceToSqr(target) <= rangeSqr
                )
                .min((a, b) ->
                        Double.compare(
                                caster.distanceToSqr(a),
                                caster.distanceToSqr(b)
                        )
                )
                .orElse(null);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 5f
                * getSpellPower(spellLevel, caster)
                * (1f + ((spellLevel - 1) * 0.1f));
    }

    public int getRadius(int spellLevel, LivingEntity caster) {
        return 5;
    }
}