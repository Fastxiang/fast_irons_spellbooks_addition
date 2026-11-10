package com.main.fast_irons_spellbooks_addition.mixin;

import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import com.main.fast_irons_spellbooks_addition.event.SpellCastCheckEvent;
import com.main.fast_irons_spellbooks_addition.event.SpellPowerCalculateEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    /**
     * 把 {@link PhantomCantor} 打出的法术伤害<b>在源头</b>就归属给它的主人。
     *
     * <p>随从是以自己为施法者施法的（投射物才会从它身上、朝它瞄准的方向飞出去），
     * 所以 {@code getDamageSource} 拿到的 attacker 是随从。这里把 causing entity 换成主人，
     * 于是击杀归属、吸血、以及所有「造成魔法伤害时」类饰品都按主人结算，
     * 而 {@code directEntity}（投射物）保持不变，击退 / 免击退 / 命中特效都不受影响。</p>
     *
     * <p>改了这里之后 {@code SpellDamageEvent} 只会 post 一次，归属就是玩家，
     * 不存在"取消再重放"的副作用；{@link com.main.fast_irons_spellbooks_addition.event.CantorDamageRedirect} 作为兜底保留。</p>
     */
    @Inject(
        method = "getDamageSource(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lio/redspace/ironsspellbooks/damage/SpellDamageSource;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private void fast_irons_spellbooks_addition$redirectCantorDamage(
            Entity projectile,
            Entity attacker,
            CallbackInfoReturnable<SpellDamageSource> cir
    ) {
        if (!(attacker instanceof PhantomCantor cantor)) {
            return;
        }
        LivingEntity owner = cantor.getOwnerEntity();
        if (owner == null || owner == cantor || !owner.isAlive() || owner.isSpectator()) {
            return;
        }
        AbstractSpell spell = (AbstractSpell) (Object) this;
        Entity direct = projectile == null ? owner : projectile;
        cir.setReturnValue(SpellDamageSource.source(direct, owner, spell));
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
