package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 吟唱期的脚下魔法阵：只在施法者正在唱「爆裂魔法」时画，正反两层反向旋转。
 * <p>
 * 判定方式和 ISS 自己的 {@code ChargeSpellLayer} 一致——读客户端同步下来的
 * {@code SyncedSpellData.isCasting() / getCastingSpellId()}，不需要额外的网络包。
 * 泛型照 ISS 的写法留成 {@code <T, M>}，这样能直接挂到玩家渲染器上。
 */
public class ExplosionChantLayer<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final String SPELL_ID = FastIronsSpellbooksAddition.id("explosion").toString();
    private static final ResourceLocation CIRCLE =
            FastIronsSpellbooksAddition.id("textures/entity/magic_circle.png");

    public ExplosionChantLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        SyncedSpellData data = ClientMagicData.getSyncedSpellData(entity);
        if (!data.isCasting() || !SPELL_ID.equals(data.getCastingSpellId())) {
            return;
        }

        float t = entity.tickCount + partialTick;
        // 呼吸：亮度轻微起伏，读得出「正在蓄力」
        float pulse = 0.75F + Mth.sin(t * 0.25F) * 0.15F;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CIRCLE));

        // 外层：顺时针
        drawCircle(poseStack, vc, 2.35F, t * 1.1F, 215, 45, 32, (int) (185 * pulse));
        // 内层：逆时针、更小、更亮，做差速
        drawCircle(poseStack, vc, 1.55F, -t * 1.9F, 255, 120, 70, (int) (140 * pulse));
    }

    private static void drawCircle(PoseStack poseStack, VertexConsumer vc, float size, float rotationDeg,
                                   int r, int g, int b, int a) {
        poseStack.pushPose();
        // 抬到脚面上方一点，避免和地面 z-fighting
        poseStack.translate(0.0D, 0.03D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDeg));
        // entityTranslucentEmissive 是 NO_CULL，只发一遍，否则透明度会翻倍
        BurstGeometry.quad(vc, poseStack.last(), size, r, g, b, a);
        poseStack.popPose();
    }
}
