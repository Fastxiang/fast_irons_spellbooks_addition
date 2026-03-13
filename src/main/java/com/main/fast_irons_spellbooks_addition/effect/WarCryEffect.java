package com.main.fast_irons_spellbooks_addition.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class WarCryEffect extends MobEffect {

    private static final UUID ATTACK_UUID = UUID.fromString("a6a1b9c7-5f2c-4d9b-9f6c-1b2a9d3e8f11");

    public WarCryEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF3C3C); // 红色
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ATTACK_UUID.toString(),
                0.01D,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }
}