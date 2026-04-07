package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.util.FastSoundUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import com.main.fast_irons_spellbooks_addition.util.FastAttributeUtil;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.event.BasicAttackPreEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TripleMagicMissileSpell extends AbstractSpell {

    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("triple_magic_missile");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public TripleMagicMissileSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    /** 连击计数 */
    private final Map<UUID, Integer> comboMap = new ConcurrentHashMap<>();

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
        return CastType.LONG;
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
        return 3;
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
    public AnimationHolder getCastStartAnimation() {
    var mc = Minecraft.getInstance();
    if (mc.player == null) {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    UUID uuid = mc.player.getUUID();
    int combo = comboMap.getOrDefault(uuid, 0);
    
    if (!ClientMagicData.getRecasts().hasRecastForSpell(getSpellId())) {
    combo = 0;
    }
    
    return switch (combo) {
        case 0 -> SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
        case 1 -> SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
        case 2 -> SpellAnimations.ANIMATION_LONG_CAST;
        default -> SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    };
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return Optional.empty();
        }

        UUID uuid = mc.player.getUUID();
        int index = HeroResonanceSpell.elementIndexMap.getOrDefault(uuid, 0);
        TripleMagicMissileProjectile.ElementType current = HeroResonanceSpell.ELEMENTS[index];
        return FastSoundUtil.getElementTypeSound(current);
    }
    
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", getDamageText(spellLevel, caster))
        );
    }
    
    private float getDamage(int spellLevel, LivingEntity entity) {
    return FastAttributeUtil.getMagicAttack(entity) * FastAttributeUtil.getBasicMagicDamage(entity);
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
            "magic"
        );
        
        if (!MinecraftForge.EVENT_BUS.post(AttackPreEvent)) {
        
        double baseDamage = AttackPreEvent.getDamage();
        damageMultiplier = AttackPreEvent.getDamageMultiplier();
        
        double finalDamage = baseDamage * damageMultiplier;
        TripleMagicMissileProjectile.ElementType element = TripleMagicMissileProjectile.ElementType.FIRE;
        TripleMagicMissileProjectile magicMissileProjectile = new TripleMagicMissileProjectile(world, caster);
        if (caster.getPersistentData().contains("hero_element")) {
            element = TripleMagicMissileProjectile.ElementType.valueOf(caster.getPersistentData().getString("hero_element"));
        }
        if (AttackPreEvent.getElementType() != null) {
            element = AttackPreEvent.getElementType();
        }
        magicMissileProjectile.setElementProperties(element);
        magicMissileProjectile.setPos(caster.position().add(0, caster.getEyeHeight() - magicMissileProjectile.getBoundingBox().getYsize() * .5f, 0));
        magicMissileProjectile.shoot(caster.getLookAngle());
        magicMissileProjectile.setCombo(combo);
        magicMissileProjectile.setDamage((float) finalDamage);
        world.addFreshEntity(magicMissileProjectile);
        }
        
        caster.swing(InteractionHand.MAIN_HAND);
        
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }
}
