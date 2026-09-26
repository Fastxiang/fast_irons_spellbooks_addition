package com.main.fast_irons_spellbooks_addition.event;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

public class SpellCastCheckEvent extends Event {

    private final AbstractSpell spell;
    private final MagicData playerMagicData;
    private final Player player;

    private int manaCost;

    private final boolean isSpellOnCooldown;
    private final boolean hasRecastForSpell;

    private final boolean originalAllowed;
    private boolean allow;
    @Nullable
    private Component errorMessage;

    public SpellCastCheckEvent(
            AbstractSpell spell,
            MagicData playerMagicData,
            Player player,
            int manaCost,
            boolean isSpellOnCooldown,
            boolean hasRecastForSpell,
            boolean originalAllowed
    ) {
        this.spell = spell;
        this.playerMagicData = playerMagicData;
        this.player = player;
        this.manaCost = manaCost;
        this.isSpellOnCooldown = isSpellOnCooldown;
        this.hasRecastForSpell = hasRecastForSpell;
        this.originalAllowed = originalAllowed;
        this.allow = originalAllowed;
    }

    public AbstractSpell getSpell() {
        return spell;
    }

    public MagicData getPlayerMagicData() {
        return playerMagicData;
    }

    public Player getPlayer() {
        return player;
    }

    public int getManaCost() {
        return manaCost;
    }

    /**
     * 修改本次施放的实际蓝耗，供后续监听者通过 getManaCost() 获取最新值。不影响 allow 结果
     */
    public void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public boolean isSpellOnCooldown() {
        return isSpellOnCooldown;
    }

    public boolean hasRecastForSpell() {
        return hasRecastForSpell;
    }

    /**
     * 原版逻辑是否允许施放
     */
    public boolean isOriginalAllowed() {
        return originalAllowed;
    }

    /**
     * 新的允许结果。初始为 originalAllowed
     */
    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
        this.errorMessage = null;
    }

    /**
     * 设置是否允许施放并附带自定义失败消息。只有 allow 为 false 时 errorMessage 才生效
     */
    public void setAllow(boolean allow, @Nullable Component errorMessage) {
        this.allow = allow;
        this.errorMessage = errorMessage;
    }

    /**
     * 自定义失败消息，仅当 isAllow() 为 false 时使用
     */
    @Nullable
    public Component getErrorMessage() {
        return errorMessage;
    }
}