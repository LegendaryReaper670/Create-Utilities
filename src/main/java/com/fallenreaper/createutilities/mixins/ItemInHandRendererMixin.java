package com.fallenreaper.createutilities.mixins;

import com.fallenreaper.createutilities.core.data.IFirstPersonAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

       //Credits: MehVahdJukaar/Moonlight
        @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemTransforms$TransformType;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                ordinal = 1), require = 0)
        public void renderItem(AbstractClientPlayer entity, float partialTicks, float pitch, InteractionHand hand, float attackAnim, ItemStack stack, float handHeight, PoseStack matrixStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
            Item item = stack.getItem();
            if (item instanceof IFirstPersonAnimation provider) {
                provider.animateItem(entity, stack, hand, matrixStack, partialTicks, pitch, attackAnim, handHeight);
            }
    }
}
