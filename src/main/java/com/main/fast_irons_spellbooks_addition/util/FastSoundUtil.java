package com.main.fast_irons_spellbooks_addition.util;

import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public class FastSoundUtil {
    public static Optional<SoundEvent> getElementTypeSound(TripleMagicMissileProjectile.ElementType type) {
        return switch (type) {
            case FIRE -> Optional.of(SoundRegistry.FIRE_CAST.get());
            case LIGHTNING -> Optional.of(SoundRegistry.LIGHTNING_CAST.get());
            case BLOOD -> Optional.of(SoundRegistry.BLOOD_CAST.get());
            case HOLY -> Optional.of(SoundRegistry.HOLY_CAST.get());
            case EVOCATION -> Optional.of(SoundRegistry.EVOCATION_CAST.get());
            case NATURE -> Optional.of(SoundRegistry.NATURE_CAST.get());
            case ICE -> Optional.of(SoundRegistry.ICE_CAST.get());
            case ENDER -> Optional.of(SoundRegistry.ENDER_CAST.get());
        };
    }
}
