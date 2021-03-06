package com.fallenreaper.createutilities.content.blocks.sprinkler;


import com.fallenreaper.createutilities.index.CUBlockPartials;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.flwdata.RotatingData;
import com.simibubi.create.content.contraptions.relays.encased.ShaftInstance;
import net.minecraft.world.level.block.state.BlockState;

public class SprinklerInstance extends ShaftInstance implements DynamicInstance {
    protected final SprinklerBlockEntity tile;

    protected final ModelData propagatorModelData;

    public SprinklerInstance(MaterialManager dispatcher, KineticTileEntity tile) {
        super(dispatcher, tile);
        this.tile = (SprinklerBlockEntity) tile;
        propagatorModelData = getTransformMaterial().getModel(CUBlockPartials.SPRINKLER_PROPAGATOR, blockState).createInstance();
    }


    @Override
    protected Instancer<RotatingData> getModel() {
        return super.getModel();
    }

    @Override
    protected BlockState getRenderedBlockState() {
        return super.getRenderedBlockState();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, propagatorModelData);
    }

    @Override
    public void remove() {
        super.remove();
        propagatorModelData.delete();
    }

    @Override
    public void beginFrame() {

    }
}
