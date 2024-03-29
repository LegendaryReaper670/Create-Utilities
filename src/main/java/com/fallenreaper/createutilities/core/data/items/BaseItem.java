package com.fallenreaper.createutilities.core.data.items;

import com.fallenreaper.createutilities.CreateUtilities;
import com.fallenreaper.createutilities.core.data.IThirdPersonAnimation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BaseItem extends Item implements IThirdPersonAnimation {
   // protected TranslatableComponent pTextComponent;
    private boolean hasDescription;
    private ChatFormatting[] chatFormatting;

    public BaseItem(Properties pProperties) {
        super(pProperties);
        this.hasDescription = false;
    }

    public BaseItem addDescription(ChatFormatting... formatting) {
        this.hasDescription = true;
        this.chatFormatting = formatting;
    //    this.pTextComponent = pTextComponent;
        return this;
    }


    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        if(hasDescription) {

           pTooltipComponents.add(new TranslatableComponent(CreateUtilities.ID + "." +this.getDescriptionId().substring(21)+"."+"description").withStyle((chatFormatting)));
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public <T extends LivingEntity> boolean poseArm(ItemStack itemStack, HumanoidArm arm, HumanoidModel<T> model, T entity, boolean rightHand) {
        return false;
    }
}
