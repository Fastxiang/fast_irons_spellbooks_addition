package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile.ElementType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicAttackHitEvent extends Event {

    private final Entity attacker;
    private final List<LivingEntity> targets;
    private float damage;
    private final int combo;

    private final ElementType elementType;

    private final String type;

    private DamageSource damageSource;

    public BasicAttackHitEvent(
            Entity attacker,
            List<LivingEntity> targets,
            float damage,
            int combo,
            ElementType elementType,
            String type,
            DamageSource damageSource
    ) {
        this.attacker = attacker;
        this.targets = targets;
        this.damage = damage;
        this.combo = combo;
        this.elementType = elementType;
        this.type = type;
        this.damageSource = damageSource;
    }

    public Entity getAttacker() {
        return this.attacker;
    }

    public List<LivingEntity> getTargets() {
        return this.targets;
    }

    @Nullable
    public LivingEntity getTarget() {
        return this.targets.size() == 1 ? this.targets.get(0) : null;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public int getCombo() {
        return this.combo;
    }
    
    public ElementType getElementType() {
        return this.elementType;
    }

    public String getType() {
        return this.type;
    }
    
    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    public void setDamageSource(DamageSource damageSource) {
        this.damageSource = damageSource;
    }
}
