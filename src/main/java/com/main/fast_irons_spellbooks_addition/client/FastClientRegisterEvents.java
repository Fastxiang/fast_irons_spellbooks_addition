package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.client.MirrorShooterRenderer;
import com.main.fast_irons_spellbooks_addition.entity.client.SkillArrowRenderer;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionRenderer;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class FastClientRegisterEvents {
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                FastEntityRegistry.TRIPLE_MAGIC_MISSILE_PROJECTILE.get(),
                TripleMagicMissileRenderer::new
        );
        event.registerEntityRenderer(FastEntityRegistry.ARROW_RAIN.get(), NoopRenderer::new);
        event.registerEntityRenderer(FastEntityRegistry.SMALL_EXPLOSION_PROJECTILE.get(), (context) -> new ExplosionRenderer(context, 0.75f));
        event.registerEntityRenderer(FastEntityRegistry.EXPLOSION_PROJECTILE.get(), (context) -> new ExplosionRenderer(context, 1.25f));
        event.registerEntityRenderer(
                FastEntityRegistry.MIRROR_SHOOTER.get(),
                MirrorShooterRenderer::new
        );
        event.registerEntityRenderer(
                FastEntityRegistry.SKILL_ARROW.get(),
                SkillArrowRenderer::new
        );
    }
    
}
