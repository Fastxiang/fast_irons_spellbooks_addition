package com.main.fast_irons_spellbooks_addition.mixin.client;

import com.main.fast_irons_spellbooks_addition.client.GreatswordArmamentClient;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 玩家模型手上渲染的物品：武装状态期间换成大剑。
 *
 * <p>只换送去渲染的 ItemStack，不拦掉重画 —— 握持姿态、手臂摆动、附魔光效、
 * 物品自身的 display 变换全部还是原版那套，只是"手上那个东西"变了。</p>
 *
 * <h2>只动主手（关键）</h2>
 * <p>原版 {@code ItemInHandLayer.render} 对主手和副手各调一次
 * {@code renderArmWithItem}，而且<b>主手固定用 {@code HumanoidArm.RIGHT} 传</b>
 * （内部先按 {@code getMainArm() == RIGHT} 把两个 ItemStack 对调，再分别以
 * RIGHT / LEFT 调用）。所以只要卡住 {@code arm == HumanoidArm.RIGHT} 就是"只换主手"，
 * 不需要看 {@code getMainArm()}。</p>
 *
 * <p>不卡的话左手的盾牌/法杖也会变成大剑，玩家看到的就是左右手各一把，
 * 误以为代码里多渲染了一次。</p>
 *
 * <h2>第一人称跳过</h2>
 * <p>ISS 播施法动画时 {@code AnimationHelper.animatePlayerStart} 会把 PlayerAnimator
 * 切成 {@code FirstPersonMode.THIRD_PERSON_MODEL}，第一人称下也会渲染一只完整的
 * 第三人称模型手。那只手的手持物走的就是本方法，而第一人称真正的手由
 * {@code ItemInHandRenderer.renderArmWithItem} 渲染 —— 两边都换就会叠成两把。</p>
 *
 * <p>但不能一律跳过：施法那 2.5 秒里 {@code ItemInHandRenderer} 是不画手的
 * （PlayerAnimator 已经用模型手顶替），这时如果本层也跳过，屏幕上就一把剑都没有。
 * 所以只有"第一人称 <b>且</b> 非施法"才返回 {@link ItemStack#EMPTY}（本层内部
 * {@code if (!itemStack.isEmpty())} 会整段跳过），施法期间照常出图。</p>
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @ModifyVariable(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack fast$greatswordArmament(
            ItemStack original,
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (!(entity instanceof Player player) || !GreatswordArmamentClient.isArmed(player)) {
            return original;
        }
        // 只动主手槽位。原版 render() 里主手固定走 HumanoidArm.RIGHT 那一支
        // （flag = getMainArm() == RIGHT，两个 ItemStack 互换后再按 RIGHT/LEFT 传），
        // 所以这里判 RIGHT 就是"主手"，和玩家是不是左撇子无关。
        // 副手放行，否则左右手各一把，看起来就是两个大剑。
        if (arm != HumanoidArm.RIGHT) {
            return original;
        }
        // 第一人称：分两种情况，否则会"一把都没有"或者"叠成两把"
        //   施法中  —— 第一人称由 PlayerAnimator 的模型手接管（THIRD_PERSON_MODEL），
        //              原版 ItemInHandRenderer 这时不再画手，所以必须由本层出图；
        //   非施法  —— 模型手不存在，唯一可见的是原版那只手，本层要让路，
        //              交给 ItemInHandRendererMixin。
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()
                && !ClientMagicData.isCasting()) {
            return ItemStack.EMPTY;
        }
        return GreatswordArmamentClient.displayStack();
    }
}
