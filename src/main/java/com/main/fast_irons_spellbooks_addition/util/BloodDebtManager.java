package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.effect.BloodDebtEffect;
import com.main.fast_irons_spellbooks_addition.event.BloodDebtSettleEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 猩红刺狱-血债 的记账器。
 *
 * <p>时长与可视由目标身上的 {@link BloodDebtEffect} 承载（玩家能在 HUD 看到倒计时），
 * 这里只存 Effect 存不下的东西：<b>施法者</b> 与 <b>累计伤害</b>。</p>
 *
 * <ol>
 *   <li>挂债：{@link #apply} 只记施法者与比例，<b>已有债时不覆盖</b></li>
 *   <li>记账：监听 {@link SpellDamageEvent}，只统计「猩红伤害」，按比例累加</li>
 *   <li>结算：Effect 到期时由 {@link BloodDebtEffect#onEffectRemoved} 回调 {@link #settle}</li>
 * </ol>
 *
 * <p>「吸血加速」：施法者每次被治疗，自己名下所有血债各缩短 1 秒
 * （通过摘掉 Effect 再按缩短后的时长挂回去实现，HUD 上看到的时长始终是真实的）。</p>
 */
@Mod.EventBusSubscriber(modid = FastIronsSpellbooksAddition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodDebtManager {

    /** 吸血加速：每次治疗缩短的时长 */
    private static final int HEAL_ACCELERATE_TICKS = 20;
    /** 吸血加速的最小间隔 */
    private static final int HEAL_ACCELERATE_COOLDOWN = 10;

    private static final Map<UUID, Debt> DEBTS = new HashMap<>();
    /** 摘掉 Effect 重挂期间，抑制一次结算回调 */
    private static final Set<UUID> SUPPRESS_SETTLE = new HashSet<>();

    public static class Debt {
        final UUID casterUUID;
        final float ratio;
        float accumulated;
        long lastAccelerateTick;

        Debt(UUID casterUUID, float ratio) {
            this.casterUUID = casterUUID;
            this.ratio = ratio;
        }
    }

    private static MobEffect effect() {
        return FastEffectRegistry.BLOOD_DEBT.get();
    }

    /**
     * 记一笔债。已经有债的目标不会被覆盖（时长、比例都保持原样）。
     *
     * @return true 表示这是一笔新债
     */
    public static boolean apply(LivingEntity caster, LivingEntity target, float ratio) {
        if (DEBTS.containsKey(target.getUUID())) {
            return false;
        }
        DEBTS.put(target.getUUID(), new Debt(caster.getUUID(), ratio));
        return true;
    }

    public static boolean hasDebt(LivingEntity target) {
        return DEBTS.containsKey(target.getUUID());
    }

    /** 供 KubeJS 查询当前累计量 */
    public static float getAccumulated(LivingEntity target) {
        Debt debt = DEBTS.get(target.getUUID());
        return debt == null ? 0 : debt.accumulated;
    }

    /** 供 KubeJS 查询记录比例 */
    public static float getRatio(LivingEntity target) {
        Debt debt = DEBTS.get(target.getUUID());
        return debt == null ? 0 : debt.ratio;
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (DEBTS.isEmpty()) {
            return;
        }
        LivingEntity target = event.getEntity();
        Debt debt = DEBTS.get(target.getUUID());
        if (debt == null) {
            return;
        }
        Entity attacker = event.getSpellDamageSource().getEntity();
        if (attacker == null || !attacker.getUUID().equals(debt.casterUUID)) {
            return;
        }
        // 只记猩红伤害
        if (!event.getSpellDamageSource().is(ISSDamageTypes.BLOOD_MAGIC)) {
            return;
        }
        float gained = event.getAmount() * debt.ratio;
        if (gained <= 0) {
            return;
        }
        debt.accumulated += gained;
        MagicManager.spawnParticles(
                target.level(),
                ParticleHelper.BLOOD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                2, .3, .3, .3, .01, false
        );
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (DEBTS.isEmpty()) {
            return;
        }
        LivingEntity healer = event.getEntity();
        long now = healer.level().getGameTime();
        for (Map.Entry<UUID, Debt> entry : DEBTS.entrySet()) {
            Debt debt = entry.getValue();
            if (!debt.casterUUID.equals(healer.getUUID())) {
                continue;
            }
            if (now - debt.lastAccelerateTick < HEAL_ACCELERATE_COOLDOWN) {
                continue;
            }
            LivingEntity target = resolveTarget(healer, entry.getKey());
            if (target == null) {
                continue;
            }
            debt.lastAccelerateTick = now;
            accelerate(target, HEAL_ACCELERATE_TICKS);
        }
    }

    /**
     * 把目标身上的血债缩短指定时长。
     *
     * <p>摘掉再挂回，是为了让 HUD 上的倒计时与真实剩余时间保持一致；
     * 摘掉时临时抑制结算回调，避免"加速"被当成"到期"炸掉。</p>
     */
    public static void accelerate(LivingEntity target, int ticks) {
        MobEffectInstance instance = target.getEffect(effect());
        if (instance == null) {
            return;
        }
        int next = instance.getDuration() - ticks;
        UUID uuid = target.getUUID();
        SUPPRESS_SETTLE.add(uuid);
        try {
            target.removeEffect(effect());
            if (next > 0) {
                target.addEffect(new MobEffectInstance(effect(), next, 0, false, true, true));
                return;
            }
        } finally {
            SUPPRESS_SETTLE.remove(uuid);
        }
        // 缩短到 0：直接结算
        settle(target);
    }

    /** Effect 到期回调（由 {@link BloodDebtEffect#onEffectRemoved} 触发） */
    public static void settle(LivingEntity target) {
        if (SUPPRESS_SETTLE.contains(target.getUUID())) {
            return;
        }
        Debt debt = DEBTS.remove(target.getUUID());
        if (debt == null || !target.isAlive() || target.level().isClientSide) {
            return;
        }
        LivingEntity caster = resolveCaster(target, debt.casterUUID);
        if (caster == null || !caster.isAlive()) {
            return;
        }
        float amount = debt.accumulated;
        if (amount > 0) {
            DamageSources.applyDamage(
                    target,
                    amount,
                    FastSpellRegistry.BLOOD_DEBT.get().getDamageSource(caster)
            );
            MagicManager.spawnParticles(
                    target.level(),
                    ParticleHelper.BLOOD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    30, .35, .4, .35, .1, true
            );
            target.level().playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_HURT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.2f,
                    0.6f
            );
        }
        MinecraftForge.EVENT_BUS.post(new BloodDebtSettleEvent(caster, target, amount));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DEBTS.remove(event.getEntity().getUUID());
        SUPPRESS_SETTLE.remove(event.getEntity().getUUID());
    }

    /** 清账（供 KubeJS / 其他系统调用） */
    public static void clear(LivingEntity target) {
        DEBTS.remove(target.getUUID());
    }

    private static LivingEntity resolveTarget(LivingEntity caster, UUID targetUUID) {
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(targetUUID);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity resolveCaster(LivingEntity target, UUID casterUUID) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(casterUUID);
        return entity instanceof LivingEntity living ? living : null;
    }
}
