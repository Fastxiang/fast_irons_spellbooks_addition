package com.main.fast_irons_spellbooks_addition.mixin;

import com.main.fast_irons_spellbooks_addition.event.SpellPowerCalculateEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AbstractSpell.class)
public abstract class AbstractSpellMixin {

    @Shadow protected int baseSpellPower;
    @Shadow protected int spellPowerPerLevel;

    @Inject(
        method = "getSpellPower",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void fast_irons_spellbooks_addition$onGetSpellPower(
            int spellLevel,
            @Nullable Entity sourceEntity,
            CallbackInfoReturnable<Float> cir
    ) {
        AbstractSpell spell = (AbstractSpell) (Object) this;

        float originalPower = cir.getReturnValue();

        SpellPowerCalculateEvent event =
                new SpellPowerCalculateEvent(
                        spell,
                        spellLevel,
                        sourceEntity,
                        this.baseSpellPower,
                        this.spellPowerPerLevel,
                        originalPower
                );

        MinecraftForge.EVENT_BUS.post(event);

        cir.setReturnValue(event.getPower());
    }
}
