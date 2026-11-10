package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 烟火四射-双生 的窗口记账器。
 *
 * <p>施放后 5 秒内，施法者的<b>下一个非唤魔技能</b>冷却 -10 秒。</p>
 *
 * <p>窗口本身是 {@link FastEffectRegistry#TWIN_SOUL} 挂在施法者身上的 Effect，
 * 玩家能在 HUD 上看到剩余时间；这里只负责「判定 + 改写冷却 + 吃掉图标」。</p>
 *
 * <p>减 CD 在 {@link SpellCooldownAddedEvent.Post} 里做：Pre 阶段只是"提出建议值"，
 * 如果这期间还有别的逻辑（属性、其他 mod 的冷却修正、同步包覆盖）动过数值，
 * Pre 的修改会被盖掉。Post 是冷却真正写进 {@link PlayerCooldowns} 之后才触发的，
 * 在这里直接改写存储值并重新同步给客户端，才是最终生效的那一次。</p>
 */
@Mod.EventBusSubscriber(modid = FastIronsSpellbooksAddition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinSoulTracker {

    /** 默认窗口时长：5 秒 */
    public static final int DEFAULT_DURATION = 100;
    /** 冷却缩短（tick）：10 秒 */
    public static final int COOLDOWN_REDUCTION = 200;

    /** 开窗（重复施放会刷新时长，但不会叠加） */
    public static void apply(LivingEntity caster, int durationTicks) {
        caster.addEffect(new MobEffectInstance(
                FastEffectRegistry.TWIN_SOUL.get(),
                durationTicks,
                0,
                false,  // ambient
                false,  // visible：不显示原版的药水漩涡粒子，改用 Effect 自己的 clientTick
                true    // showIcon：HUD 上必须能看见
        ));
    }

    public static boolean isActive(LivingEntity caster) {
        return caster.hasEffect(FastEffectRegistry.TWIN_SOUL.get());
    }

    /** 窗口剩余 tick，没有则 0 */
    public static int getRemainingTicks(LivingEntity caster) {
        MobEffectInstance instance = caster.getEffect(FastEffectRegistry.TWIN_SOUL.get());
        return instance == null ? 0 : Math.max(0, instance.getDuration());
    }

    public static void clear(LivingEntity caster) {
        caster.removeEffect(FastEffectRegistry.TWIN_SOUL.get());
    }

    /**
     * 冷却写入之后改写：直接覆盖 {@link PlayerCooldowns} 里的剩余时长并重新同步客户端，
     * 然后把窗口图标吃掉（用掉了就没了）。
     */
    @SubscribeEvent
    public static void onCooldownAdded(SpellCooldownAddedEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isActive(player)) {
            return;
        }
        AbstractSpell spell = event.getSpell();
        if (spell == null) {
            return;
        }
        // 双生自己不算「下一个技能」
        if (spell.getSpellId().equals(FastSpellRegistry.FIRECRACKER_TWIN_SOUL.get().getSpellId())) {
            return;
        }
        // 只对非唤魔技能生效
        if (spell.getSchoolType() == SchoolRegistry.EVOCATION.get()) {
            return;
        }
        int current = event.getEffectiveCooldown();
        if (current <= 0) {
            // 本来就没有冷却，不算消费
            return;
        }
        int reduced = Math.max(0, current - COOLDOWN_REDUCTION);

        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }
        PlayerCooldowns cooldowns = magicData.getPlayerCooldowns();
        cooldowns.removeCooldown(spell.getSpellId());
        if (reduced > 0) {
            cooldowns.addCooldown(spell, reduced);
        }
        cooldowns.syncToPlayer(player);

        // 用掉窗口：图标消失 = 已经生效过了
        clear(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.4f);
    }
}
