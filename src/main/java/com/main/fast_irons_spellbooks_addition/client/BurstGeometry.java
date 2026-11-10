package com.main.fast_irons_spellbooks_addition.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 爆裂魔法三个阶段的公共绘制工具：朝向镜头的整图、星芒梯形分段。
 * <p>
 * 所有顶点都写在 {@link PoseStack.Pose} 的局部 XY 平面上（z=0），调用方负责摆好朝向。
 * 顶点只带颜色不带贴图细节：整图走 0~1 UV，星芒只取 {@code lens_flare.png} 中心那一点纯白。
 * <p>
 * {@link #quadBoth} / {@link #ray} 会把两种绕序都发一遍：{@code RenderType.eyes} 是开背面剔除的，
 * 而 billboard 变换（{@code cameraOrientation} + {@code YP 180°}）会把本地 X 镜像，
 * 两遍发出去就能保证正面那遍一定在，且加法混合下亮度不会翻倍。
 */
public final class BurstGeometry {

    private BurstGeometry() {
    }

    /** 一张朝向镜头的整图（0~1 UV，用贴图自带的渐变） */
    public static void quad(VertexConsumer vc, PoseStack.Pose pose, float size, int r, int g, int b, int a) {
        vc.vertex(pose.pose(), -size, -size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), -size, size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), size, size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), size, -size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    /** 同上，但正反两面各发一遍（给开剔除的渲染类型用，比如贴地的冲击波环） */
    public static void quadBoth(VertexConsumer vc, PoseStack.Pose pose, float size, int r, int g, int b, int a) {
        quad(vc, pose, size, r, g, b, a);
        vc.vertex(pose.pose(), -size, -size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), size, -size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), size, size, 0.0F).color(r, g, b, a)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        vc.vertex(pose.pose(), -size, size, 0.0F).color(r, g, b, a)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    /**
     * 一根星芒：沿角度方向按段画梯形，宽度从根部收到尖端，颜色和亮度同时向尖端衰减。
     *
     * @param widthTip 建议留一点点宽度（0.02~0.06），完全收成 0 会在尖端出现锯齿
     */
    public static void ray(VertexConsumer vc, PoseStack.Pose pose, float angleDeg, float inner, float outer,
                           float widthBase, float widthTip,
                           int alpha, int r0, int g0, int b0, int r1, int g1, int b1) {
        float rad = angleDeg * Mth.DEG_TO_RAD;
        float dx = Mth.cos(rad);
        float dy = Mth.sin(rad);
        float px = -dy;
        float py = dx;

        for (int i = 0; i < RAY_STEPS; i++) {
            float f0 = i / (float) RAY_STEPS;
            float f1 = (i + 1) / (float) RAY_STEPS;

            float rA = Mth.lerp(f0, inner, outer);
            float rB = Mth.lerp(f1, inner, outer);
            float wA = Mth.lerp(f0, widthBase, widthTip);
            float wB = Mth.lerp(f1, widthBase, widthTip);

            int aA = (int) (alpha * (1.0F - f0 * f0));
            int aB = (int) (alpha * (1.0F - f1 * f1));

            int cAr = (int) Mth.lerp(f0, r0, r1);
            int cAg = (int) Mth.lerp(f0, g0, g1);
            int cAb = (int) Mth.lerp(f0, b0, b1);
            int cBr = (int) Mth.lerp(f1, r0, r1);
            int cBg = (int) Mth.lerp(f1, g0, g1);
            int cBb = (int) Mth.lerp(f1, b0, b1);

            float x0 = dx * rA + px * wA;
            float y0 = dy * rA + py * wA;
            float x1 = dx * rA - px * wA;
            float y1 = dy * rA - py * wA;
            float x2 = dx * rB - px * wB;
            float y2 = dy * rB - py * wB;
            float x3 = dx * rB + px * wB;
            float y3 = dy * rB + py * wB;

            vertex(vc, pose, x0, y0, cAr, cAg, cAb, aA);
            vertex(vc, pose, x1, y1, cAr, cAg, cAb, aA);
            vertex(vc, pose, x2, y2, cBr, cBg, cBb, aB);
            vertex(vc, pose, x3, y3, cBr, cBg, cBb, aB);

            vertex(vc, pose, x0, y0, cAr, cAg, cAb, aA);
            vertex(vc, pose, x3, y3, cBr, cBg, cBb, aB);
            vertex(vc, pose, x2, y2, cBr, cBg, cBb, aB);
            vertex(vc, pose, x1, y1, cAr, cAg, cAb, aA);
        }
    }

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, int r, int g, int b, int a) {
        vc.vertex(pose.pose(), x, y, 0.0F).color(r, g, b, a)
                .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    /** 星芒每段的细分数，越大越平滑、顶点也越多 */
    private static final int RAY_STEPS = 5;
}
