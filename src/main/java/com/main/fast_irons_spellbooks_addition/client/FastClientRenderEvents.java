package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.registry.FastSpellRegistry;
import com.main.fast_irons_spellbooks_addition.spells.physical.MagicBeamSpell;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 魔法光束客户端事件
 * 复刻原版 ClientPlayerEvents 中 Ray of Siphoning 的渲染与粒子表现：
 * - RenderLivingEvent.Post：玩家施法时绘制元素颜色光束
 * - PlayerTickEvent：施法期间在命中点生成对应元素粒子
 */
@Mod.EventBusSubscriber(
        modid = FastIronsSpellbooksAddition.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public class FastClientRenderEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() && event.phase == TickEvent.Phase.END && event.player == Minecraft.getInstance().player) {
            var level = Minecraft.getInstance().level;

            if (level != null) {
                List<Entity> spellcasters = level.getEntities((Entity) null, event.player.getBoundingBox().inflate(64), (mob) -> mob instanceof Player || mob instanceof IMagicEntity);
                spellcasters.forEach((entity) -> {
                    var spellData = ClientMagicData.getSyncedSpellData((LivingEntity) entity);
                    if (spellData.isCasting() && spellData.getCastingSpellId().equals(FastSpellRegistry.MAGIC_BEAM.get().getSpellId())) {
                        Vec3 impact = Utils.raycastForEntity(entity.level(), entity, MagicBeamSpell.getRange(0), true).getLocation().subtract(0, .25, 0);
                        // 勇者调律：命中点粒子随元素改变
                        ParticleOptions particle = getElementParticle(FastSpellRenderingHelper.getCastingElement((LivingEntity) entity));
                        for (int i = 0; i < 8; i++) {
                            Vec3 motion = new Vec3(
                                    Utils.getRandomScaled(.2f),
                                    Utils.getRandomScaled(.2f),
                                    Utils.getRandomScaled(.2f)
                            );
                            entity.level().addParticle(particle, impact.x + motion.x, impact.y + motion.y, impact.z + motion.z, motion.x, motion.y, motion.z);
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        var livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            var syncedData = ClientMagicData.getSyncedSpellData(livingEntity);
            if (syncedData.isCasting() && syncedData.getCastingSpellId().equals(FastSpellRegistry.MAGIC_BEAM.get().getSpellId())) {
                FastSpellRenderingHelper.renderMagicBeam(livingEntity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
            }
        }
    }

    /**
     * 元素对应的粒子（与 TripleMagicMissileProjectile.getParticle 一致）
     */
    private static ParticleOptions getElementParticle(TripleMagicMissileProjectile.ElementType type) {
        return switch (type) {
            case LIGHTNING -> ParticleHelper.ELECTRICITY;
            case BLOOD -> ParticleHelper.BLOOD;
            case HOLY -> ParticleHelper.WISP;
            case EVOCATION -> ParticleTypes.SMOKE;
            case NATURE -> ParticleHelper.ACID;
            case ICE -> ParticleHelper.SNOWFLAKE;
            case ENDER -> ParticleHelper.UNSTABLE_ENDER;
            default -> ParticleHelper.FIRE_EMITTER;
        };
    }
}
