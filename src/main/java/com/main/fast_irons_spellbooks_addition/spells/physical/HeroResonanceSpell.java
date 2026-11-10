package com.main.fast_irons_spellbooks_addition.spells.physical;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.event.FastSpellOnCastEvent;
import com.main.fast_irons_spellbooks_addition.network.FastNetwork;
import com.main.fast_irons_spellbooks_addition.network.SyncHeroElementPacket;
import com.main.fast_irons_spellbooks_addition.registry.FastSchoolRegistry;
import com.main.fast_irons_spellbooks_addition.util.FastSoundUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeroResonanceSpell extends AbstractSpell {
    private static final ResourceLocation spellId =
            FastIronsSpellbooksAddition.id("hero_resonance");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(FastSchoolRegistry.PHYSICAL_ID)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public HeroResonanceSpell() {
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }

    public static final Map<UUID, Integer> elementIndexMap = new ConcurrentHashMap<>();

    public static final TripleMagicMissileProjectile.ElementType[] ELEMENTS = TripleMagicMissileProjectile.ElementType.values();

    /**
     * 勇者调律元素的唯一解析入口：从持久化数据（服务端权威）读取，无记录时返回 null
     */
    @Nullable
    public static TripleMagicMissileProjectile.ElementType getStoredElement(LivingEntity caster) {
        return caster.getPersistentData().contains("hero_element")
                ? TripleMagicMissileProjectile.ElementType.valueOf(caster.getPersistentData().getString("hero_element"))
                : null;
    }

    /**
     * 当前勇者调律元素：持久化数据 → 索引表（客户端回退）→ 默认首个元素（火焰）
     */
    public static TripleMagicMissileProjectile.ElementType getCurrentElement(@Nullable LivingEntity caster) {
        if (caster == null) {
            return ELEMENTS[0];
        }
        TripleMagicMissileProjectile.ElementType stored = getStoredElement(caster);
        return stored != null ? stored : ELEMENTS[elementIndexMap.getOrDefault(caster.getUUID(), 0)];
    }

    /**
     * 客户端播放：当前元素的施法音效
     */
    public static Optional<SoundEvent> getCurrentElementSound() {
        Player player = Minecraft.getInstance().player;
        return player == null ? Optional.empty() : FastSoundUtil.getElementTypeSound(getCurrentElement(player));
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

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return getCurrentElementSound();
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        TripleMagicMissileProjectile.ElementType element = getCurrentElement(caster);
        return List.of(
                Component.translatable("ui.fast_irons_spellbooks_addition.current_element",
                        Component.translatable("school.irons_spellbooks." + element.name().toLowerCase(Locale.ROOT)))
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
        var Event = new FastSpellOnCastEvent(caster, spellLevel, getSpellId(), this);
        if (!MinecraftForge.EVENT_BUS.post(Event)) {
            UUID uuid = caster.getUUID();

            // 未施法时为默认元素（火焰），一旦施法就切到下一个元素并开始循环
            int index = elementIndexMap.getOrDefault(uuid, 0);

            index = (index + 1) % ELEMENTS.length;
            
            elementIndexMap.put(uuid, index);

            TripleMagicMissileProjectile.ElementType current = ELEMENTS[index];

            caster.getPersistentData().putString("hero_element", current.name());

            // 同步给追踪该实体的客户端及其自身，修正多人游戏下客户端元素显示不同步
            if (!world.isClientSide) {
                FastNetwork.sendToPlayersTrackingEntityAndSelf(caster, new SyncHeroElementPacket(caster.getUUID(), index));
            }
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
      }
}
