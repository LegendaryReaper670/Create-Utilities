package com.fallenreaper.createutilities.content.blocks.punchcard_writer;

import com.fallenreaper.createutilities.index.CUBlockPartials;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class PunchcardWriterRenderer extends SmartTileEntityRenderer<PunchcardWriterBlockEntity> {
    public PunchcardWriterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
    @Override
    protected void renderSafe(PunchcardWriterBlockEntity tileEntityIn, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = tileEntityIn.getBlockState();
        if(tileEntityIn.hasPunchcard()) {
            SuperByteBuffer blueprint = CachedBufferer.partial(CUBlockPartials.PUNCHCARD, blockState);

            blueprint.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
