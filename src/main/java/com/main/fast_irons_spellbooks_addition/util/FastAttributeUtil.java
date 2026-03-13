package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.registry.FastAttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FastAttributeUtil {
    
    public static float getMagicAttack(LivingEntity entity) {
        if (entity == null) return 0F;

        AttributeInstance instance =
                entity.getAttribute(FastAttributeRegistry.MAGIC_ATTACK.get());

        return instance != null ? (float) instance.getValue() : 0F;
    }
    
    public static float getPhysicalAttack(LivingEntity entity) {
        if (entity == null) return 0F;

        AttributeInstance instance =
                entity.getAttribute(Attributes.ATTACK_DAMAGE);

        return instance != null ? (float) instance.getValue() : 0F;
    }
    
    public static float getBasicPhysicalDamage(LivingEntity entity) {
        if (entity == null) return 1F;
        AttributeInstance instance = entity.getAttribute(FastAttributeRegistry.BASIC_PHYSICAL_DAMAGE.get());
        return instance != null ? (float) instance.getValue() : 1F;
    }
    
    public static float getBasicMagicDamage(LivingEntity entity) {
        if (entity == null) return 1F;
        AttributeInstance instance = entity.getAttribute(FastAttributeRegistry.BASIC_MAGIC_DAMAGE.get());
        return instance != null ? (float) instance.getValue() : 1F;
    }
}
