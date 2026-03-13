package com.main.fast_irons_spellbooks_addition.event;

import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile.ElementType;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

public class BasicAttackMissEvent extends Event {

    private final Entity attacker;
    private final int combo;
    private final ElementType elementType;
    private final String type;

    public BasicAttackMissEvent(
            Entity attacker,
            int combo,
            ElementType elementType,
            String type
    ) {
        this.attacker = attacker;
        this.combo = combo;
        this.elementType = elementType;
        this.type = type;
    }

    public Entity getAttacker() {
        return this.attacker;
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
}