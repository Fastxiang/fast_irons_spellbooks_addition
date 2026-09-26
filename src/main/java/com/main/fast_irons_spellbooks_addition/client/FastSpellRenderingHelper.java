package com.main.fast_irons_spellbooks_addition.client;

import com.main.fast_irons_spellbooks_addition.entity.spells.magic.TripleMagicMissileProjectile;
import com.main.fast_irons_spellbooks_addition.spells.physical.HeroResonanceSpell;
import com.main.fast_irons_spellbooks_addition.spells.physical.MagicBeamSpell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 客户端光束渲染辅助类
 * 复刻原版 SpellRenderingHelper.renderRayOfSiphoning 的光束绘制，
 * 但颜色按勇者调律元素（hero_element）替换为对应属性颜色。
 */
@OnlyIn(Dist.CLIENT)
public class FastSpellRenderingHelper {
    public static final ResourceLocation BEACON = IronsSpellbooks.id("textures/entity/ray/beacon_beam.png");
    public static final ResourceLocation TWISTING_GLOW = IronsSpellbooks.id("textures/entity/ray/twisting_glow.png");

    /**
     * 读取施法者当前勇者调律元素
     */
    public static TripleMagicMissileProjectile.ElementType getCastingElement(LivingEntity entity) {
        int index = HeroResonanceSpell.elementIndexMap.getOrDefault(entity.getUUID(), 0);
        return HeroResonanceSpell.ELEMENTS[index];
    }

    /**
     * 元素对应的属性颜色（与 TripleMagicMissileProjectile.setElementProperties 一致）
     */
    public static int[] getElementColor(TripleMagicMissileProjectile.ElementType type) {
        return switch (type) {
            case FIRE -> new int[]{255, 60, 30};
            case LIGHTNING -> new int[]{85, 255, 255};
            case BLOOD -> new int[]{180, 20, 20};
            case HOLY -> new int[]{242, 247, 92};
            case EVOCATION -> new int[]{0, 170, 170};
            case NATURE -> new int[]{20, 163, 40};
            case ICE -> new int[]{208, 249, 255};
            case ENDER -> new int[]{255, 180, 255};
        };
    }

    public static void renderMagicBeam(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        poseStack.pushPose();
        poseStack.translate(0, entity.getEyeHeight() * .8f, 0);

        var pose = poseStack.last();
        Vec3 start = Vec3.ZERO;
        Vec3 end;

        Vec3 impact = Utils.raycastForEntity(entity.level(), entity, MagicBeamSpell.getRange(0), true).getLocation();
        float distance = (float) entity.getEyePosition().distanceTo(impact);
        float radius = .12f;

        // 勇者调律：光束颜色随元素改变（保持与原版一致的亮度缩放）
        int[] rgb = getElementColor(getCastingElement(entity));
        int r = (int) (rgb[0] * .7f);
        int g = (int) (rgb[1] * .7f);
        int b = (int) (rgb[2] * .7f);
        int a = (int) (255 * 1f);

        float deltaTicks = entity.tickCount + partialTicks;
        float deltaUV = -deltaTicks % 10;
        float max = Mth.frac(deltaUV * 0.2F - (float) Mth.floor(deltaUV * 0.1F));
        float min = -1.0F + max;

        var dir = entity.getLookAngle().normalize();

        //y rotation is a triangle of x and z axis
        float dx = (float) dir.x;
        float dz = (float) dir.z;
        //angle = atan o/a
        float yRot = (float) Mth.atan2(dz, dx) - 1.5707f; // for some reason, we are rotated 90 degrees the wrong way. subtracting 2 pi here.
        //x rotation is a triangle of xz and y axis
        float dxz = Mth.sqrt(dx * dx + dz * dz);
        float dy = (float) dir.y;
        //angle = atan o/a
        float xRot = (float) Mth.atan2(dy, dxz);
        poseStack.mulPose(Axis.YP.rotation(-yRot));
        poseStack.mulPose(Axis.XP.rotation(-xRot));
        for (float j = 1; j <= distance; j += .5f) {
            Vec3 wiggle = new Vec3(
                    Mth.sin(deltaTicks * .8f) * .02f,
                    Mth.sin(deltaTicks * .8f + 100) * .02f,
                    Mth.cos(deltaTicks * .8f) * .02f
            );
            end = new Vec3(0, 0, Math.min(j, distance)).add(wiggle);
            VertexConsumer inner = bufferSource.getBuffer(RenderType.entityTranslucent(BEACON, true));
            drawHull(start, end, radius, radius, pose, inner, r, g, b, a, min, max);
            VertexConsumer outer = bufferSource.getBuffer(RenderType.entityTranslucent(TWISTING_GLOW));
            drawQuad(start, end, radius * 4f, 0, pose, outer, r, g, b, a, min, max);
            drawQuad(start, end, 0, radius * 4f, pose, outer, r, g, b, a, min, max);
            start = end;
        }
        poseStack.popPose();
    }

    public static void drawHull(Vec3 from, Vec3 to, float width, float height, PoseStack.Pose pose, VertexConsumer consumer, int r, int g, int b, int a, float uvMin, float uvMax) {
        //Bottom
        drawQuad(from.subtract(0, height * .5f, 0), to.subtract(0, height * .5f, 0), width, 0, pose, consumer, r, g, b, a, uvMin, uvMax);
        //Top
        drawQuad(from.add(0, height * .5f, 0), to.add(0, height * .5f, 0), width, 0, pose, consumer, r, g, b, a, uvMin, uvMax);
        //Left
        drawQuad(from.subtract(width * .5f, 0, 0), to.subtract(width * .5f, 0, 0), 0, height, pose, consumer, r, g, b, a, uvMin, uvMax);
        //Right
        drawQuad(from.add(width * .5f, 0, 0), to.add(width * .5f, 0, 0), 0, height, pose, consumer, r, g, b, a, uvMin, uvMax);
    }

    public static void drawQuad(Vec3 from, Vec3 to, float width, float height, PoseStack.Pose pose, VertexConsumer consumer, int r, int g, int b, int a, float uvMin, float uvMax) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float halfWidth = width * .5f;
        float halfHeight = height * .5f;

        consumer.vertex(poseMatrix, (float) from.x - halfWidth, (float) from.y - halfHeight, (float) from.z).color(r, g, b, a).uv(0f, uvMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalMatrix, 0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, (float) from.x + halfWidth, (float) from.y + halfHeight, (float) from.z).color(r, g, b, a).uv(1f, uvMin).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalMatrix, 0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, (float) to.x + halfWidth, (float) to.y + halfHeight, (float) to.z).color(r, g, b, a).uv(1f, uvMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalMatrix, 0f, 1f, 0f).endVertex();
        consumer.vertex(poseMatrix, (float) to.x - halfWidth, (float) to.y - halfHeight, (float) to.z).color(r, g, b, a).uv(0f, uvMax).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalMatrix, 0f, 1f, 0f).endVertex();
    }
}
