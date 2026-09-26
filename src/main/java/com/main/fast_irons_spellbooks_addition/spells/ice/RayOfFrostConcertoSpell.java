package com.main.fast_irons_spellbooks_addition.spells.ice;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ray_of_frost.RayOfFrostVisualEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

public class RayOfFrostConcertoSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("ray_of_frost_concerto");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(5)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.freeze_time", Utils.timeFromTicks(getFreezeTime(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    public RayOfFrostConcertoSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 200;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.RAY_OF_FROST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var Event = new FastSpellOnCastEvent(entity, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(Event);

        var hitResult = Utils.raycastForEntity(level, entity, getRange(spellLevel, entity), true, .15f);
        level.addFreshEntity(new RayOfFrostVisualEntity(level, entity.getEyePosition(), hitResult.getLocation(), entity));
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hitResult).getEntity();
            //Set freeze time right here because it scales off of level and power
            DamageSources.applyDamage(target, getDamage(spellLevel, entity), getDamageSource(entity).setFreezeTicks(target.getTicksRequiredToFreeze() + getFreezeTime(spellLevel, entity)));
            MagicManager.spawnParticles(level, ParticleHelper.ICY_FOG, hitResult.getLocation().x, target.getY(), hitResult.getLocation().z, 4, 0, 0, 0, .3, true);

            // 协奏效果：命中敌人时在施法者头部附近随机位置生成 2 发三连魔法导弹普攻投射物，射向该敌人，伤害为 100% 魔攻
            spawnConcertoProjectiles(level, entity, target);
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            MagicManager.spawnParticles(level, ParticleHelper.ICY_FOG, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z, 4, 0, 0, 0, .3, true);
        }
        MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z, 50, 0, 0, 0, .3, false);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
    /**
     * 在施法者头部附近随机位置固定生成 2 发 TripleMagicMissileProjectile，朝向目标敌人
     * 不管命中几个敌人，一次施法命中时只生成 2 发（本方法只会在命中时调用一次）
     * 伤害：固定 100% 魔攻（getMagicAttack * getBasicMagicDamage）
     * 不使用 BasicAttackPreEvent（这不属于主动普攻）
     * 应用勇者调律元素：优先读取 caster 的 persistentData "hero_element"
     */
    private void spawnConcertoProjectiles(Level level, LivingEntity caster, Entity target) {
        if (level.isClientSide) {
            return;
        }

        float finalDamage = FastAttributeUtil.getMagicAttack(caster) * FastAttributeUtil.getBasicMagicDamage(caster);
        Vec3 eyePos = caster.getEyePosition();

        // 读取勇者调律的元素选择
        TripleMagicMissileProjectile.ElementType element = TripleMagicMissileProjectile.ElementType.FIRE;
        if (caster.getPersistentData().contains("hero_element")) {
                element = TripleMagicMissileProjectile.ElementType.valueOf(
                        caster.getPersistentData().getString("hero_element")
                );
        }

        // 固定生成 2 发投射物
        int count = 2;
        for (int i = 0; i < count; i++) {
            TripleMagicMissileProjectile projectile = new TripleMagicMissileProjectile(level, caster);
            projectile.setElementProperties(element);
            // 施法者头部附近随机偏移位置
            Vec3 randomOffset = Utils.getRandomVec3(1.2);
            Vec3 spawnPos = eyePos.add(randomOffset);
            projectile.setPos(spawnPos);
            // 朝向被命中敌人中心
            Vec3 shootFrom = spawnPos;
            Vec3 shootTo = target.position().add(0, target.getBoundingBox().getYsize() / 2.0, 0);
            Vec3 shootDir = shootTo.subtract(shootFrom);
            double len = shootDir.length();
            if (len < 0.0001) {
                shootDir = caster.getLookAngle();
            } else {
                shootDir = shootDir.scale(1.0 / len);
            }
            projectile.shoot(shootDir);
            projectile.setCombo(1);
            projectile.setDamage(finalDamage);
            level.addFreshEntity(projectile);
        }
    }

    public static float getRange(int level, LivingEntity caster) {
        return 30;
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 5f * getSpellPower(spellLevel, caster) * (1f + ((spellLevel - 1) * 0.1f));
    }

    private int getFreezeTime(int spellLevel, LivingEntity caster) {
        return 15;
    }
}
