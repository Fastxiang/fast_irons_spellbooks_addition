package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 爆裂魔法的命中特效实体：只负责「看」，没有任何伤害/破坏逻辑（那部分仍在 {@link ExplosionProjectile#onHit}）。
 * <p>
 * 几何部分（爆闪 / 星芒 / 冲击波 / 火柱）由 {@code ExplosionBurstRenderer} 按 tickCount 推进；
 * 蘑菇云在这里用粒子堆——{@code FogParticle} 单个能活 60~120 tick 并持续膨胀，正好当烟团。
 * 粒子只在本机生成（{@code level.addParticle}），不走包。
 */
public class ExplosionBurst extends Entity {

    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(ExplosionBurst.class, EntityDataSerializers.FLOAT);

    /** 特效总时长（tick）：爆闪 9、星芒 18、火柱 26、冲击波 34、烟柱和蘑菇云一直放到 60 */
    public static final int LIFE = 60;

    /** 蘑菇柱体：深灰烟，单个够大才像一团而不是一粒 */
    private static final ParticleOptions STEM_SMOKE =
            new FogParticleOptions(new Vector3f(0.30F, 0.27F, 0.25F), 2.2F);
    /** 蘑菇帽：更宽更厚 */
    private static final ParticleOptions CAP_SMOKE =
            new FogParticleOptions(new Vector3f(0.34F, 0.30F, 0.27F), 3.6F);
    /** 帽底被火烤亮的部分 */
    private static final ParticleOptions CAP_GLOW =
            new FogParticleOptions(new Vector3f(1.0F, 0.42F, 0.10F), 2.8F);
    /** 贴地扬尘 */
    private static final ParticleOptions GROUND_DUST =
            new FogParticleOptions(new Vector3f(0.52F, 0.45F, 0.38F), 2.4F);

    public ExplosionBurst(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ExplosionBurst(Level level, Vec3 pos, float radius) {
        this(FastEntityRegistry.EXPLOSION_BURST.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.setRadius(radius);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 8.0F);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(DATA_RADIUS, radius);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount > LIFE) {
                this.discard();
            }
            return;
        }

        spawnParticles();
    }

    /**
     * 蘑菇云配方：
     * <ol>
     *   <li>核心火球：向外抛火和火星</li>
     *   <li>柱身：从地面往上冒的烟，半径随高度略收</li>
     *   <li>帽：在顶部一圈环形摊开，外沿微微下卷；帽底补一层橙色，做出「被火烤亮的下沿」</li>
     *   <li>地面尘环：贴地向外推</li>
     * </ol>
     */
    private void spawnParticles() {
        float radius = getRadius();
        Level level = this.level();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        int t = this.tickCount;

        float capHeight = radius * 1.15F;
        float capRadius = radius * 0.72F;
        // 帽的张开进度：8 tick 开始，40 tick 张到位
        float open = Mth.clamp((t - 8) / 32.0F, 0.0F, 1.0F);

        // ① 核心火球：0~12 tick
        if (t <= 12) {
            int count = (int) Mth.clamp(radius * 0.45F, 6, 18);
            for (int i = 0; i < count; i++) {
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                double dist = this.random.nextDouble() * radius * 0.30F;
                double vx = Math.cos(ang) * (0.35 + this.random.nextDouble() * 0.6);
                double vz = Math.sin(ang) * (0.35 + this.random.nextDouble() * 0.6);
                double vy = 0.2 + this.random.nextDouble() * 0.5;
                level.addParticle(ParticleHelper.EMBERS,
                        x + Math.cos(ang) * dist, y + 0.8, z + Math.sin(ang) * dist, vx, vy, vz);
                if (i % 2 == 0) {
                    level.addParticle(ParticleHelper.FIRE,
                            x + Math.cos(ang) * dist * 0.6, y + 0.6, z + Math.sin(ang) * dist * 0.6,
                            vx * 0.5, vy * 1.1, vz * 0.5);
                }
            }
        }

        // ② 柱身：4~52 tick，贴着中轴往上补烟
        if (t >= 4 && t <= 52) {
            for (int i = 0; i < 4; i++) {
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                // 指数偏置：靠近地面的烟更密，柱身才立得住
                double h = Math.pow(this.random.nextDouble(), 1.4) * capHeight * Math.max(0.35F, open);
                // 柱身要粗一点，太细会像路灯；越往上略收
                double r = radius * (0.16 - 0.05 * (h / Math.max(1.0F, capHeight)));
                level.addParticle(STEM_SMOKE,
                        x + Math.cos(ang) * r, y + 1.5 + h, z + Math.sin(ang) * r,
                        Math.cos(ang) * 0.02, 0.12, Math.sin(ang) * 0.02);
            }
        }

        // ③ 蘑菇帽：8~56 tick，环形摊开
        if (t >= 8 && t <= 56) {
            int count = (int) Mth.clamp(radius * 0.18F, 3, 7);
            for (int i = 0; i < count; i++) {
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                double spread = 0.30F + 0.70F * open;
                double ringR = capRadius * spread * (0.85 + this.random.nextDouble() * 0.3);
                // 中间高、外沿低：这样帽是个穹顶，而不是一张煎饼
                double norm = Math.min(1.0, ringR / Math.max(0.001, capRadius * spread));
                double hy = y + capHeight * (1.06 - 0.35 * norm) + (this.random.nextDouble() - 0.5) * capHeight * 0.10;
                double vx = Math.cos(ang) * 0.11;
                double vz = Math.sin(ang) * 0.11;
                level.addParticle(CAP_SMOKE,
                        x + Math.cos(ang) * ringR, hy, z + Math.sin(ang) * ringR,
                        vx, 0.05 + this.random.nextDouble() * 0.04, vz);
                // 帽底的下沿：橙色，向外卷一点
                if (i % 2 == 0) {
                    level.addParticle(CAP_GLOW,
                            x + Math.cos(ang) * ringR * 0.82, hy - capHeight * 0.10, z + Math.sin(ang) * ringR * 0.82,
                            vx * 0.7, -0.01, vz * 0.7);
                }
                // 帽顶再补一层，把「煎饼」堆成穹顶
                if (i % 2 == 0) {
                    level.addParticle(CAP_SMOKE,
                            x + Math.cos(ang) * ringR * 0.55, hy + capHeight * 0.16, z + Math.sin(ang) * ringR * 0.55,
                            vx * 0.4, 0.08, vz * 0.4);
                }
                // 最外圈往下塌的那一下，让帽沿有厚度
                if (i % 3 == 0) {
                    level.addParticle(CAP_SMOKE,
                            x + Math.cos(ang) * ringR * 1.12, hy - capHeight * 0.14, z + Math.sin(ang) * ringR * 1.12,
                            vx * 0.5, -0.05, vz * 0.5);
                }
            }
        }

        // ④ 地面尘环：4~44 tick，贴地向外推
        if (t >= 4 && t <= 44) {
            float push = Mth.clamp((t - 4) / 40.0F, 0.0F, 1.0F);
            for (int i = 0; i < 3; i++) {
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                double ringR = radius * (0.25F + 0.95F * push) * (0.85 + this.random.nextDouble() * 0.3);
                level.addParticle(GROUND_DUST,
                        x + Math.cos(ang) * ringR, y + 0.5 + this.random.nextDouble() * 0.8, z + Math.sin(ang) * ringR,
                        Math.cos(ang) * 0.3, 0.03, Math.sin(ang) * 0.3);
            }
        }

        // ⑤ 余焰：20~60 tick，地面残火
        if (t > 20) {
            int count = (int) Mth.clamp(radius * 0.12F, 2, 5);
            for (int i = 0; i < count; i++) {
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                double dist = this.random.nextDouble() * radius * 0.85F;
                level.addParticle(ParticleHelper.EMBERS,
                        x + Math.cos(ang) * dist, y + 0.4, z + Math.sin(ang) * dist,
                        0.0, 0.12 + this.random.nextDouble() * 0.2, 0.0);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setRadius(tag.getFloat("Radius"));
        this.tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", this.getRadius());
        tag.putInt("Age", this.tickCount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
