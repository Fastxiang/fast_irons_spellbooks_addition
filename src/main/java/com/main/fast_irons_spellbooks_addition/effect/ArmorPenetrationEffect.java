package com.main.fast_irons_spellbooks_addition.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class ArmorPenetrationEffect extends MobEffect {

    private static final UUID ATTACK_UUID = UUID.fromString("a6a1b9c7-5f2c-4d9b-9f6c-1b2a9d3e8f12");

    public ArmorPenetrationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF3C3C); // 红色
        this.addAttributeModifier(
                Attributes.ARMOR,
                ATTACK_UUID.toString(),
                -0.5D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }
}