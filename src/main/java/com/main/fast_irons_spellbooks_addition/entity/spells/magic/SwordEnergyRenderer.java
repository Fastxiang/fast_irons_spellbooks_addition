package com.main.fast_irons_spellbooks_addition.entity.spells.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 剑气渲染器：复刻原版 Blood Slash 渲染，仅把颜色从血色改为青白色
 */
public class SwordEnergyRenderer extends EntityRenderer<SwordEnergyProjectile> {
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_0.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_1.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_2.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_3.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_4.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_5.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_6.png"),
            ResourceLocation.withDefaultNamespace("textures/particle/sweep_7.png")
    };

    /** 青白色（原版 Blood Slash 为 90, 0, 10 的血红色） */
    private static final int COLOR_R = 190;
    private static final int COLOR_G = 250;
    private static final int COLOR_B = 255;

    public SwordEnergyRenderer(Context context) {
        super(context);
    }

    @Override
    public void render(SwordEnergyProjectile entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();

        Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        entity.animationTime++;
        poseStack.mulPose(Axis.ZP.rotationDegrees(((entity.animationSeed % 30) - 15) * (float) Math.sin(entity.animationTime * .015)));

        float oldWith = (float) entity.oldBB.getXsize();
        float width = entity.getBbWidth();
        width = oldWith + (width - oldWith) * Math.min(partialTicks, 1);

        poseStack.mulPose(Axis.YP.rotationDegrees(-15));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-10));
        drawSlash(pose, entity, bufferSource, light, width, 4);

        poseStack.mulPose(Axis.YP.rotationDegrees(30));
        poseStack.mulPose(Axis.ZP.rotationDegrees(20));
        drawSlash(pose, entity, bufferSource, light, width, 0);

        poseStack.popPose();

        super.render(entity, yaw, partialTicks, poseStack, bufferSource, light);
    }

    private void drawSlash(Pose pose, SwordEnergyProjectile entity, MultiBufferSource bufferSource, int light, float width, int offset) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity, offset)));
        float halfWidth = width * .5f;
        consumer.vertex(poseMatrix, -halfWidth, -.1f, -halfWidth).color(COLOR_R, COLOR_G, COLOR_B, 255).uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, halfWidth, -.1f, -halfWidth).color(COLOR_R, COLOR_G, COLOR_B, 255).uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, halfWidth, -.1f, halfWidth).color(COLOR_R, COLOR_G, COLOR_B, 255).uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, -halfWidth, -.1f, halfWidth).color(COLOR_R, COLOR_G, COLOR_B, 255).uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0f, 1f, 0f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SwordEnergyProjectile entity) {
        int frame = (entity.animationTime / 4) % TEXTURES.length;
        return TEXTURES[frame];
    }

    private ResourceLocation getTextureLocation(SwordEnergyProjectile entity, int offset) {
        int frame = (entity.animationTime / 6 + offset) % TEXTURES.length;
        return TEXTURES[frame];
    }
}
