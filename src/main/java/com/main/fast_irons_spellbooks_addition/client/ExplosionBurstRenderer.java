package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.FastIronsSpellbooksAddition;
import com.main.fast_irons_spellbooks_addition.entity.spells.magic.ExplosionBurst;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 爆裂魔法的命中特效渲染。
 * <p>
 * 结构照 {@code Megumin_staff.mcaddon}（基岩版那个包）的配方搬过来：
 * <ol>
 *   <li>11 层<b>水平火环</b>，从 0.09R 一路叠到 2.2R 高，直径按 4/12/8/20/28/12/20/16/12/8/40 的比例跳变
 *       —— 这就是番剧里那根「蘑菇柱」的来历，不是一根圆柱，而是许多片旋转的水平火焰片</li>
 *   <li>同一位置再叠一层<b>红烟片</b>，生命更长，负责柱子外圈的烟</li>
 *   <li>爆闪换成<b>星芒</b>贴图（基岩版是 100 格大的星形贴片，0.15 秒）</li>
 *   <li>竖向<b>光柱</b>：两张竖直拉长的光斑（基岩版 {@code explosion_ray} 的 lookat_y 做法）</li>
 *   <li>贴地三圈<b>冲击波</b></li>
 * </ol>
 * 比例统一按我们的爆炸半径 R 换算，基岩版原版是 40 宽 / 110 高。
 */
public class ExplosionBurstRenderer extends EntityRenderer<ExplosionBurst> {

    private static final ResourceLocation GLOW =
            FastIronsSpellbooksAddition.id("textures/entity/lens_flare.png");
    private static final ResourceLocation RING =
            FastIronsSpellbooksAddition.id("textures/entity/shockwave_ring.png");
    private static final ResourceLocation DISC =
            FastIronsSpellbooksAddition.id("textures/entity/burst_disc.png");
    private static final ResourceLocation STAR =
            FastIronsSpellbooksAddition.id("textures/entity/star_flare.png");

    /** 11 层的高度比例（基岩版 10/20/…/110 归一化） */
    private static final float[] LAYER_H = {
            0.09F, 0.18F, 0.27F, 0.36F, 0.45F, 0.55F, 0.64F, 0.73F, 0.82F, 0.91F, 1.00F
    };
    /** 对应的直径比例（基岩版 4/12/8/20/28/12/20/16/12/8/40 归一化）——故意不单调，才像翻腾的火柱 */
    private static final float[] LAYER_D = {
            0.10F, 0.30F, 0.20F, 0.50F, 0.70F, 0.30F, 0.50F, 0.40F, 0.30F, 0.20F, 1.00F
    };

