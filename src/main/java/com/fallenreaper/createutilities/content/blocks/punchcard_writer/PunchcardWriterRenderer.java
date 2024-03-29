package com.fallenreaper.createutilities.content.blocks.punchcard_writer;

import com.fallenreaper.createutilities.index.CUBlockPartials;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;

import static com.fallenreaper.createutilities.content.blocks.typewriter.TypewriterRenderer.rotateCenteredInDirection;

public class PunchcardWriterRenderer extends SmartTileEntityRenderer<PunchcardWriterBlockEntity> {
    public PunchcardWriterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PunchcardWriterBlockEntity tileEntityIn, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        var blockState = tileEntityIn.getBlockState();
        if (!tileEntityIn.inventory.getStackInSlot(0).isEmpty()) {
            var indicator = CachedBufferer.partial(CUBlockPartials.PUNCHCARD, blockState);
            ItemRenderer itemRenderer = Minecraft.getInstance()
                    .getItemRenderer();

            var facing = blockState.getValue(PunchcardWriterBlock.HORIZONTAL_FACING);
            /*
            ms.pushPose();
            TransformStack.cast(ms)
                    .centre()
                    .rotate(Direction.UP,
                            AngleHelper.rad(180 + AngleHelper.horizontalAngle(facing)))
                    .rotate(Direction.EAST,
                            Mth.PI / 2)
                    .translate(0, 4.5 / 16f, 0)
                    .scale(.75f);

             */
            rotateCenteredInDirection(indicator, Direction.UP, facing);
            indicator.renderInto(ms, buffer.getBuffer(RenderType.solid()));
           // itemRenderer.renderStatic(tileEntityIn.inventory.getStackInSlot(0), ItemTransforms.TransformType.FIXED, light, overlay, ms, buffer, 0);
            //itemRenderer.renderStatic(CUItems.WATERING_CAN.asStack(), ItemTransforms.TransformType.FIXED, light, overlay, ms, buffer, 0);

          //  ms.popPose();
        }
    }
}
