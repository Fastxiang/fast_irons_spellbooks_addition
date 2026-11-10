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
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ExplosionSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("explosion");

    /**
     * 吟唱音效由 KubeJS 注册（kubejs/assets/kubejs/sounds.json + startup_scripts/sounds/register.js），
     * 这里只按 id 取，不做注册表登记。
     */
    private static final ResourceLocation chantSoundId =
            new ResourceLocation("kubejs", "explosion1");

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

    /**
     * 吟唱音效（照 ISS HeatSurgeSpell / BlackHoleSpell 的写法：起手音 + 取消时停音）
     */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(chantSoundId));
    }

    /**
     * 手动打断（松开右键 / 换法术触发 CancelCastPacket）时让客户端停掉吟唱音
     */
    @Override
    public boolean stopSoundOnCancel() {
        return true;
    }

    /**
     * 吟唱途中的蓄力粒子，配合客户端脚下的魔法阵（{@code ExplosionChantLayer}）。
     * 照基岩版的 {@code explosion_orb_charge}：在施法者周围一个圆柱体里不断冒红色火星并上飘，
     * 每 20 tick 补一圈更规整的环，读起来是「在聚气」。
     * 只在服务端发、走原版粒子包，不需要额外网络代码。
     */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 柱状火星：每 2 tick 一把，随机落在半径 1.1、高度 0.9 的范围里
        if (entity.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                    ParticleHelper.EMBERS,
                    entity.getX(),
                    entity.getY() + 0.9,
                    entity.getZ(),
                    4, 1.1, 0.9, 1.1, 0.01
            );
        }

        // 每 20 tick 一圈更规整的环，给个节拍
        if (entity.tickCount % 20 == 0) {
            final int points = 8;
            for (int i = 0; i < points; i++) {
                double angle = (entity.tickCount / 20.0 + i) * Mth.TWO_PI / points;
                serverLevel.sendParticles(
                        ParticleHelper.EMBERS,
                        entity.getX() + Math.cos(angle) * 2.2,
                        entity.getY() + 0.3,
                        entity.getZ() + Math.sin(angle) * 2.2,
                        1, 0.0, 0.05, 0.0, 0.0
                );
            }
        }
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
        // 和本 mod 其他法术保持一致：每升 1 级 +10% 伤害
        return 30 * FastAttributeUtil.getMagicAttack(entity)
                * (1f + ((spellLevel - 1) * 0.1f))
                * getDamageUp(spellLevel, entity);
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
