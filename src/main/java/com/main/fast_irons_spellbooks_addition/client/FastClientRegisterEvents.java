package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.client.MirrorShooterRenderer;
import com.main.fast_irons_spellbooks_addition.entity.client.SkillArrowRenderer;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionRenderer;
import com.main.fast_irons_spellbooks_addition.registry.FastEntityRegistry;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.SwordEnergyRenderer;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileRenderer;
import io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.player.Player;
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
        event.registerEntityRenderer(
                FastEntityRegistry.SWORD_ENERGY_PROJECTILE.get(),
                SwordEnergyRenderer::new
        );
        event.registerEntityRenderer(FastEntityRegistry.ARROW_RAIN.get(), NoopRenderer::new);
        event.registerEntityRenderer(FastEntityRegistry.BLOOD_RAIN.get(), NoopRenderer::new);
        event.registerEntityRenderer(FastEntityRegistry.MAGIC_FIREBALL_TRACKING.get(), (context) -> new FireballRenderer(context, 1.25f));
        event.registerEntityRenderer(FastEntityRegistry.SMALL_EXPLOSION_PROJECTILE.get(), (context) -> new ExplosionRenderer(context, 0.75f));
        // 爆裂魔法的投射物：保持原来的火球模型（用户要求），爆炸特效另外做
        event.registerEntityRenderer(FastEntityRegistry.EXPLOSION_PROJECTILE.get(), (context) -> new ExplosionRenderer(context, 1.25f));
        // 爆裂魔法的命中特效
        event.registerEntityRenderer(FastEntityRegistry.EXPLOSION_BURST.get(), ExplosionBurstRenderer::new);
        event.registerEntityRenderer(
                FastEntityRegistry.MIRROR_SHOOTER.get(),
                MirrorShooterRenderer::new
        );
        event.registerEntityRenderer(
                FastEntityRegistry.SKILL_ARROW.get(),
                SkillArrowRenderer::new
        );
        event.registerEntityRenderer(
                FastEntityRegistry.MAGIC_ARROW_DRAGON_BREATH.get(),
                MagicArrowRenderer::new
        );
        event.registerEntityRenderer(
                FastEntityRegistry.PHANTOM_CANTOR.get(),
                PhantomCantorRenderer::new
        );
        event.registerEntityRenderer(
                FastEntityRegistry.FISSION_MAGIC_FIREBALL.get(),
                (context) -> new FireballRenderer(context, 1.1f)
        );
        event.registerEntityRenderer(
                FastEntityRegistry.FISSION_SMALL_FIREBALL.get(),
                // 注意：ExplosionRenderer 内部会再乘 13 倍（那是给爆点特效用的），
                // 用在这里会让「小火球」比本体还大，所以改用 FireballRenderer 的小缩放
                (context) -> new FireballRenderer(context, 0.5f)
        );
        event.registerEntityRenderer(FastEntityRegistry.THUNDER_ARRAY.get(), NoopRenderer::new);
        event.registerEntityRenderer(
                FastEntityRegistry.MIASMA_POISON_ARROW.get(),
                io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrowRenderer::new
        );
    }

    /**
     * 给玩家渲染器挂「吟唱魔法阵」图层。
     * 写法与 ISS 的 {@code ClientSetup.addLayerToPlayerSkin} 一致：拿 raw 的 LivingEntityRenderer 再 addLayer。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new ExplosionChantLayer<>(livingRenderer));
            }
        }
    }

}
