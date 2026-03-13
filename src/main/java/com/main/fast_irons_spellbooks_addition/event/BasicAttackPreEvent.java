package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile.ElementType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

@Cancelable
public class BasicAttackPreEvent extends Event {

    private final Entity attacker;
    private final int combo;

    private float damage;
    private double damageMultiplier;
    private final String type;

    @Nullable
    private ElementType elementType;

    public BasicAttackPreEvent(
            Entity attacker,
            int combo,
            float damage,
            double damageMultiplier,
            String type
    ) {
        this.attacker = attacker;
        this.combo = combo;
        this.damage = damage;
        this.damageMultiplier = damageMultiplier;
        this.elementType = null;
        this.type = type;
    }

    public Entity getAttacker() {
        return this.attacker;
    }

    public int getCombo() {
        return this.combo;
    }

    public float getDamage() {
        return this.damage;
    }

    public double getDamageMultiplier() {
        return this.damageMultiplier;
    }

    @Nullable
    public ElementType getElementType() {
        return this.elementType;
    }
    
    public String getType() {
        return this.type;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }
}
