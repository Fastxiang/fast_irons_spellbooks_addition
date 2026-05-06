package com.main.fast_irons_spellbooks_addition.entity.client;

import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import org.jetbrains.annotations.NotNull;

public class MirrorShooterModel extends HumanoidModel<MirrorShooterEntity> {

    public MirrorShooterModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(@NotNull MirrorShooterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.isUsingItem() && entity.getMainHandItem().getItem() instanceof BowItem) {
            // 拉弓动画：根据头部旋转调整手臂
            float headX = headPitch * Mth.DEG_TO_RAD;
            float headY = netHeadYaw * Mth.DEG_TO_RAD;

            this.rightArm.xRot = -Mth.HALF_PI + headX;
            this.rightArm.yRot = -0.4F + headY;
            this.rightArm.zRot = 0.0F;

            this.leftArm.xRot = -Mth.HALF_PI + headX;
            this.leftArm.yRot = 0.4F + headY;
            this.leftArm.zRot = 0.0F;
        }

        float attackAnim = entity.getAttackAnimProgress();
        if (attackAnim > 0.0F) {
            this.rightArm.xRot = -1.2F + Mth.sin(attackAnim * (float) Math.PI) * 1.2F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = Mth.sin(attackAnim * (float) Math.PI) * 0.5F;
            this.leftArm.xRot = -0.8F;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
    }
}