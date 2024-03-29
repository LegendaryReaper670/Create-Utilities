package com.fallenreaper.createutilities.content.blocks.typewriter;

import com.fallenreaper.createutilities.index.CUContainerTypes;
import com.simibubi.create.foundation.gui.container.ContainerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class TypewriterContainer extends ContainerBase<TypewriterBlockEntity> {

    public TypewriterContainer(MenuType<?> type, int id, Inventory inv, TypewriterBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public TypewriterContainer(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static TypewriterContainer create(int id, Inventory inv, TypewriterBlockEntity te) {
        return new TypewriterContainer(CUContainerTypes.TYPEWRITER.get(), id, inv, te);
    }

    @Override
    protected TypewriterBlockEntity createOnClient(FriendlyByteBuf extraData) {
        var world = Minecraft.getInstance().level;
        var tileEntity = world.getBlockEntity(extraData.readBlockPos());
        if (tileEntity instanceof TypewriterBlockEntity typewriter) {
            typewriter.readClient(extraData.readNbt());
            return typewriter;
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot clickedSlot = getSlot(pIndex);
        if (!clickedSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = clickedSlot.getItem();

        if (pIndex < slots.size()) {
            moveItemStackTo(stack, 0, slots.size(), false);
        } else {
            if (moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 2, 3, false)
                    || moveItemStackTo(stack, 4, 5, false))
                ;
        }

        return ItemStack.EMPTY;
    }

    @Override
    protected void initAndReadInventory(TypewriterBlockEntity contentHolder) {


    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index > contentHolder.inventory.getSlots() && super.canDragTo(slot);
    }

    @Override
    protected void addSlots() {
        int x = 0;
        int y = 0;
        int[] yOffsets = {y + 20, y + 30, y + 79, y + 61 //y+ 49, y + 78
                //
        };

        addSlot(new SlotItemHandler(contentHolder.inventory, 4, x + 173, yOffsets[1] - 16 * 2));
        addSlot(new SlotItemHandler(contentHolder.inventory, 5, x + 173, yOffsets[2] - 16 * 2));
        addSlot(new SlotItemHandler(contentHolder.inventory, 0, x + 45, yOffsets[0] - 16 * 2));
        addSlot(new SlotItemHandler(contentHolder.inventory, 1, x + 45, yOffsets[3] - 16 * 2));


        addPlayerSlots(8, 165);
    }

    @Override
    protected void saveData(TypewriterBlockEntity contentHolder) {

    }

}
