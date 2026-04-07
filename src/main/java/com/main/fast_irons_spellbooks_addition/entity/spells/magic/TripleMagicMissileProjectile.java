package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.main.fast_irons_spellbooks_addition.event.BasicAttackMissEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackHitEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.damagesource.DamageSource;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TripleMagicMissileProjectile extends AbstractMagicProjectile {

    public TripleMagicMissileProjectile(EntityType<? extends TripleMagicMissileProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public TripleMagicMissileProjectile(EntityType<? extends TripleMagicMissileProjectile> entityType, Level levelIn, LivingEntity shooter) {
        this(entityType, levelIn);
        setOwner(shooter);
    }
    
    public TripleMagicMissileProjectile(Level levelIn, LivingEntity shooter) {
        this(FastEntityRegistry.TRIPLE_MAGIC_MISSILE_PROJECTILE.get(), levelIn, shooter);
    }
    
    public TripleMagicMissileProjectile(Level levelIn, LivingEntity shooter, ElementType elementType) {
        this(FastEntityRegistry.TRIPLE_MAGIC_MISSILE_PROJECTILE.get(), levelIn, shooter);
        setElementProperties(elementType);
    }
    
    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(
                this.level(),
                this.getParticle(),
                x, y, z,
                25,
                0, 0, 0,
                0.18,
                true
        );
    }
    
    private static final EntityDataAccessor<Integer> RED =
            SynchedEntityData.defineId(TripleMagicMissileProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GREEN =
            SynchedEntityData.defineId(TripleMagicMissileProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BLUE =
            SynchedEntityData.defineId(TripleMagicMissileProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARTICLE_ID =
            SynchedEntityData.defineId(TripleMagicMissileProjectile.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RED, 255);
        this.entityData.define(GREEN, 60);
        this.entityData.define(BLUE, 30);
        this.entityData.define(PARTICLE_ID, 0);
    }
    
    private int combo;
    private ElementType elementType = ElementType.FIRE;
    
    private int red = 255;
    private int green = 60;
    private int blue = 30;
    
    private ParticleOptions particle = ParticleHelper.FIRE_EMITTER;
    
    private DamageSource damageSource;
    
    @Override
    public float getSpeed() {
        return 2.5f;
    }
    
    public void setCombo(int combo) {
        this.combo = combo;
    }
    
    public int getCombo() {
        return combo;
    }
    
    public void setElementProperties(ElementType type) {
        this.elementType = type;

        switch (type) {
            case FIRE -> {
                red = 255; green = 60; blue = 30;
                this.damageSource = SpellRegistry.FIREBALL_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case LIGHTNING -> {
                red = 85; green = 255; blue = 255;
                this.damageSource = SpellRegistry.BALL_LIGHTNING_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case BLOOD -> {
                red = 180; green = 20; blue = 20;
                this.damageSource = SpellRegistry.BLOOD_SLASH_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case HOLY -> {
                red = 242; green = 247; blue = 92;
                this.damageSource = SpellRegistry.WISP_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case EVOCATION -> {
                red = 0; green = 170; blue = 170;
                this.damageSource = SpellRegistry.ARROW_VOLLEY_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case NATURE -> {
                red = 20; green = 163; blue = 40;
                this.damageSource = SpellRegistry.EARTHQUAKE_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case ICE -> {
                red = 208; green = 249; blue = 255;
                this.damageSource = SpellRegistry.ICICLE_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
            case ENDER -> {
                red = 255; green = 180; blue = 255;
                this.damageSource = SpellRegistry.MAGIC_MISSILE_SPELL.get()
                        .getDamageSource(this, getOwner());
            }
        }

        this.entityData.set(RED, red);
        this.entityData.set(GREEN, green);
        this.entityData.set(BLUE, blue);
        this.entityData.set(PARTICLE_ID, type.ordinal());
    }

    public ElementType getElementType() {
        return this.elementType;
    }

    public DamageSource getDamageSource() {
        if (this.damageSource == null) {
        this.damageSource = SpellRegistry.FIREBALL_SPELL.get()
                        .getDamageSource(this, getOwner());
        }
        return this.damageSource;
    }
    
    public int getRed() { return this.entityData.get(RED); }
    public int getGreen() { return this.entityData.get(GREEN); }
    public int getBlue() { return this.entityData.get(BLUE); }

    public ParticleOptions getParticle() {
        return switch (this.entityData.get(PARTICLE_ID)) {
            case 1 -> ParticleHelper.ELECTRICITY;
            case 2 -> ParticleHelper.BLOOD;
            case 3 -> ParticleHelper.WISP;
            case 4 -> ParticleTypes.SMOKE;
            case 5 -> ParticleHelper.ACID;
            case 6 -> ParticleHelper.SNOWFLAKE;
            case 7 -> ParticleHelper.UNSTABLE_ENDER;
            default -> ParticleHelper.FIRE_EMITTER;
        };
    }
    
    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BasicAttackMissEvent AttackMissEvent = new BasicAttackMissEvent(getOwner(), this.getCombo(), getElementType(), "magic");
        MinecraftForge.EVENT_BUS.post(AttackMissEvent);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        
        Entity target = result.getEntity();
        
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        BasicAttackHitEvent AttackHitEvent = new BasicAttackHitEvent(
                getOwner(),
                List.of(livingTarget),
                this.getDamage(),
                this.getCombo(),
                getElementType(),
                "magic",
                getDamageSource()
        );

        MinecraftForge.EVENT_BUS.post(AttackHitEvent);

        target.invulnerableTime = 0;
        DamageSources.applyDamage(
                target,
                AttackHitEvent.getDamage(),
                AttackHitEvent.getDamageSource()
        );
        target.invulnerableTime = 20;

        pierceOrDiscard();
    }
    
    @Override
    public void trailParticles() {
        Vec3 vec = getDeltaMovement();
        double length = vec.length();
        int count = (int) Math.min(20, Math.round(length) * 3) + 1;
        float f = (float) length / count;

        for (int i = 0; i < count; i++) {
            Vec3 random = Utils.getRandomVec3(0.02);
            Vec3 p = vec.scale(f * i);
            this.level().addParticle(
                    this.getParticle(),
                    this.getX() + random.x + p.x,
                    this.getY() + random.y + p.y,
                    this.getZ() + random.z + p.z,
                    random.x, random.y, random.z
            );
        }
    }
    
    public enum ElementType {
        FIRE,
        LIGHTNING,
        BLOOD,
        HOLY,
        EVOCATION,
        NATURE,
        ICE,
        ENDER
    }
}
