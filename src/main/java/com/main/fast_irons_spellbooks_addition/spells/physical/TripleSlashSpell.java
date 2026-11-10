package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.api.ComboSpell;
import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.SwordEnergyProjectile;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackHitEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackMissEvent;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackPreEvent;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import com.main.fast_irons_spellbooks_addition.spells.*;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraftforge.common.ForgeMod.ENTITY_REACH;

/**
 * 剑气三连击：以物理三连击（TripleStrikeSpell）为模板，
 * 每次施法在近战挥砍的同时朝准星方向发射 1 道剑气（复刻 Blood Slash）。
 * - 剑气伤害与近战伤害相同，算作物理
 * - 若剑气和近战同时命中同一目标，只生效一个命中（剑气排除近战已命中目标）
 */
public class TripleSlashSpell extends AbstractSpell implements ComboSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("triple_slash");



    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.COMMON)
        .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(0)
        .setAllowCrafting(false)
        .build();

    public TripleSlashSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    private final Map<UUID, Integer> comboMap = new ConcurrentHashMap<>();

    /**
     * 非玩家施法者（女仆 / 怪物）不会走 {@code onCast} 里「玩家开段清零」那条分支，
     * 段号会一直往上漂，重击倍率就落到错误的段上。由外部战斗 AI 在起手前调用。
     */
    @Override
    public void resetCombo(LivingEntity caster) {
        if (caster != null) {
            comboMap.put(caster.getUUID(), 0);
        }
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
    if (entity == null) return getCastTime(spellLevel);

    UUID uuid = entity.getUUID();
    int combo = comboMap.getOrDefault(uuid, 0);
    if (combo >= getRecastCount(spellLevel, entity)) {
    combo = 0;
    }

    if (entity instanceof Player player) {
    if (!MagicData.getPlayerMagicData(player)
            .getPlayerRecasts()
            .hasRecastForSpell(getSpellId())) {
        combo = 0;
    }
    }

    return switch (combo) {
        case 0 -> getCastTime(spellLevel);
        case 1 -> getCastTime(spellLevel);
        case 2 -> 14;
        default -> getCastTime(spellLevel);
    };
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 3;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
    var mc = Minecraft.getInstance();
    if (mc.player == null) {
        return PhysicalSpellAnimations.ATTACK_1;
    }

    UUID uuid = mc.player.getUUID();
    int combo = comboMap.getOrDefault(uuid, 0);

    if (!ClientMagicData.getRecasts().hasRecastForSpell(getSpellId())) {
    combo = 0;
    }

    return switch (combo) {
        case 0 -> PhysicalSpellAnimations.ATTACK_1;
        case 1 -> PhysicalSpellAnimations.ATTACK_3;
        case 2 -> PhysicalSpellAnimations.ATTACK_4;
        default -> PhysicalSpellAnimations.ATTACK_1;
    };
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
    return FastAttributeUtil.getPhysicalAttack(entity) * FastAttributeUtil.getBasicPhysicalDamage(entity);
    }

    private DamageSource getDamageSource(int spellLevel, LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.damageSources().playerAttack(player);
        } else {
            return entity.damageSources().mobAttack(entity);
        }
    }

    @Override
    public void onCast(
        Level world,
        int spellLevel,
        LivingEntity caster,
        CastSource castSource,
        MagicData magicData
) {

    UUID uuid = caster.getUUID();
    if (!magicData.getPlayerCooldowns().isOnCooldown(this)
            && !magicData.getPlayerRecasts().hasRecastForSpell(getSpellId())) {

        magicData.getPlayerRecasts().addRecast(
                new RecastInstance(
                        getSpellId(),
                        spellLevel,
                        getRecastCount(spellLevel, caster),
                        80,
                        castSource,
                        null
                ),
                magicData
        );
            if (caster instanceof Player player) {
            comboMap.put(uuid, 0);
            }
    }

    int combo = comboMap.getOrDefault(uuid, 0) + 1;
    if (combo > getRecastCount(spellLevel, caster)) {
    combo = 1;
    }
    comboMap.put(uuid, combo);

    double damageMultiplier;
    if (combo == 3) {
        damageMultiplier = 1.4;
    } else {
        damageMultiplier = 0.8;
    }

    BasicAttackPreEvent AttackPreEvent = new BasicAttackPreEvent(
            caster,
            combo,
            getDamage(spellLevel, caster),
            damageMultiplier,
            "physical"
            );

    if (!MinecraftForge.EVENT_BUS.post(AttackPreEvent)) {

        double baseDamage = AttackPreEvent.getDamage();
        damageMultiplier = AttackPreEvent.getDamageMultiplier();

        double finalDamage = baseDamage * damageMultiplier;

        DamageSource ds = getDamageSource(spellLevel, caster);

        double range = FastAttributeUtil.getAttackRange(caster);
    double halfAngle = Math.toRadians(60);

    Vec3 lookVec = caster.getLookAngle();
    Vec3 casterPos = caster.position().add(0, caster.getEyeHeight(), 0);

    List<LivingEntity> hitTargets = new ArrayList<>();

    for (LivingEntity e : world.getEntitiesOfClass(
        LivingEntity.class,
        caster.getBoundingBox().inflate(range),
        target -> target != caster
    )) {
    Vec3 targetPos = e.position().add(0, e.getEyeHeight() / 2, 0);
    Vec3 dirToTarget = targetPos.subtract(casterPos).normalize();

    double distance = casterPos.distanceTo(targetPos);
    double dot = lookVec.dot(dirToTarget);

    if (distance < 0.5 || dot >= Math.cos(halfAngle)) {
        hitTargets.add(e);
    }
    }

    if (!hitTargets.isEmpty()) {

    BasicAttackHitEvent AttackHitEvent = new BasicAttackHitEvent(
            caster,
            hitTargets,
            (float) finalDamage,
            combo,
            null,
            "physical",
            ds
    );

    MinecraftForge.EVENT_BUS.post(AttackHitEvent);

        for (LivingEntity e : hitTargets) {
        e.invulnerableTime = 0;
        e.hurt(AttackHitEvent.getDamageSource(), AttackHitEvent.getDamage());
        e.invulnerableTime = 20;
        }
    } else {
        BasicAttackMissEvent AttackMissEvent = new BasicAttackMissEvent(caster, combo, null, "physical");
        MinecraftForge.EVENT_BUS.post(AttackMissEvent);
    }

    // 发射 1 道剑气：伤害与近战相同，物理伤害；排除近战已命中目标，保证只生效一个命中
    if (!world.isClientSide) {
        SwordEnergyProjectile swordEnergy = new SwordEnergyProjectile(world, caster);
        swordEnergy.setPos(caster.getEyePosition());
        swordEnergy.shoot(caster.getLookAngle());
        swordEnergy.setDamage((float) finalDamage);
        swordEnergy.setCombo(combo);
        swordEnergy.setDamageSource(ds);
        swordEnergy.excludeTargets(hitTargets);
        world.addFreshEntity(swordEnergy);
    }

    }

    caster.swing(InteractionHand.MAIN_HAND);

    super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}
