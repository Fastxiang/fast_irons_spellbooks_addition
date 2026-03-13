package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
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

public class TripleStrikeSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("triple_strike");
            
    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.COMMON)
        .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(0)
        .setAllowCrafting(false)
        .build();
        
    public TripleStrikeSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }
    
    private final Map<UUID, Integer> comboMap = new ConcurrentHashMap<>();

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
    
    private float getDamageText(int spellLevel, LivingEntity entity) {
        if (entity != null) {
            return getDamage(spellLevel, entity);
        }
        return 0;
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

        double range = 3;

        var attr = caster.getAttribute(ENTITY_REACH.get());
        if (attr != null) {
            double value = attr.getValue();
            if (value > 0) {
                range = value;
            }
        }
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
    
    }
    
    caster.swing(InteractionHand.MAIN_HAND);
    
    super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}
