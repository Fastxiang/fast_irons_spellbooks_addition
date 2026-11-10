package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.summon.PhantomCantor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 自动施法随从的渲染：两层朝向镜头的光斑 + 吟唱时多一层扩散环。
 * 贴图沿用 mod 里已有的 {@code lens_flare.png}，不需要额外美术资源。
 */
public class PhantomCantorRenderer extends EntityRenderer<PhantomCantor> {

    private static final ResourceLocation FLARE =
            FastIronsSpellbooksAddition.id("textures/entity/lens_flare.png");

    public PhantomCantorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public void render(PhantomCantor entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;
        float cast = entity.getCastProgress(partialTick);
        boolean casting = entity.isCastingSpell();

        // 平时轻微呼吸，吟唱时整体放大并提亮
        float pulse = 1.0F + Mth.sin(t * 0.15F) * 0.07F;
        float core = (0.42F + cast * 0.18F) * pulse;
        float halo = core * (1.9F + cast * 0.9F);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(FLARE));

        // 外层光晕
        int haloAlpha = 60 + (int) (cast * 70.0F);
        drawQuad(consumer, matrix, halo, 120, 200, 255, haloAlpha);
        // 内核
        int coreAlpha = 190 + (int) (cast * 60.0F);
        drawQuad(consumer, matrix, core, 210, 245, 255, coreAlpha);

        poseStack.popPose();

        if (casting) {
            // 吟唱环：随进度扩张、逐渐变淡
            float ring = core * (2.2F + cast * 2.4F);
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            PoseStack.Pose ringPose = poseStack.last();
            int ringAlpha = (int) ((1.0F - cast) * 150.0F);
            drawQuad(bufferSource.getBuffer(RenderType.entityTranslucent(FLARE)),
                    ringPose.pose(), ring, 255, 200, 140, ringAlpha);
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, float size,
                                 int r, int g, int b, int a) {
        consumer.vertex(matrix, -size, -size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, -size, size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, size, size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, size, -size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomCantor entity) {
        return FLARE;
    }
}
