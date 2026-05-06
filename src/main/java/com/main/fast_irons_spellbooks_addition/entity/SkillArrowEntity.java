package com.main.fast_irons_spellbooks_addition.entity;

import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class SkillArrowEntity extends Arrow {

    private int life = 0;

    public SkillArrowEntity(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public SkillArrowEntity(Level level, LivingEntity shooter) {
        this(FastEntityRegistry.SKILL_ARROW.get(), level);
        this.setOwner(shooter);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            super.onHitEntity(hitResult);
            living.invulnerableTime = 20;
        } else {
            super.onHitEntity(hitResult);
        }
    }

    @Override
    protected void tickDespawn() {
        ++this.life;
        if (this.life >= 60) {
            this.discard();
        }
    }
}
