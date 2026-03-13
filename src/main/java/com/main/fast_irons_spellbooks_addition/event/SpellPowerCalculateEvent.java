package com.main.fast_irons_spellbooks_addition.event;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

public class SpellPowerCalculateEvent extends Event {

    private final AbstractSpell spell;
    private final int spellLevel;
    @Nullable
    private final Entity sourceEntity;
    
    private final int baseSpellPower;
    private final int spellPowerPerLevel;
    
    private float power;

    public SpellPowerCalculateEvent(
            AbstractSpell spell,
            int spellLevel,
            @Nullable Entity sourceEntity,
            int baseSpellPower,
            int spellPowerPerLevel,
            float power
    ) {
        this.spell = spell;
        this.spellLevel = spellLevel;
        this.sourceEntity = sourceEntity;
        this.baseSpellPower = baseSpellPower;
        this.spellPowerPerLevel = spellPowerPerLevel;
        this.power = power;
    }

    public AbstractSpell getSpell() {
        return spell;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    @Nullable
    public Entity getSourceEntity() {
        return sourceEntity;
    }
    
    public int getBaseSpellPower() {
        return baseSpellPower;
    }
    
    public int getSpellPowerPerLevel() {
        return spellPowerPerLevel;
    }
    
    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }
}
