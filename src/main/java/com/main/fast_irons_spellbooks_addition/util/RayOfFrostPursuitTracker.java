package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.registry.FastEffectRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import com.main.fast_irons_spellbooks_addition.spells.ice.RayOfFrostPursuitSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ray_of_frost.RayOfFrostVisualEntity;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冰霜射线-追击 的窗口记账器。
 *
 * <p>窗口本身是 {@link FastEffectRegistry#RAY_OF_FROST_PURSUIT} 挂在施法者身上的 Effect，
 * 玩家能在 HUD 上看到剩余时间；这里负责「触发判定 + 补一条射线 + 回蓝」。
 * 补的这条射线<b>按冰霜射线本体结算伤害</b>（{@link RayOfFrostPursuitSpell#getRayDamage}），
 * 不是把触发它的那一下复制一遍。</p>
 *
 * <h2>为什么用 {@link LivingHurtEvent}</h2>
 * <p>需求是「造成<b>所有</b>伤害时」，而这个事件覆盖近战 / 弓箭 / 枪械 / 法术全部来源，
 * {@code getSource().getEntity()} 就是发起攻击的实体（ISS 的 {@code AbstractSpellMixin}
 * 会把法术伤害的 causing entity 换成施法者）。</p>
 *
 * <h2>无敌帧</h2>
 * <p>追击这一发要求「<b>无视无敌帧</b>，但<b>不抢无敌帧</b>」：</p>
 * <ul>
 *   <li>无视：触发时目标正处在本次伤害刚写进去的无敌帧里（{@code invulnerableTime = 20}），
 *       {@code LivingEntity#hurt} 在 {@code invulnerableTime > 10} 且
 *       {@code amount <= lastHurt} 时直接返回 false，不打穿就根本吃不到伤害。
 *       所以结算前临时清零。</li>
 *   <li>不抢：结算完立刻把 {@code invulnerableTime} 原值写回，并给伤害源
 *       {@code setIFrames(0)}（ISS 的 {@code DamageSources#postHitEffects} 就不会再补帧）。
 *       否则这发追击会顶掉目标身上的无敌帧，把玩家 / 召唤物紧接着的攻击全部吞掉。</li>
 * </ul>
 *
 * <h2>递归</h2>
 * <p>追击自己造成的伤害同样会触发这个事件，用 {@link #replaying} 挡住，
 * 否则会一环套一环无限追加。</p>
 */
@Mod.EventBusSubscriber(modid = FastIronsSpellbooksAddition.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RayOfFrostPursuitTracker {

    /** 窗口时长：30 秒 */
    public static final int DEFAULT_DURATION = 600;

    /** 内部冷却：1 秒 */
    public static final int INTERNAL_COOLDOWN_TICKS = 20;

    /** 命中回复的法力值 */
    public static final float MANA_REFUND = 50f;

    /** 起点相对玩家的随机范围：横向、偏后、高度（相对眼位） */
    private static final double MIN_SIDE = 1.2;
    private static final double MAX_SIDE = 2.2;
    private static final double MIN_BACK = 0.2;
    private static final double MAX_BACK = 1.4;
    private static final double MIN_HEIGHT = -0.75;
    private static final double MAX_HEIGHT = -0.15;

    /** 超过这个距离就不播射线了（只结算），避免拉出一条几十格的怪线 */
    private static final double MAX_VISUAL_DISTANCE = 32.0;

    /** 每个玩家上次触发的 tickCount */
    private static final Map<UUID, Integer> LAST_TRIGGER = new ConcurrentHashMap<>();

    /** 追击伤害重放中：这期间产生的 hurt 事件不再触发追击 */
    private static boolean replaying = false;

    private RayOfFrostPursuitTracker() {
    }

    /**
     * 开窗（重复施放刷新时长，不叠加）。
     * <p>施法等级存进 Effect 的 amplifier（等级 1~5 → amplifier 0~4），
     * 追击结算时按它算「冰霜射线本体伤害」。</p>
     */
    public static void apply(LivingEntity caster, int durationTicks, int spellLevel) {
        caster.addEffect(new MobEffectInstance(
                FastEffectRegistry.RAY_OF_FROST_PURSUIT.get(),
                durationTicks,
                Math.max(0, spellLevel - 1),
                false,  // ambient
                false,  // visible：不显示原版药水漩涡粒子，窗口状态只看 HUD 图标
                true    // showIcon：HUD 上必须能看见
        ));
    }

    /** 从 Effect 的 amplifier 还原施法等级 */
    private static int getSpellLevel(LivingEntity caster) {
        MobEffectInstance instance = caster.getEffect(FastEffectRegistry.RAY_OF_FROST_PURSUIT.get());
        return instance == null ? 1 : instance.getAmplifier() + 1;
    }

    public static boolean isActive(LivingEntity caster) {
        return caster.hasEffect(FastEffectRegistry.RAY_OF_FROST_PURSUIT.get());
    }

    public static void clear(LivingEntity caster) {
        caster.removeEffect(FastEffectRegistry.RAY_OF_FROST_PURSUIT.get());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (replaying) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (target == player || !target.isAlive()) {
            return;
        }
        if (!isActive(player)) {
            return;
        }
        // 0 伤害的事件（例如被完全挡下的攻击）不触发追击
        if (event.getAmount() <= 0) {
            return;
        }

        // 1 秒内部冷却
        int now = player.tickCount;
        Integer last = LAST_TRIGGER.get(player.getUUID());
        if (last != null && now - last < INTERNAL_COOLDOWN_TICKS) {
            return;
        }
        LAST_TRIGGER.put(player.getUUID(), now);

        Vec3 end = target.getBoundingBox().getCenter();
        spawnPursuitRay(player, end);
        // 追击打的是「冰霜射线本体」的伤害，与这一下触发了多少无关
        float damage = RayOfFrostPursuitSpell.getRayDamage(
                FastSpellRegistry.RAY_OF_FROST_PURSUIT.get(), getSpellLevel(player), player);
        // 只有追击真的打上了才回蓝：目标无敌 / 伤害被取消时不该白给
        if (dealPursuitDamage(player, target, damage)) {
            refundMana(player);
        }
    }

    /**
     * 从玩家身边（不是玩家身上）射出一条冰霜射线到目标位置。
     * <p>起点每次都随机：左右随机、前后随机、高度随机，但横向永远 ≥ 1.2 格且偏在身后，
     * 所以射线只会从视野边缘掠过 —— 不会糊在准星上，也不会从身体里穿出来。</p>
     */
    private static void spawnPursuitRay(ServerPlayer player, Vec3 end) {
        Vec3 start = randomStart(player, end);

        double distance = start.distanceTo(end);
        // 起终点重合时 direction 归一化会得到 NaN，直接不画
        if (distance > 0.05 && distance <= MAX_VISUAL_DISTANCE) {
            player.level().addFreshEntity(new RayOfFrostVisualEntity(player.level(), start, end, player));
        }

        // 命中点冰雾 + 雪花：追击落点要看得见
        MagicManager.spawnParticles(player.level(), ParticleHelper.ICY_FOG,
                end.x, end.y, end.z, 12, 0, 0, 0, .3, true);
        MagicManager.spawnParticles(player.level(), ParticleHelper.SNOWFLAKE,
                end.x, end.y, end.z, 24, 0, 0, 0, .3, false);
    }

    /**
     * 在玩家身侧随机取一个射线起点。
     * <p>横向取 {@link #MIN_SIDE}~{@link #MAX_SIDE} 格、偏后 {@link #MIN_BACK}~{@link #MAX_BACK} 格、
     * 高度在眼位下 {@link #MIN_HEIGHT}~{@link #MAX_HEIGHT} 格（胸口附近），每次都不一样。</p>
     *
     * <p>左右不纯随机：<b>目标偏在视线哪一侧，起点就取哪一侧</b>（目标几乎在正前方时才随机）。
     * 这样射线从同侧掠过，不会为了打到对面的目标横穿镜头；
     * 加上横向 1.2 格的下限，起点也不会落在正后方把射线送进镜头里。</p>
     */
    private static Vec3 randomStart(ServerPlayer player, Vec3 end) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();
        // 水平右向量：把朝向绕 Y 轴转 90°
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 eye = player.getEyePosition();

        var random = player.getRandom();
        double targetSide = end.subtract(eye).dot(right);
        boolean onRight = Math.abs(targetSide) > 0.5 ? targetSide > 0 : random.nextBoolean();
        double side = (onRight ? 1 : -1) * (MIN_SIDE + random.nextDouble() * (MAX_SIDE - MIN_SIDE));
        double back = MIN_BACK + random.nextDouble() * (MAX_BACK - MIN_BACK);
        double height = MIN_HEIGHT + random.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT);

        return eye
                .add(right.scale(side))
                .subtract(forward.scale(back))
                .add(0, height, 0);
    }

    /**
     * 追击伤害：<b>冰霜射线本体伤害</b>（500% 魔法攻击，按 buff 里记的施法等级算），
     * 与触发它的那一下无关，不做增伤也不衰减。
     * <p>结算时临时清空无敌帧（否则同等伤害会被 {@code LivingEntity#hurt} 判定成「没打死过」而丢弃），
     * 打完把原值写回，并让伤害源声明 {@code iFrames = 0}，保证这一发既不吞无敌帧、也不留无敌帧。</p>
     *
     * @return 伤害是否真的落到了目标身上
     */
    private static boolean dealPursuitDamage(ServerPlayer player, LivingEntity target, float damage) {
        int savedInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        replaying = true;
        try {
            return DamageSources.applyDamage(target, damage,
                    FastSpellRegistry.RAY_OF_FROST_PURSUIT.get().getDamageSource(player)
                            // 0 表示这一发不写无敌帧；ISS 的 postHitEffects 会照此结算
                            .setIFrames(0));
        } finally {
            replaying = false;
            target.invulnerableTime = savedInvulnerableTime;
        }
    }

    private static void refundMana(ServerPlayer player) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }
        magicData.addMana(MANA_REFUND);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
    }
}
