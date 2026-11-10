package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.registry.FastAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

public class FastAttributeUtil {

    /** 近战扇形类法术的判定基准半径（技能设计值） */
    public static final double DEFAULT_ATTACK_RANGE = 3.0D;

    /** 女仆实体 id（软依赖：只按注册名比对，不引女仆模组的类） */
    private static final ResourceLocation MAID_ENTITY_ID = new ResourceLocation("touhou_little_maid", "maid");
    /**
     * 女仆的近战扇形补偿。
     *
     * <p>女仆模组给女仆注册的 {@code ENTITY_REACH} 是 <b>2</b>（它自己用来做近战范围判断），
     * 玩家基础是 <b>3</b>。不补的话，同一个技能女仆的扇形比玩家小一圈 —— 判定代码同一份，
     * 缩水的是半径来源。补 1 让两者从同一条起跑线出发。</p>
     */
    private static final double MAID_REACH_BONUS = 1.0D;

    /**
     * 近战扇形 / 横扫类法术的判定半径。
     *
     * <p>取施法者的 {@code ENTITY_REACH}（玩家靠装备堆距离依然生效），
     * 女仆额外 +1 对齐玩家的 3 格基准。</p>
     */
    public static double getAttackRange(LivingEntity entity) {
        if (entity == null) return DEFAULT_ATTACK_RANGE;

        AttributeInstance instance = entity.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (instance == null) return DEFAULT_ATTACK_RANGE;

        double value = instance.getValue();
        if (value <= 0) return DEFAULT_ATTACK_RANGE;

        if (MAID_ENTITY_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            value += MAID_REACH_BONUS;
        }
        return value;
    }
    
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
