package com.main.fast_irons_spellbooks_addition.mixin;

import com.main.fast_irons_spellbooks_addition.event.SpellCastCheckEvent;
import com.main.fast_irons_spellbooks_addition.event.SpellPowerCalculateEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

    @Inject(
        method = "canBeCastedBy",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void fast_irons_spellbooks_addition$onCanBeCastedBy(
            int spellLevel,
            CastSource castSource,
            MagicData playerMagicData,
            Player player,
            CallbackInfoReturnable<CastResult> cir
    ) {
        AbstractSpell spell = (AbstractSpell) (Object) this;

        boolean originalAllowed = cir.getReturnValue().isSuccess();

        boolean isSpellOnCooldown = playerMagicData.getPlayerCooldowns().isOnCooldown(spell);
        boolean hasRecastForSpell = playerMagicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId());

        SpellCastCheckEvent event =
                new SpellCastCheckEvent(
                        spell,
                        playerMagicData,
                        player,
                        spell.getManaCost(spellLevel),
                        isSpellOnCooldown,
                        hasRecastForSpell,
                        originalAllowed
                );

        MinecraftForge.EVENT_BUS.post(event);

        if (event.isAllow()) {
            cir.setReturnValue(new CastResult(CastResult.Type.SUCCESS));
        } else if (event.getErrorMessage() != null) {
            // 自定义失败消息
            cir.setReturnValue(new CastResult(CastResult.Type.FAILURE, event.getErrorMessage()));
        } else if (originalAllowed) {
            cir.setReturnValue(new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable("ui.irons_spellbooks.cast_error_mana", spell.getDisplayName(player)).withStyle(ChatFormatting.RED)
            ));
        } else {
            // 原版本身就失败，保持原来的失败结果
            // 不修改返回值
        }
    }
}
