package com.main.fast_irons_spellbooks_addition.spells;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;

public class PhysicalSpellAnimations {
    
    public static final ResourceLocation ANIMATION_RESOURCE =
            FastIronsSpellbooksAddition.id("animation");
            
    public static final AnimationHolder ATTACK_1 =
            new AnimationHolder(id("attack1"), true);
            
    public static final AnimationHolder ATTACK_2 =
            new AnimationHolder(id("attack2"), true);
            
    public static final AnimationHolder ATTACK_3 =
            new AnimationHolder(id("attack3"), true);
            
    public static final AnimationHolder ATTACK_4 =
            new AnimationHolder(id("attack4"), true);

    public static final AnimationHolder COUNTER_STANCE =
            new AnimationHolder(id("counter_stance"), true);

    public static final AnimationHolder COUNTER_1 =
            new AnimationHolder(id("counter_1"), true);

    public static final AnimationHolder CHARGE_SLASH =
            new AnimationHolder(id("charge_slash"), true);

    public static final AnimationHolder RAGING_SLASH =
            new AnimationHolder(id("raging_slash"), true);

    public static final AnimationHolder CRUSHING_STANCE =
            new AnimationHolder(id("crushing_stance"), true);

    public static final AnimationHolder PERFECT_GUARD =
            new AnimationHolder(id("perfect_guard"), true);

    private static ResourceLocation id(String name) {
        return FastIronsSpellbooksAddition.id(name);
    }
}