    public ExplosionBurstRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public void render(ExplosionBurst entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float t = entity.tickCount + partialTick;
        float radius = entity.getRadius();
        float columnTop = radius * 1.05F;
        float columnWide = radius * 0.90F;

        poseStack.pushPose();

        // ① 火环柱：爆炸的核心火体，烟柱和蘑菇帽由 ExplosionBurst 的粒子负责
        fireColumn(poseStack, bufferSource, t, columnTop, columnWide);

        // ③ 朝向镜头的部分
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        verticalShafts(poseStack, bufferSource, t, radius);
        starFlash(poseStack, bufferSource, t, radius);
        starRays(poseStack, bufferSource, t, radius);

        poseStack.popPose();

        // ④ 贴地冲击波
        ring(poseStack, bufferSource, t, radius, 0.0F, 26.0F, 1.05F, 235, 255, 170, 90);
        ring(poseStack, bufferSource, t, radius, 4.0F, 30.0F, 0.90F, 180, 255, 110, 60);
        ring(poseStack, bufferSource, t, radius, 8.0F, 34.0F, 0.70F, 130, 220, 60, 40);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 11 层水平火环，逐层向上延迟出现，前 4 tick 张开 */
    private void fireColumn(PoseStack poseStack, MultiBufferSource bufferSource, float t,
                            float columnTop, float columnWide) {
        VertexConsumer hot = bufferSource.getBuffer(RenderType.eyes(DISC));
        for (int i = 0; i < LAYER_H.length; i++) {
            float local = t - i * 1.0F;
            if (local < 0.0F || local > 26.0F) {
                continue;
            }
            float grow = Mth.clamp(local / 5.0F, 0.0F, 1.0F);
            float fade = 1.0F - local / 26.0F;
            float size = columnWide * LAYER_D[i] * (0.35F + 0.65F * grow);
            int a = (int) (150.0F * fade * fade);

            float spin = (i % 2 == 0 ? 1 : -1) * (3.0F + i);
            flatDisc(poseStack, hot, size * 0.5F, columnTop * LAYER_H[i], local, spin, 255, 116, 0, a);
        }
    }

    /** 竖直光柱：把光斑按 (0.12, 2.4) 拉长，得到基岩版 lookat_y 那种竖向芒 */
    private void verticalShafts(PoseStack poseStack, MultiBufferSource bufferSource, float t, float radius) {
        if (t > 16.0F) {
            return;
        }
        float fade = 1.0F - t / 16.0F;
        VertexConsumer hot = bufferSource.getBuffer(RenderType.eyes(GLOW));

        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * 90.0F));
            poseStack.scale(radius * 0.11F, radius * 1.9F, 1.0F);
            poseStack.translate(0.0D, 0.55D, 0.0D);
            BurstGeometry.quadBoth(hot, poseStack.last(), 1.0F, 255, 210, 150, (int) (150 * fade));
            poseStack.popPose();
        }
    }

    /** 爆闪：星形贴片，前 3 tick 拉到 1.5R 然后迅速淡出 */
    private void starFlash(PoseStack poseStack, MultiBufferSource bufferSource, float t, float radius) {
        if (t > 9.0F) {
            return;
        }
        float grow = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
        float fade = 1.0F - t / 9.0F;
        VertexConsumer hot = bufferSource.getBuffer(RenderType.eyes(STAR));

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(t * 0.9F));
        BurstGeometry.quadBoth(hot, poseStack.last(), radius * (0.55F + 0.95F * grow),
                255, 248, 232, (int) (255 * fade));
        poseStack.popPose();

        // 中心过曝的另一层，几乎不透明白
        poseStack.pushPose();
        BurstGeometry.quadBoth(hot, poseStack.last(), radius * (0.10F + 0.22F * grow),
                255, 180, 90, (int) (230 * fade));
        poseStack.popPose();
    }

    /** 星芒：从核心炸出去的短芒，配合火环柱 */
    private void starRays(PoseStack poseStack, MultiBufferSource bufferSource, float t, float radius) {
        if (t > 18.0F) {
            return;
        }
        float prog = Mth.clamp(t / 12.0F, 0.0F, 1.0F);
        float fade = 1.0F - t / 18.0F;
        float inner = radius * (0.05F + 0.25F * prog);
        float outer = radius * (0.25F + 1.05F * prog);
        float base = radius * 0.14F * (1.0F - 0.55F * prog);
        float tip = radius * 0.012F;
        float spin = t * 3.0F;

        VertexConsumer hot = bufferSource.getBuffer(RenderType.eyes(GLOW));
        for (int i = 0; i < 8; i++) {
            BurstGeometry.ray(hot, poseStack.last(), spin + i * 45.0F, inner, outer, base, tip,
                    (int) (200 * fade), 255, 245, 220, 255, 110, 40);
        }
    }

    /** 一片躺平的火环/烟环，绕 Y 轴自转 */
    private void flatDisc(PoseStack poseStack, VertexConsumer vc, float halfSize, float height,
                          float localTick, float spinDeg, int r, int g, int b, int a) {
        poseStack.pushPose();
        poseStack.translate(0.0D, height, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(localTick * spinDeg));
        BurstGeometry.quadBoth(vc, poseStack.last(), halfSize, r, g, b, a);
        poseStack.popPose();
    }

    /** 一圈贴地扩散的冲击波 */
    private void ring(PoseStack poseStack, MultiBufferSource bufferSource, float t, float radius,
                      float delay, float duration, float spread, int alpha, int r, int g, int b) {
        float local = t - delay;
        if (local <= 0.0F || local >= duration) {
            return;
        }
        float prog = local / duration;
        // 前期张得快、后期放缓，像真实冲击波
        float size = radius * (0.12F + spread * (float) Math.pow(prog, 0.6F));
        int a = (int) (alpha * Math.pow(1.0F - prog, 1.4F));

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(local * 5.0F));
        BurstGeometry.quadBoth(bufferSource.getBuffer(RenderType.eyes(RING)), poseStack.last(), size, r, g, b, a);
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ExplosionBurst entity) {
        return GLOW;
    }
}
