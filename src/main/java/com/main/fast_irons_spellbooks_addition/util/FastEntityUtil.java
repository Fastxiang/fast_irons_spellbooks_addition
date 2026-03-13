package com.main.fast_irons_spellbooks_addition.util;


import com.main.fast_irons_spellbooks_addition.entity.SkillArrowEntity;
import net.minecraft.world.entity.projectile.Arrow;

public class FastEntityUtil {
    
    public static boolean isSkillArrow(Arrow arrow) {
        return arrow instanceof SkillArrowEntity;
    }

}
