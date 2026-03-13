package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.main.fast_irons_spellbooks_addition.entity.SkillArrowEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.small_magic_arrow.SmallMagicArrow;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

public class ArrowRainEntity extends AbstractMagicProjectile {
    public ArrowRainEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    int rows;
    int arrowsPerRow;
    int delay = 5;

    @Override
    public void tick() {
        Level level = this.level();
        if (!level.isClientSide) {
            if (tickCount % delay == 0) {
                for (int i = 0; i < arrowsPerRow; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 4.0; // -2 ~ 2
                    double offsetZ = (random.nextDouble() - 0.5) * 4.0; // -2 ~ 2

                    double spawnX = this.getX() + offsetX;
                    double spawnZ = this.getZ() + offsetZ;
                    double spawnY = this.getY() + 12 + random.nextDouble() * 3; // 上方12~15格

                    SkillArrowEntity arrow = new SkillArrowEntity(level, (LivingEntity) this.getOwner());
                    arrow.setBaseDamage(this.getDamage());
                    arrow.setPos(spawnX, spawnY, spawnZ);
                    Vec3 motion = new Vec3(0, -1.5, 0);
                    arrow.shoot(motion.x, motion.y, motion.z, 1.0f, 0.0f);

                    arrow.setOwner(this.getOwner());
                    level.addFreshEntity(arrow);
                    MagicManager.spawnParticles(
                            level,
                            ParticleTypes.CRIT,
                            spawnX,
                            spawnY,
                            spawnZ,
                            2, .1, .1, .1, .05, false
                    );
                }

                level.playSound(
                        null,
                        position().x,
                        position().y,
                        position().z,
                        SoundEvents.ARROW_SHOOT,
                        SoundSource.NEUTRAL,
                        2.0f,
                        0.9f + Utils.random.nextFloat() * 0.2f
                );
            }
            else if (tickCount > rows * delay) {
                discard();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("rows", rows);
        tag.putInt("arrowsPerRow", arrowsPerRow);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rows = tag.getInt("rows");
        this.arrowsPerRow = tag.getInt("arrowsPerRow");
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setArrowsPerRow(int arrowsPerRow) {
        this.arrowsPerRow = arrowsPerRow;
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