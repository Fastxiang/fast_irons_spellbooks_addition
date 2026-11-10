package com.main.fast_irons_spellbooks_addition.entity.summon;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.event.CantorCanAttackEvent;
import com.main.fast_irons_spellbooks_addition.event.CantorTargetMemory;
import com.main.fast_irons_spellbooks_addition.registry.FastAttributeRegistry;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.spells.fire.BurningDashSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 自动施法随从（悬浮炮台）
 *
 * <p>悬浮在主人头部左右侧，按固定间隔<b>代替主人</b>释放它记录的那一个法术。</p>
 *
 * <h2>它是"施法者"，不是"主人的手"</h2>
 * <p>本类<b>自己实现</b> {@link IMagicEntity}，用 {@code new MagicData(true)}（mob 模式）走一遍
 * 完整的施法流程：{@code initiateCast → handleCastDuration → onServerCastTick → onCast(CastSource.MOB)}。
 * 因此法术的<b>生成位置是实体自己的眼睛位置</b>、<b>发射方向是实体自己的视线</b>，
 * 瞄准由实体自己完成（见 {@link #setTargetMode(String)}），与玩家的朝向无关。</p>
 *
 * <p><b>为什么不借 fast 模组的 PathfinderMob mixin</b>：本 mod 是 fast 的前置，反向依赖会成环。
 * ISS 的 {@code IMagicEntity} 本身就是公开接口，自己实现即可，不引入任何 mixin。</p>
 *
 * <h2>伤害仍然算玩家的</h2>
 * <p>实体施法产生的伤害，其 {@code SpellDamageSource} 的归属实体是它自己。为了让伤害、
 * 击杀、吸血、以及所有「造成魔法伤害时」类饰品都按<b>主人</b>结算，
 * {@code event.CantorDamageRedirect} 会在 {@code SpellDamageEvent} 最早时机把这一发
 * 改写成主人的 {@code SpellDamageSource} 再重放，等价于"从头就是玩家打的"。</p>
 *
 * <p>配套地，每次施法前会调用 {@link #mirrorOwnerAttributes} 把主人的法术属性
 * （魔攻 / 法术强度 / 各学派强度 / 吟唱缩减）整体复制到实体身上，
 * 所以实体算出来的伤害倍率与主人完全一致。</p>
 */
/**
 * 悬浮在主人身侧、按固定间隔自动施法的随从。
 *
 * <p><b>必须继承 {@link PathfinderMob}，不能只继承 {@link Mob}</b>：
 * ISS 的 {@code SyncedSpellData#doSync()} 在实体的施法状态变化时会构造
 * {@code SyncEntityDataPacket(SyncedSpellData, IMagicEntity)}，而那个构造函数硬性检查
 * {@code entity instanceof PathfinderMob}，否则直接抛
 * {@code IllegalStateException: ... must extend PathfinderMob}。
 * 也就是说 —— 只实现 {@code IMagicEntity} 还不够，ISS 只认 PathfinderMob 系的施法生物。
 * 我们不用寻路（{@code registerGoals()} 空、位置由 followOwner 直接写），
 * 继承它只是为了拿到这个类型身份。</p>
 */
public class PhantomCantor extends PathfinderMob implements IMagicEntity {

    public static final int SIDE_LEFT = -1;
    public static final int SIDE_RIGHT = 1;

    /** 自动索敌：打最近的敌对生物（默认） */
    public static final String TARGET_NEAREST = "nearest";
    /** 跟随主人的攻击目标 */
    public static final String TARGET_OWNER_TARGET = "owner_target";
    /** 复制主人的视线方向（实体位置 + 玩家朝向） */
    public static final String TARGET_OWNER_LOOK = "owner_look";
    /** 不瞄准，保持自身朝向 */
    public static final String TARGET_NONE = "none";

    /**
     * 锁定目标的复查间隔（tick）。
     *
     * <p>索敌每 tick 都要跑（朝向要跟着目标），但 {@code canAttack} 事件会进 KJS，
     * 每 tick 抛一次太贵。所以选定后缓存这么多 tick，期间直接复用，
     * 目标死亡 / 出界 / 换维度时才立刻重选。</p>
     */
    private static final int TARGET_REFRESH_TICKS = 10;

    /** 主人「最近主动打过的目标」记忆时长（tick），超过就退回普通索敌 */
    private static final int OWNER_ATTACK_MEMORY_TICKS = 200;

    private static final EntityDataAccessor<String> DATA_SPELL_ID =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SPELL_LEVEL =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INTERVAL =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SIDE =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAST_REMAIN =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAST_TOTAL =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TARGET_MODE =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_SEARCH_RANGE =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(PhantomCantor.class, EntityDataSerializers.OPTIONAL_UUID);

    /**
     * 每次施法前从主人身上镜像过来的属性。
     *
     * <p><b>{@code magic_attack} 是必须项</b>：本 mod 的 {@code AbstractSpellMixin} 会在
     * {@code AbstractSpell#getSpellPower} 末尾 post {@code SpellPowerCalculateEvent}，
     * 整合包 {@code set_spell_power.js} 监听它并执行
     * {@code entity.getAttribute("fast_irons_spellbooks_addition:magic_attack").getValue()}
     * 来覆盖法术强度 —— 所以实体的 {@code magic_attack} 必须存在且在施法前等于主人的值，
     * 否则不仅数值对不上，那一行 JS 还会因为属性为 null 直接抛异常。</p>
     *
     * <p>其余属性（{@code spell_power} 与九大学派强度）是兜底：即使某个整合包没装那段 JS，
     * 实体算出来的倍率也和主人一致。</p>
     */
    private static final List<RegistryObject<Attribute>> MIRROR_ATTRIBUTES = List.of(
            FastAttributeRegistry.MAGIC_ATTACK,
            AttributeRegistry.SPELL_POWER,
            AttributeRegistry.FIRE_SPELL_POWER,
            AttributeRegistry.ICE_SPELL_POWER,
            AttributeRegistry.LIGHTNING_SPELL_POWER,
            AttributeRegistry.HOLY_SPELL_POWER,
            AttributeRegistry.ENDER_SPELL_POWER,
            AttributeRegistry.BLOOD_SPELL_POWER,
            AttributeRegistry.EVOCATION_SPELL_POWER,
            AttributeRegistry.NATURE_SPELL_POWER,
            AttributeRegistry.ELDRITCH_SPELL_POWER,
            AttributeRegistry.CAST_TIME_REDUCTION,
            AttributeRegistry.SUMMON_DAMAGE
    );

    /** mob 模式的魔法数据：内部 recasts 由 ISS 提供 no-op 兜底，不会 NPE */
    private final MagicData magicData = new MagicData(true);

    @Nullable
    private UUID ownerUUID;

    /** 距离下一次开始吟唱还有多少 tick */
    private int castCooldown = 20;
    /** 剩余寿命，-1 = 永久 */
    private int lifetime = -1;
    /** 召唤时刻（{@code level.getGameTime()}）。到达上限时用它挑"最旧"的那只替换掉 */
    private long birthTime = 0L;
    /** 上一 tick 正在吟唱，用于检测"施法结束"补一次冷却 */
    private boolean wasCasting = false;

    /** 当前锁定的目标（只在服务端用，不进存档） */
    @Nullable
    private LivingEntity lockedTarget;
    /** 锁定目标的过期时刻（{@code level.getGameTime()}） */
    private long lockedTargetExpire = 0L;

    public PhantomCantor(EntityType<? extends PhantomCantor> type, Level level) {
        super(type, level);
        this.magicData.setSyncedData(new SyncedSpellData(this));
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noCulling = true;
        this.setPersistenceRequired();
        this.setMaxUpStep(0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        for (RegistryObject<Attribute> entry : MIRROR_ATTRIBUTES) {
            Attribute attribute = entry.isPresent() ? entry.get() : null;
            if (attribute != null) {
                builder.add(attribute);
            }
        }
        return builder;
    }

    // ------------------------------------------------------------------
    // 召唤 / 查找
    // ------------------------------------------------------------------

    /**
     * 召唤一个随从。
     *
     * <p>同侧已有 → 直接返回旧的（不会叠一堆）。总数受主人的
     * {@code fast_irons_spellbooks_addition:cantor_limit} 属性限制：已经到上限时，
     * 先解雇<b>最旧</b>的那只再召唤新的。所有联动（装备 / 饰品 / 套装）共用这一个上限，
     * 以后加新来源只需要给玩家 +1 属性，不用各自写占位逻辑。</p>
     *
     * <h2>上限检查必须盖住"早退"这条路</h2>
     * <p>以前这里是 {@code if (find(...) != null) return it;} 早退，<b>直接跳过了上限</b>。
     * 于是只要曾经因为任何原因多出来一只（调高过属性、调小过 maxValue、跨维度留下的残影），
     * 后续每次召唤都会命中早退、永远不清理，那多出来的一只就永久存活 ——
     * 表现得就像"上限没生效"。现在早退分支也会把总数裁到上限以内。</p>
     *
     * @return 召唤出来的随从；主人上限为 0 时返回 {@code null}，调用方必须判空
     */
    @Nullable
    public static PhantomCantor summon(LivingEntity owner, int side) {
        // 一次 AABB 查询同时用于"找同侧"和"数总数"，避免早退路径再查一遍
        List<PhantomCantor> owned = findAll(owner);
        int limit = getSummonLimit(owner);

        PhantomCantor existing = null;
        for (PhantomCantor cantor : owned) {
            if (cantor.getSide() == side) {
                existing = cantor;
                break;
            }
        }

        if (limit <= 0) {
            // 上限被压到 0：这一侧本来就该没有随从
            if (existing != null) {
                existing.dismiss();
            }
            return null;
        }

        if (existing != null) {
            // 早退也要裁一次：把历史上多出来的、或上限被调低后超出的那些清掉
            trimToLimit(owned, existing, limit);
            return existing;
        }

        // 新建前腾位置：这里是 >= limit（要空出一个名额），和 trimToLimit 的 > limit 差 1
        while (owned.size() >= limit) {
            PhantomCantor oldest = owned.get(0);
            for (PhantomCantor candidate : owned) {
                if (candidate.birthTime < oldest.birthTime) {
                    oldest = candidate;
                }
            }
            oldest.dismiss();
            owned.remove(oldest);
        }

        PhantomCantor cantor = new PhantomCantor(FastEntityRegistry.PHANTOM_CANTOR.get(), owner.level());
        cantor.setOwner(owner);
        cantor.setSide(side);
        cantor.birthTime = owner.level().getGameTime();
        cantor.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.92, owner.getZ());
        owner.level().addFreshEntity(cantor);
        return cantor;
    }

    /**
     * 把主人名下的随从裁到上限以内，{@code keep} 这一只不动。
     *
     * <p>和新建前的腾位置差 1：腾位置用 {@code >= limit}（要空出一个名额），
     * 这里用 {@code > limit}（已经有 {@code keep} 了，只要不超标就行）。</p>
     */
    private static void trimToLimit(List<PhantomCantor> owned, PhantomCantor keep, int limit) {
        while (owned.size() > limit) {
            PhantomCantor oldest = null;
            for (PhantomCantor candidate : owned) {
                if (candidate == keep) {
                    continue;
                }
                if (oldest == null || candidate.birthTime < oldest.birthTime) {
                    oldest = candidate;
                }
            }
            if (oldest == null) {
                // 只剩 keep 自己，没有可裁的了
                return;
            }
            oldest.dismiss();
            owned.remove(oldest);
        }
    }

    /** 主人当前的随从数量上限（属性基础值 1，靠装备叠加） */
    public static int getSummonLimit(LivingEntity owner) {
        RegistryObject<Attribute> entry = FastAttributeRegistry.CANTOR_LIMIT;
        Attribute attribute = entry.isPresent() ? entry.get() : null;
        if (attribute == null) {
            return 1;
        }
        AttributeInstance instance = owner.getAttribute(attribute);
        return instance == null ? 1 : Mth.floor(instance.getValue());
    }

    /** 主人当前所有的随从（不分左右），按召唤顺序无关，挑最旧时看 {@link #birthTime} */
    public static List<PhantomCantor> findAll(LivingEntity owner) {
        List<PhantomCantor> result = new ArrayList<>();
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return result;
        }
        AABB area = owner.getBoundingBox().inflate(64.0D);
        for (PhantomCantor cantor : serverLevel.getEntitiesOfClass(PhantomCantor.class, area)) {
            if (cantor.isAlive()
                    && cantor.ownerUUID != null
                    && cantor.ownerUUID.equals(owner.getUUID())) {
                result.add(cantor);
            }
        }
        return result;
    }

    /** 找主人某一侧已经存在的随从 */
    @Nullable
    public static PhantomCantor find(LivingEntity owner, int side) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        AABB area = owner.getBoundingBox().inflate(16.0D);
        List<PhantomCantor> list = serverLevel.getEntitiesOfClass(PhantomCantor.class, area);
        for (PhantomCantor cantor : list) {
            if (cantor.getSide() == side && owner.getUUID().equals(cantor.ownerUUID)) {
                return cantor;
            }
        }
        return null;
    }

    /**
     * 解雇主人身上的随从。
     *
     * <p>给 KubeJS 侧「饰品卸下 / 前置条件失效时清场」用：那些钩子手里只有实体，
     * 拿不到实体引用，只能按主人反查。所以这里是静态入口，不是实例上的 {@link #dismiss()}。</p>
     *
     * <p>主人是 {@link LivingEntity}：玩家与女仆共用同一套召唤 / 解雇入口。</p>
     *
     * <p><b>只保留这一个重载</b>：同一个方法名再来一个参数更少的版本，Rhino 会判定
     * "The choice of Java method ... is ambiguous"。{@code side} 用 0 表示"不限侧"，
     * 就把两种语义收进同一个签名里了。</p>
     *
     * @param side 只解雇这一侧（{@link #SIDE_LEFT} / {@link #SIDE_RIGHT}）；传 0 表示两侧全删
     * @return 实际解雇的数量，调用方可以据此区分"本来就没有"和"真的删掉了"
     */
    public static int dismissAll(LivingEntity owner, int side) {
        int count = 0;
        for (PhantomCantor cantor : findAll(owner)) {
            if (side != 0 && cantor.getSide() != side) {
                continue;
            }
            cantor.dismiss();
            count++;
        }
        return count;
    }

    // ------------------------------------------------------------------
    // 记录的法术
    // ------------------------------------------------------------------

    /**
     * 记录要释放的法术。
     *
     * <p><b>只保留这一个重载</b>：KubeJS 侧传字符串会自动转成 {@link ResourceLocation}，
     * 但如果同时存在 {@code setSpell(String,int)}，Rhino 两个都判定为可调用 → 直接抛
     * "The choice of Java method ... is ambiguous"。所以字符串版本改成私有方法，对外只有这一个入口。</p>
     */
    public void setSpell(ResourceLocation spellId, int spellLevel) {
        setSpellInternal(spellId == null ? "" : spellId.toString(), spellLevel);
    }

    /**
     * 幂等：只有"换了法术 / 改了等级"才重排第一发的等待时间。
     *
     * <p>调用方可能每 10 tick 兜底刷新一次配置（见整合包 {@code spell_choir.js} 的疾风颂歌）。
     * 如果这里无条件把 {@code castCooldown} 压回 20，而兜底间隔也是 20 tick 以内，
     * 那么 {@code --castCooldown} 永远走不到 0，侍灵会一直悬着但一发都放不出来。</p>
     */
    private void setSpellInternal(String spellId, int spellLevel) {
        String normalized = spellId == null ? "" : spellId;
        int level = Math.max(1, spellLevel);
        String previousId = this.entityData.get(DATA_SPELL_ID);
        int previousLevel = this.entityData.get(DATA_SPELL_LEVEL);
        this.entityData.set(DATA_SPELL_ID, normalized);
        this.entityData.set(DATA_SPELL_LEVEL, level);
        if (!normalized.equals(previousId) || level != previousLevel) {
            this.castCooldown = 20;
        }
    }

    public String getSpellId() {
        return this.entityData.get(DATA_SPELL_ID);
    }

    public int getSpellLevel() {
        return Math.max(1, this.entityData.get(DATA_SPELL_LEVEL));
    }

    @Nullable
    public AbstractSpell resolveSpell() {
        String id = getSpellId();
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        AbstractSpell spell = SpellRegistry.getSpell(location);
        return spell == SpellRegistry.none() ? null : spell;
    }

    // ------------------------------------------------------------------
    // 瞄准
    // ------------------------------------------------------------------

    /** 瞄准方式，见 {@code TARGET_*} 常量 */
    public void setTargetMode(String mode) {
        this.entityData.set(DATA_TARGET_MODE, mode == null ? TARGET_NEAREST : mode);
    }

    public String getTargetMode() {
        return this.entityData.get(DATA_TARGET_MODE);
    }

    /** 自动索敌半径（格） */
    public void setSearchRange(double range) {
        this.entityData.set(DATA_SEARCH_RANGE, (float) Math.max(1.0D, range));
    }

    public double getSearchRange() {
        return Math.max(1.0D, this.entityData.get(DATA_SEARCH_RANGE));
    }

    /** 按当前瞄准方式解算目标；返回 null 表示"不指定目标" */
    @Nullable
    private LivingEntity resolveTarget(LivingEntity owner) {
        String mode = getTargetMode();
        if (TARGET_NONE.equals(mode) || TARGET_OWNER_LOOK.equals(mode)) {
            clearLockedTarget();
            return null;
        }

        long now = this.level().getGameTime();
        LivingEntity locked = this.lockedTarget;
        if (locked != null && now < this.lockedTargetExpire && isStillValidTarget(mode, owner, locked)) {
            return locked;
        }
        clearLockedTarget();

        LivingEntity chosen = TARGET_OWNER_TARGET.equals(mode)
                ? resolveOwnerTarget(owner)
                : findNearestEnemy(owner);

        if (chosen == null) {
            return null;
        }

        this.lockedTarget = chosen;
        this.lockedTargetExpire = now + TARGET_REFRESH_TICKS;
        return chosen;
    }

    /** 缓存里的目标还值不值得继续锁定（纯 Java 判断，不再抛事件） */
    private boolean isStillValidTarget(String mode, LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isRemoved() || target.level() != this.level()) {
            return false;
        }
        if (TARGET_NEAREST.equals(mode)) {
            double range = getSearchRange();
            return this.distanceToSqr(target) <= range * range;
        }
        return true;
    }

    private void clearLockedTarget() {
        this.lockedTarget = null;
        this.lockedTargetExpire = 0L;
    }

    /** 打主人的目标：优先主人当前的攻击目标，其次主人最近打过的 */
    @Nullable
    private LivingEntity resolveOwnerTarget(LivingEntity owner) {
        LivingEntity ownerTarget = owner instanceof Mob mob ? mob.getTarget() : null;
        if (ownerTarget == null || !ownerTarget.isAlive() || ownerTarget == this) {
            ownerTarget = owner.getLastHurtMob();
        }
        if (ownerTarget == null || ownerTarget == this || ownerTarget == owner) {
            return null;
        }
        return canAttack(owner, ownerTarget) ? ownerTarget : null;
    }

    /**
     * 自动索敌，两级优先级：
     * <ol>
     *     <li>主人刚刚主动打过的目标（{@link CantorTargetMemory} 记录）</li>
     *     <li>范围内最近的、通过 {@code canAttack} 的目标</li>
     * </ol>
     * 每一只候选都要先过 {@link #canAttack}，被 KJS 否掉的（友军 / 宠物 / 女仆等）直接跳过。
     */
    @Nullable
    private LivingEntity findNearestEnemy(LivingEntity owner) {
        AABB area = this.getBoundingBox().inflate(getSearchRange());
        List<LivingEntity> candidates = new ArrayList<>(this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != this
                        && e != owner
                        && !(e instanceof Player)
                        && !(e instanceof PhantomCantor)
                        && e.isAlive()
                        && e.isPickable()
                        && !e.isSpectator()
                        && !owner.isAlliedTo(e)
        ));

        // 第一优先级：主人正在打的那只
        LivingEntity ownerTarget = findOwnerAttackTarget(owner);
        if (ownerTarget != null) {
            for (LivingEntity candidate : candidates) {
                if (candidate == ownerTarget && canAttack(owner, candidate)) {
                    return candidate;
                }
            }
        }

        // 第二优先级：最近的、能打的那只
        candidates.sort((a, b) -> Double.compare(this.distanceToSqr(a), this.distanceToSqr(b)));
        for (LivingEntity candidate : candidates) {
            if (canAttack(owner, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 主人最近主动打过的目标，且必须还在索敌半径内。
     *
     * <p>记录由 {@link CantorTargetMemory} 维护；侍灵自己打出去的伤害在重放成主人伤害时
     * 会被排除，所以不会出现"它把自己上一发的目标当成主人正在打的目标"。</p>
     */
    @Nullable
    private LivingEntity findOwnerAttackTarget(LivingEntity owner) {
        LivingEntity target = CantorTargetMemory.getLastAttacked(owner, OWNER_ATTACK_MEMORY_TICKS);
        if (target == null || target == this || target == owner) {
            return null;
        }
        if (!target.isAlive() || target.isRemoved() || target.level() != this.level()) {
            return null;
        }
        double range = getSearchRange();
        return this.distanceToSqr(target) <= range * range ? target : null;
    }

    /**
     * 询问一次「这只能不能打」。
     *
     * <p>Java 侧先给出初始结果（活着 / 同维度 / 不是主人 / 不是侍灵 / 不在同一队伍），
     * 然后 post {@link CantorCanAttackEvent} 让监听方改写 —— 友军、宠物、女仆、
     * 其他召唤物都在这一层被剔掉。</p>
     */
    private boolean canAttack(LivingEntity owner, LivingEntity target) {
        boolean allowed = target != owner
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof PhantomCantor)
                && target.level() == this.level()
                && !owner.isAlliedTo(target);
        CantorCanAttackEvent event = new CantorCanAttackEvent(this, owner, target, allowed);
        MinecraftForge.EVENT_BUS.post(event);
        return event.isCanAttack();
    }

    /** 让实体正对某个目标（把 yRot / xRot 直接写成精确值，getLookAngle 立刻生效） */
    private void lookAt(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI)));
        applyRotation(yaw, pitch);
    }

    private void copyOwnerLook(LivingEntity owner) {
        applyRotation(owner.getYRot(), owner.getXRot());
    }

    private void applyRotation(float yaw, float pitch) {
        this.setYRot(yaw);
        this.setXRot(pitch);
        // 同步上一 tick 的值，避免客户端渲染插值把朝向拉回旧角度
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    /** 每 tick 更新朝向；正在吟唱时持续咬住目标 */
    private void updateFacing(LivingEntity owner) {
        LivingEntity target = resolveTarget(owner);
        if (target != null) {
            this.setTarget(target);
            lookAt(target);
        } else {
            this.setTarget(null);
            if (TARGET_OWNER_LOOK.equals(getTargetMode())) {
                copyOwnerLook(owner);
            }
        }
    }

    // ------------------------------------------------------------------
    // 属性镜像
    // ------------------------------------------------------------------

    /**
     * 把主人的法术属性复制到实体自身。
     *
     * <p>只在施法前调用一次（不是每 tick），避免 {@code setBaseValue} 反复触发属性脏标记。</p>
     */
    private void mirrorOwnerAttributes(LivingEntity owner) {
        for (RegistryObject<Attribute> entry : MIRROR_ATTRIBUTES) {
            Attribute attribute = entry.isPresent() ? entry.get() : null;
            if (attribute == null) {
                continue;
            }
            AttributeInstance mine = this.getAttribute(attribute);
            AttributeInstance theirs = owner.getAttribute(attribute);
            if (mine != null && theirs != null) {
                mine.setBaseValue(theirs.getValue());
            }
        }
    }

    // ------------------------------------------------------------------
    // 节奏
    // ------------------------------------------------------------------

    /** 两次施法之间的间隔（tick），从"这一发打完"开始算 */
    public void setInterval(int ticks) {
        this.entityData.set(DATA_INTERVAL, Math.max(1, ticks));
    }

    public int getInterval() {
        return Math.max(1, this.entityData.get(DATA_INTERVAL));
    }

    /** 立即开始下一发（正在吟唱时无效） */
    public void castNow() {
        if (!this.level().isClientSide && !this.magicData.isCasting()) {
            this.castCooldown = 0;
        }
    }

    /** 立刻消散 */
    public void dismiss() {
        this.discard();
    }

    // ------------------------------------------------------------------
    // 外观
    // ------------------------------------------------------------------

    /** -1 左侧 / 1 右侧 */
    public void setSide(int side) {
        this.entityData.set(DATA_SIDE, side >= 0 ? SIDE_RIGHT : SIDE_LEFT);
    }

    public int getSide() {
        return this.entityData.get(DATA_SIDE) >= 0 ? SIDE_RIGHT : SIDE_LEFT;
    }

    /** 剩余寿命（tick），-1 = 永久 */
    public void setLifetime(int ticks) {
        this.lifetime = ticks;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    // ------------------------------------------------------------------
    // 状态（给渲染 / HUD 用）
    // ------------------------------------------------------------------

    public boolean isCastingSpell() {
        return this.entityData.get(DATA_CAST_TOTAL) > 0;
    }

    public int getCastRemainingTicks() {
        return this.entityData.get(DATA_CAST_REMAIN);
    }

    public int getCastTotalTicks() {
        return this.entityData.get(DATA_CAST_TOTAL);
    }

    /** 吟唱进度 0~1 */
    public float getCastProgress(float partialTick) {
        int total = getCastTotalTicks();
        if (total <= 0) {
            return 0f;
        }
        float remain = Math.max(0f, getCastRemainingTicks() - partialTick);
        return Mth.clamp(1f - remain / total, 0f, 1f);
    }

    public int getCooldownTicks() {
        return Math.max(0, this.castCooldown);
    }

    // ------------------------------------------------------------------
    // 主人
    // ------------------------------------------------------------------

    public void setOwner(@Nullable LivingEntity owner) {
        this.ownerUUID = owner == null ? null : owner.getUUID();
        this.entityData.set(DATA_OWNER, Optional.ofNullable(this.ownerUUID));
    }

    @Nullable
    public LivingEntity getOwnerEntity() {
        UUID uuid = this.ownerUUID;
        if (uuid == null) {
            Optional<UUID> synced = this.entityData.get(DATA_OWNER);
            uuid = synced == null ? null : synced.orElse(null);
        }
        if (uuid == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            // 玩家优先走玩家列表，getEntity() 在部分情况下拿不到玩家
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.level() == this.level() ? player : null;
            }
            Entity entity = serverLevel.getEntity(uuid);
            return entity instanceof LivingEntity living ? living : null;
        }
        if (this.level().isClientSide) {
            return this.level().getPlayerByUUID(uuid);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // IMagicEntity —— 自己实现的施法通道
    // ------------------------------------------------------------------

    @Override
    public MagicData getMagicData() {
        return this.magicData;
    }

    @Override
    public boolean isCasting() {
        return this.magicData.isCasting();
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        if (spell == null || spell == SpellRegistry.none()) {
            this.magicData.resetCastingState();
            return;
        }
        this.magicData.resetCastingState();
        this.magicData.resetAdditionalCastData();

        // 和玩家施法一致：先过法术自身的施法条件（射线类法术会在这里选定目标，
        // 射线起点是实体自己的眼睛，所以锁定也是以实体为准）
        if (!spell.checkPreCastConditions(this.level(), spellLevel, this, this.magicData)) {
            this.magicData.resetAdditionalCastData();
            return;
        }

        int castTime = Math.max(0, spell.getEffectiveCastTime(spellLevel, this));
        this.magicData.initiateCast(spell, spellLevel, castTime, CastSource.MOB, SpellSelectionManager.MAINHAND);
        spell.onServerPreCast(this.level(), spellLevel, this, this.magicData);
        this.setCastState(castTime, castTime);
    }

    @Override
    public void cancelCast() {
        if (this.magicData.isCasting()) {
            AbstractSpell spell = resolveSpell();
            if (spell != null) {
                spell.onServerCastComplete(this.level(), this.getSpellLevel(), this, this.magicData, false);
            }
        }
        castComplete();
    }

    @Override
    public void castComplete() {
        this.magicData.resetCastingState();
        this.setCastState(0, 0);
        this.castCooldown = this.getInterval();
    }

    @Override
    public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
        this.magicData.setSyncedData(syncedSpellData);
    }

    @Override
    public void notifyDangerousProjectile(Projectile projectile) {
    }

    @Override
    public boolean setTeleportLocationBehindTarget(int distance) {
        return false;
    }

    @Override
    public void setBurningDashDirectionData() {
        this.magicData.setAdditionalCastData(new BurningDashSpell.BurningDashDirectionOverrideCastData());
    }

    @Override
    public boolean isDrinkingPotion() {
        return false;
    }

    @Override
    public boolean getHasUsedSingleAttack() {
        return false;
    }

    @Override
    public void setHasUsedSingleAttack(boolean value) {
    }

    @Override
    public void startDrinkingPotion() {
    }

    // ------------------------------------------------------------------
    // tick
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientTick();
            return;
        }
        this.serverTick();
    }

    private void serverTick() {
        LivingEntity owner = getOwnerEntity();
        if (owner == null
                || !owner.isAlive()
                || owner.level() != this.level()
                || (owner instanceof ServerPlayer player && player.isSpectator())) {
            this.discard();
            return;
        }

        if (this.lifetime > 0 && --this.lifetime <= 0) {
            this.discard();
            return;
        }

        this.followOwner(owner);
        this.updateFacing(owner);

        if (this.magicData.isCasting()) {
            this.wasCasting = true;
            this.tickCasting();
            return;
        }

        if (this.wasCasting) {
            // 施法刚结束（onCast 里已经自行结算），补一次冷却起点
            this.wasCasting = false;
            this.castCooldown = this.getInterval();
        }

        if (--this.castCooldown <= 0) {
            this.beginCast(owner);
        }
    }

    /** 悬浮在主人头部左右侧，平滑跟随（只管位置，朝向交给 updateFacing） */
    private void followOwner(LivingEntity owner) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));

        float phase = this.getSide() < 0 ? 1.7f : 0.0f;
        double bob = Mth.sin((this.tickCount + phase) * 0.12f) * 0.07D;

        // 站位要够远：贴着头侧 0.5 格会直接糊在第一人称视野里。
        // 侧向 1.25 格 + 身后 0.55 格 + 头顶略高，既在余光里又完全不挡视线。
        Vec3 target = owner.position()
                .add(right.scale(this.getSide() * 1.25D))
                .add(forward.scale(-0.55D))
                .add(0.0D, owner.getBbHeight() * 0.92D + bob, 0.0D);

        Vec3 current = this.position();
        if (current.distanceToSqr(target) > 16.0D) {
            this.setPos(target.x, target.y, target.z);
        } else {
            Vec3 next = current.add(target.subtract(current).scale(0.45D));
            this.setPos(next.x, next.y, next.z);
        }
    }

    private void beginCast(LivingEntity owner) {
        AbstractSpell spell = resolveSpell();
        if (spell == null) {
            this.castCooldown = this.getInterval();
            return;
        }

        // 属性对齐主人（伤害倍率 / 吟唱缩减）
        this.mirrorOwnerAttributes(owner);
        // 瞄准在施法前再确认一次，保证 getLookAngle() 是这一刻的准确朝向
        this.updateFacing(owner);

        this.initiateCastSpell(spell, this.getSpellLevel());

        if (!this.magicData.isCasting()) {
            // 施法条件不满足（没目标 / 蓝不够之类），过一会儿再试
            this.castCooldown = 20;
            return;
        }

        this.wasCasting = true;
        // 瞬发法术当 tick 就结算，不必等下一 tick
        this.tickCasting();
    }

    /** 推进吟唱；顺序与 ISS 自己的施法生物一致 */
    private void tickCasting() {
        AbstractSpell spell = resolveSpell();
        if (spell == null) {
            this.cancelCast();
            return;
        }

        this.magicData.handleCastDuration();
        int remain = this.magicData.getCastDurationRemaining();
        this.setCastRemaining(remain);

        if (remain > 0) {
            spell.onServerCastTick(this.level(), this.getSpellLevel(), this, this.magicData);
            if (spell.getCastType() == CastType.CONTINUOUS && (remain + 1) % 10 == 0) {
                spell.onCast(this.level(), this.getSpellLevel(), this, CastSource.MOB, this.magicData);
            }
            return;
        }

        spell.onCast(this.level(), this.getSpellLevel(), this, CastSource.MOB, this.magicData);
        this.castComplete();
    }

    private void setCastState(int remain, int total) {
        this.entityData.set(DATA_CAST_REMAIN, Math.max(0, remain));
        this.entityData.set(DATA_CAST_TOTAL, Math.max(0, total));
    }

    private void setCastRemaining(int remain) {
        this.entityData.set(DATA_CAST_REMAIN, Math.max(0, remain));
    }

    // ------------------------------------------------------------------
    // 客户端表现
    // ------------------------------------------------------------------

    private void clientTick() {
        if (this.tickCount % 3 != 0) {
            return;
        }
        double angle = (this.tickCount * 0.22D) + (this.getSide() < 0 ? Math.PI : 0.0D);
        double radius = 0.28D;
        double px = this.getX() + Math.cos(angle) * radius;
        double pz = this.getZ() + Math.sin(angle) * radius;
        double py = this.getY() + 0.05D + Math.sin(angle * 1.7D) * 0.05D;
        this.level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0D, 0.005D, 0.0D);

        if (this.isCastingSpell()) {
            this.level().addParticle(ParticleTypes.ENCHANT, px, py, pz, 0.0D, 0.02D, 0.0D);
            if (this.tickCount % 6 == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ------------------------------------------------------------------
    // 数据 / 存档
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SPELL_ID, "");
        this.entityData.define(DATA_SPELL_LEVEL, 1);
        this.entityData.define(DATA_INTERVAL, 200);
        this.entityData.define(DATA_SIDE, SIDE_RIGHT);
        this.entityData.define(DATA_CAST_REMAIN, 0);
        this.entityData.define(DATA_CAST_TOTAL, 0);
        this.entityData.define(DATA_TARGET_MODE, TARGET_NEAREST);
        this.entityData.define(DATA_SEARCH_RANGE, 12.0F);
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SpellId", getSpellId());
        tag.putInt("SpellLevel", getSpellLevel());
        tag.putInt("Interval", getInterval());
        tag.putInt("Side", getSide());
        tag.putInt("Lifetime", this.lifetime);
        tag.putLong("BirthTime", this.birthTime);
        tag.putString("TargetMode", getTargetMode());
        tag.putDouble("SearchRange", getSearchRange());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpellId")) {
            this.entityData.set(DATA_SPELL_ID, tag.getString("SpellId"));
        }
        if (tag.contains("SpellLevel")) {
            this.entityData.set(DATA_SPELL_LEVEL, Math.max(1, tag.getInt("SpellLevel")));
        }
        if (tag.contains("Interval")) {
            this.entityData.set(DATA_INTERVAL, Math.max(1, tag.getInt("Interval")));
        }
        if (tag.contains("Side")) {
            this.setSide(tag.getInt("Side"));
        }
        if (tag.contains("Lifetime")) {
            this.lifetime = tag.getInt("Lifetime");
        }
        if (tag.contains("BirthTime")) {
            this.birthTime = tag.getLong("BirthTime");
        }
        if (tag.contains("TargetMode")) {
            this.setTargetMode(tag.getString("TargetMode"));
        }
        if (tag.contains("SearchRange")) {
            this.setSearchRange(tag.getDouble("SearchRange"));
        }
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
            this.entityData.set(DATA_OWNER, Optional.of(this.ownerUUID));
        }
    }

    // ------------------------------------------------------------------
    // 杂项
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        // 纯粹的悬浮载体：不寻路、不移动，位置由 followOwner 直接写
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return 0;
    }

    public static ResourceLocation texture() {
        return FastIronsSpellbooksAddition.id("textures/entity/phantom_cantor.png");
    }
}
