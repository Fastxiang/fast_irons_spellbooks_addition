package com.main.fast_irons_spellbooks_addition.entity.client;

import com.main.fast_irons_spellbooks_addition.entity.MirrorShooterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class MirrorShooterRenderer extends HumanoidMobRenderer<MirrorShooterEntity, MirrorShooterModel> {

    public MirrorShooterRenderer(EntityRendererProvider.Context context) {
        super(context, new MirrorShooterModel(context.bakeLayer(ModelLayers.PLAYER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(MirrorShooterEntity entity) {
        return entity.getSkinTexture();
    }
}