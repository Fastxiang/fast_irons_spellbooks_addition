package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 【大剑武装】让玩家进入"持大剑"的状态，状态期间左键由 {@link GreatswordSlashSpell} 接管。
 *
 * <h2>状态载体是 spell 的 recast，不是 MobEffect</h2>
 * <p>和原版的召唤马（{@code SummonHorseSpell}）完全同一套做法：施法时
 * {@code PlayerRecasts.addRecast(new RecastInstance(spellId, level, n, duration, source, null))}，
 * 这个 recast 的 {@code remainingTicks} 由 {@code MagicManager} 每 2 tick 递减，
 * 到 0 时 ISS 自动移除并回调 {@code onRecastFinished}（父类实现里会补上冷却）。</p>
 *
 * <p>所以"还剩多久"就是 {@code RecastInstance.getTicksRemaining()}，
 * 判断"有没有状态"就是 {@code PlayerRecasts.hasRecastForSpell(spell)}——
 * 服务端和客户端（{@code ClientMagicData.getRecasts()}）读到的是同一份同步数据，
 * Java 与 KubeJS 都能直接读，不需要额外同步通道。</p>
 *
 * <h2>为什么不用 MobEffect</h2>
 * <p>Effect 只是"一个倒计时数字"，读完还得自己解释"这是大剑武装"；
 * 而 recast 天然带着 spell id 与 spell level，等于"玩家身上记着这个法术的状态"，
 * 和召唤物身上记着 spellId/spellLevel 是同一个思路。</p>
 *
 * <h2>重复施放</h2>
 * <p>状态还在时再放一次只刷新持续时间，不叠层。首次走 {@code addRecast(totalRecasts=2)}，
 * 之后走 {@code forceAddRecast(totalRecasts=3)} —— 因为 {@code AbstractSpell.castSpell}
 * 在"施法前已有 recast"时会自动 {@code decrementRecastCount} 一次，扣完正好留 1 次，
 * 状态继续且 {@code remainingTicks} 被重置为满值。</p>
 */
public class GreatswordArmamentSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("greatsword_armament");

    /** 持续时间（秒），固定值，不随等级变化 */
    private static final int DURATION_SECONDS = 60;

    /**
     * 首次施放的 totalRecasts。构造时 {@code remainingRecasts = totalRecasts - 1}，
     * 所以必须是 2 才能让 {@code isRecastActive} 成立（要求 &gt; 0）。
     */
    private static final int RECAST_TOTAL_FIRST = 2;
    /** 刷新时用 3：castSpell 收尾会扣掉 1，扣完仍是 1 */
    private static final int RECAST_TOTAL_REFRESH = 3;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(5)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public GreatswordArmamentSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 10;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    public static ResourceLocation getSpellResourceId() {
        return spellId;
    }

    public static int getDurationTicks(int spellLevel) {
        return DURATION_SECONDS * 20;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    /** 起手：双手把剑举到身前，ISS 自带的 self_cast_two_hands */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_TWO_HANDS;
    }

    /** 结束立刻收势，交回给常驻的持剑待机层 */
    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return RECAST_TOTAL_FIRST;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_duration",
                        Utils.timeFromTicks(getDurationTicks(spellLevel), 1))
        );
    }

    @Override
    public void onCast(
            Level world,
            int spellLevel,
            LivingEntity caster,
            CastSource castSource,
            MagicData magicData
    ) {
        var event = new FastSpellOnCastEvent(caster, spellLevel, getSpellId(), this);
        MinecraftForge.EVENT_BUS.post(event);

        if (!world.isClientSide && caster != null) {
            PlayerRecasts recasts = magicData.getPlayerRecasts();
            int duration = getDurationTicks(spellLevel);

            if (recasts.hasRecastForSpell(this)) {
                // 状态还在：重置计时。用 3 是因为 castSpell 收尾必定再扣一次
                recasts.forceAddRecast(new RecastInstance(
                        getSpellId(), spellLevel, RECAST_TOTAL_REFRESH, duration, castSource, null));
            } else {
                recasts.addRecast(new RecastInstance(
                        getSpellId(), spellLevel, RECAST_TOTAL_FIRST, duration, castSource, null), magicData);
            }

            world.playSound(null, caster.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.4f);
        }

        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    /**
     * 状态自然到期（或被打断）时走这里。
     *
     * <p>父类实现会给这个法术补上冷却，所以冷却实际是从"武装结束"开始算的，
     * 而不是从施放瞬间算——这正是想要的手感。</p>
     */
    @Override
    public void onRecastFinished(
            net.minecraft.server.level.ServerPlayer serverPlayer,
            RecastInstance recastInstance,
            io.redspace.ironsspellbooks.capabilities.magic.RecastResult recastResult,
            io.redspace.ironsspellbooks.api.spells.ICastDataSerializable castDataSerializable
    ) {
        if (serverPlayer != null) {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.7f, 0.8f);
        }
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }
}
