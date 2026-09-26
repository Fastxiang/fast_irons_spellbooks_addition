package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 血雨实体：在指定位置上空持续生成血针从天而降
 * - 持续时间：60 秒（1200 tick）
 * - 范围：20 格半径
 * - 每隔一定间隔生成一批血针
 */
public class BloodRainEntity extends AbstractMagicProjectile {
    public BloodRainEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    /** 总持续时长（tick）= 60 秒 */
    private static final int DURATION = 20 * 60;
    /** 每多少 tick 生成一波血针 */
    private static final int WAVE_DELAY = 5;
    /** 每波生成的血针总数（均匀分布的外围雨） */
    private static final int NEEDLES_PER_WAVE = 26;
    /** 每波在中心区域额外生成的血针数（提高中心命中率） */
    private static final int CENTER_NEEDLES_PER_WAVE = 6;
    /** 血雨范围（半径，方块） */
    private static final float RADIUS = 20f;
    /** 中心密集区半径（方块） */
    private static final float CENTER_RADIUS = 4f;
    /** 血针生成高度（中心点上方） */
    private static final float SPAWN_HEIGHT = 14f;

    @Override
    public void tick() {
        Level level = this.level();
        if (!level.isClientSide) {
            // 持续时间内生成血针
            if (tickCount % WAVE_DELAY == 0) {
                Entity owner = this.getOwner();
                LivingEntity shooter = (owner instanceof LivingEntity living) ? living : null;
                if (shooter == null) {
                    this.discard();
                    return;
                }

                // 外围雨：均匀分布在 RADIUS 半径内（雨的感觉）
                for (int i = 0; i < NEEDLES_PER_WAVE; i++) {
                    spawnNeedle(level, shooter, RADIUS, true);
                }
                // 中心区：集中在 CENTER_RADIUS 半径内（提高中心命中率）
                for (int i = 0; i < CENTER_NEEDLES_PER_WAVE; i++) {
                    spawnNeedle(level, shooter, CENTER_RADIUS, false);
                }

                // 血雨的环境音效
                if (tickCount % 20 == 0) {
                    level.playSound(
                            null,
                            position().x, position().y, position().z,
                            SoundEvents.GENERIC_SPLASH,
                            SoundSource.NEUTRAL,
                            1.5f,
                            0.8f + Utils.random.nextFloat() * 0.3f
                    );
                }
            }

            // 持续时间到达后销毁
            if (tickCount > DURATION) {
                discard();
            }
        }
    }

    /**
     * 生成一根从天而降的血针
     *
     * @param radius   生成半径
     * @param uniform  true=均匀面积分布（雨感）；false=中心密集分布（高命中）
     */
    private void spawnNeedle(Level level, LivingEntity shooter, float radius, boolean uniform) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        // uniform: sqrt(random) 均匀面积分布；中心密集: 直接 random 让点聚集在中心
        double r = uniform ? Math.sqrt(random.nextDouble()) * radius : random.nextDouble() * radius;
        double offsetX = Math.cos(angle) * r;
        double offsetZ = Math.sin(angle) * r;

        double spawnX = this.getX() + offsetX;
        double spawnZ = this.getZ() + offsetZ;
        double spawnY = this.getY() + SPAWN_HEIGHT + random.nextDouble() * 3.0;

        BloodNeedle needle = new BloodNeedle(level, shooter);
        needle.setDamage(this.getDamage());
        needle.setScale(0.4f);
        needle.setPos(spawnX, spawnY, spawnZ);
        // 朝下射出
        Vec3 motion = new Vec3(
                (random.nextDouble() - 0.5) * 0.2,
                -1.2,
                (random.nextDouble() - 0.5) * 0.2
        );
        needle.shoot(motion);
        needle.setOwner(this.getOwner());
        level.addFreshEntity(needle);

        MagicManager.spawnParticles(
                level,
                ParticleTypes.CRIMSON_SPORE,
                spawnX, spawnY, spawnZ,
                3, .1, .1, .1, .05, false
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public void trailParticles() {
    }

    @Override
    public void impactParticles(double x, double y, double z) {
    }

    @Override
    public float getSpeed() {
        return 0;
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }
}
