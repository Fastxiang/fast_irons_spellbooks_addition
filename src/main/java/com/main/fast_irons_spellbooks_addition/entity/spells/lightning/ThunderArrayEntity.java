package com.main.fast_irons_spellbooks_addition.entity.spells.lightning;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 伏特打击-雷阵 的地面持续区域。
 *
 * <p>1.5 秒内落下 6 道雷，每 5 tick 一道，各自结算一次全额伤害。
 * 为了「6 道都能打中同一个怪」，结算前会把目标的无敌帧清零。</p>
 *
 * <p>每次落雷命中至少一个目标时，返还本技能 1 秒冷却（最多 3 秒）——
 * 这是雷系「刷新」身份在第二分支上的体现，不涉及感电体系。</p>
 */
public class ThunderArrayEntity extends AoeEntity {

    /** 落雷次数 */
    private static final int STRIKES = 6;
    /** 落雷间隔（tick） */
    private static final int STRIKE_INTERVAL = 5;
    /** 雷阵持续时间 = 6 道 × 5 tick */
    private static final int DURATION = STRIKES * STRIKE_INTERVAL;
    /** 雷阵半径 */
    private static final float RADIUS = 3f;
    /** 视觉高度 */
    private static final int VFX_HEIGHT = 15;
    /** 每次返还的冷却（tick） */
    private static final int REFUND_PER_STRIKE = 20;
    /** 冷却返还上限（tick） */
    private static final int MAX_REFUND = 60;

    private int refundedTicks;

    public ThunderArrayEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setRadius(RADIUS);
        setCircular();
        setReapplicationDelay(STRIKE_INTERVAL);
        setDuration(DURATION);
    }

    public ThunderArrayEntity(Level level, LivingEntity owner) {
        this(FastEntityRegistry.THUNDER_ARRAY.get(), level);
        setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % STRIKE_INTERVAL == 0 && this.tickCount <= DURATION) {
            spawnStrikeVfx();
        }
    }

    private void spawnStrikeVfx() {
        Vec3 center = this.position();
        // 在半径内随机取一个落点，视觉上更像「阵」
        double angle = this.random.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(this.random.nextDouble()) * RADIUS;
        Vec3 point = center.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);

        int total = 5;
        int light = Utils.random.nextInt(total);
        Vec3 top = point.add(0, VFX_HEIGHT, 0);
        MagicManager.spawnParticles(this.level(), ParticleHelper.FOG_THUNDER_LIGHT, top.x, top.y, top.z, light, 1, 1, 1, 1, true);
        MagicManager.spawnParticles(this.level(), ParticleHelper.FOG_THUNDER_DARK, top.x, top.y, top.z, total - light, 1, 1, 1, 1, true);
        MagicManager.spawnParticles(this.level(), new ZapParticleOption(top), point.x, point.y + 1, point.z, 1, 0, 0, 0, 0, true);
        MagicManager.spawnParticles(this.level(), ParticleHelper.ELECTRIC_SPARKS, point.x, point.y + 0.3, point.z, 20, .2f, .1f, .2f, .25, true);

        this.level().playSound(
                null,
                point.x, point.y, point.z,
                SoundRegistry.SMALL_LIGHTNING_STRIKE.get(),
                SoundSource.PLAYERS,
                2f,
                .8f + this.random.nextFloat() * .5f
        );
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // 5 tick 一道远快于 20 tick 的无敌帧，必须清零才能让 6 道都结算
        target.invulnerableTime = 0;
        DamageSources.applyDamage(
                target,
                this.damage,
                FastSpellRegistry.VOLT_STRIKE_THUNDER_ARRAY.get().getDamageSource(this, getOwner())
        );
    }

    @Override
    public void onPostHit() {
        if (getOwner() instanceof ServerPlayer player) {
            refundCooldown(player);
        }
    }

    private void refundCooldown(ServerPlayer player) {
        if (this.refundedTicks >= MAX_REFUND) {
            return;
        }
        var spell = FastSpellRegistry.VOLT_STRIKE_THUNDER_ARRAY.get();
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }
        var cooldowns = magicData.getPlayerCooldowns();
        var instance = cooldowns.getSpellCooldowns().get(spell.getSpellId());
        if (instance == null) {
            return;
        }
        int amount = Math.min(REFUND_PER_STRIKE, MAX_REFUND - this.refundedTicks);
        cooldowns.decrementCooldown(instance, amount);
        this.refundedTicks += amount;
        cooldowns.syncToPlayer(player);
    }

    @Override
    public float getParticleCount() {
        return 0;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }
}
