package com.main.fast_irons_spellbooks_addition.mixin.client;

import com.main.fast_irons_spellbooks_addition.client.GreatswordArmamentClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 第一人称：和第三人称同一套做法，只把送去渲染的 ItemStack 换掉。
 *
 * <p>不取消原版渲染、不自己摆姿势 —— 抬手角度、挥动进度、物品的
 * {@code firstperson_righthand} 变换都由原版算，我们只换"手里那个东西"。</p>
 *
 * <p>同样只处理主手：{@code renderArmWithItem} 每手各调一次，副手的物品保持原样。</p>
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @ModifyVariable(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack fast$greatswordArmament(
            ItemStack original,
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack itemStack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return original;
        }
        if (GreatswordArmamentClient.isArmed(player)) {
            return GreatswordArmamentClient.displayStack();
        }
        return original;
    }
}
