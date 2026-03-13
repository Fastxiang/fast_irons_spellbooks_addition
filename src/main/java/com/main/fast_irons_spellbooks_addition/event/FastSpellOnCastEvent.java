package com.main.fast_irons_spellbooks_addition.event;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class FastSpellOnCastEvent extends Event {

    private final Entity caster;
    private final int spellLevel;
    private final String spellId;
    private final AbstractSpell spell;

    public FastSpellOnCastEvent(
            Entity caster,
            int spellLevel,
            String spellId,
            AbstractSpell spell
    ) {
        this.caster = caster;
        this.spellLevel = spellLevel;
        this.spellId = spellId;
        this.spell = spell;
    }

    public Entity getCaster() {
        return this.caster;
    }

    public int getSpellLevel() { return this.spellLevel; }

    public String getSpellId() {
        return this.spellId;
    }

    public AbstractSpell getSpell() {
        return this.spell;
    }
}
